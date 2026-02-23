package org.example.campus_performance_ticketing.logic;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.campus_performance_ticketing.dao.*;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.ticket.TicketAttendanceDTO;
import org.example.campus_performance_ticketing.logic.dto.ticket.TicketBookingDTO;
import org.example.campus_performance_ticketing.logic.dto.ticket.TicketDetailDTO;
import org.example.campus_performance_ticketing.model.*;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.awt.*;
import org.apache.poi.ss.usermodel.Font;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Slf4j
@Valid
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketTemplateRepository ticketTemplateRepository;
    private final PerformanceSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final OrganizationInfoRepository organizationInfoRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    private final static Logger logger = Logger.getLogger(TicketService.class.getName());

    @Value("${file.base.url}")
    private String fileBaseUrl;

    /**
     * 用户预约抢票
     *
     * @param openId 当前用户 OpenId
     * @param dto    预约请求参数
     * @return 成功返回票据 ID
     */
    @Transactional(rollbackOn = Exception.class)
    public ApiResponse<TicketDetailDTO> bookTicket(@NotBlank String openId, TicketBookingDTO dto) {
        // 1. 获取用户信息
        UserInfo user = userRepository.findByOpenid(openId).orElse(null);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }
        if (user.getStatus() != 1) {
            return ApiResponse.fail("账号状态异常，无法预约");
        }

        // 2. 校验场次有效性
        PerformanceSession session = sessionRepository.findById(dto.getSessionId()).orElse(null);
        if (session == null) {
            return ApiResponse.fail("场次不存在");
        }

        // 检查演出是否已发布
        if (session.getPerformance().getPublishStatus() != 1) {
            return ApiResponse.fail("演出尚未发布或已下架");
        }

        // 检查时间：是否已经开演或结束？(假设开演后不可预约)
        if (LocalDateTime.now().isAfter(session.getStartTime())) {
            return ApiResponse.fail("演出已开始，停止预约");
        }

        // 3. 重复预约校验 (同一个场次每人限一张)
        // 状态 0(已预约) 和 1(已核销) 视为已占用配额
        boolean hasTicket = ticketRepository.existsByUserIdAndSessionIdAndStatusIn(
                user.getId(),
                session.getId(),
                new Integer[]{0, 1}
        );
        if (hasTicket) {
            return ApiResponse.fail("您已预约过该场次，请勿重复操作");
        }

        // 4. 扣减库存 (核心并发控制)
        // update set surplus = surplus - 1 where id = ? and surplus > 0
        int updatedRows = sessionRepository.decreaseStock(session.getId());
        if (updatedRows == 0) {
            return ApiResponse.fail("手慢了，票已售罄");
        }

        // 5. 创建票据
        Ticket ticket = new Ticket();
        ticket.setUser(user);
        ticket.setPerformance(session.getPerformance());
        ticket.setSession(session);
        ticket.setStatus(0); // 0-已预约

        // 生成唯一核销码 (简单版用 UUID，生产环境可用 Snowflake + 签名)
        ticket.setTicketCode(generateTicketCode());

        ticketRepository.save(ticket);

        log.info("用户 [{}] 成功预约场次 [{}], 票号: {}", user.getNickname(), session.getId(), ticket.getTicketCode());

        // 6. 构造返回 DTO
        TicketDetailDTO result = convertToDetailDTO(ticket);
        return ApiResponse.success(result);
    }


    /**
     * 查询单张票据详情 (供前端展示票面)
     */
    public ApiResponse<TicketDetailDTO> getTicketDetail(@NotBlank String openId, Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) {
            return ApiResponse.fail("票据不存在");
        }

        // 校验权限：只能看自己的票
        if (!ticket.getUser().getOpenid().equals(openId)) {
            return ApiResponse.fail("无权查看此票据");
        }

        return ApiResponse.success(convertToDetailDTO(ticket));
    }


    /**
     * 获取我的票夹列表 (分页)
     * @param openId
     * @param page
     * @param size
     * @param performanceId
     * @param status
     * @param sortByUpcomingStartTime
     * @return
     */
    public ApiResponse<Page<TicketDetailDTO>> getMyTickets(
            String openId,
            int page,
            int size,
            Long performanceId,
            Integer status,
            Boolean sortByUpcomingStartTime
    ) {
        UserInfo user = userRepository.findByOpenid(openId).orElse(null);
        if (user == null) return ApiResponse.fail("用户不存在");

        Pageable pageable;
        Page<Ticket> ticketPage;

        boolean sortUpcoming = Boolean.TRUE.equals(sortByUpcomingStartTime);

        if (sortUpcoming) {
            pageable = PageRequest.of(page, size);
            ticketPage = ticketRepository.findMyTicketsOrderByUpcomingStartTime(
                    user.getId(), performanceId, status, pageable
            );
        } else {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            if (status != null && performanceId != null) {
                ticketPage = ticketRepository.findByUserIdAndPerformanceIdAndStatusOrderByCreatedAtDesc(
                        user.getId(), performanceId, status, pageable
                );
            } else if (status != null) {
                ticketPage = ticketRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                        user.getId(), status, pageable
                );
            } else if (performanceId != null) {
                ticketPage = ticketRepository.findByUserIdAndPerformanceIdOrderByCreatedAtDesc(
                        user.getId(), performanceId, pageable
                );
            } else {
                ticketPage = ticketRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
            }
        }

        Page<TicketDetailDTO> dtoPage = ticketPage.map(this::convertToDetailDTO);
        return ApiResponse.success(dtoPage);
    }

    /**
     * 允许场地管理员、演出举办者、组织成员、超级管理员扫码核销 (检票)
     * @param operatorOpenId
     * @param ticketCode
     * @return
     */
    @Transactional(rollbackOn = Exception.class)
    public ApiResponse<Void> checkInTicket(@NotBlank String operatorOpenId, String ticketCode) {
        // 1. 获取操作员信息
        UserInfo operator = userRepository.findByOpenid(operatorOpenId).orElse(null);
        if (operator == null) {
            return ApiResponse.fail("操作员不存在");
        }

        // 2. 查找票据
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode).orElse(null);
        if (ticket == null) {
            return ApiResponse.fail("无效的票据");
        }

        // 3. 校验票据状态
        if (ticket.getStatus() == 1) {
            return ApiResponse.fail("该票据已核销，请勿重复检票");
        }
        if (ticket.getStatus() != 0) {
            return ApiResponse.fail("票据状态异常 (已取消或已失效)");
        }

        // 4. 校验核销时间窗口
        PerformanceSession session = ticket.getSession();
        LocalDateTime now = LocalDateTime.now();
        // 允许提前 2 小时开始检票，直到演出结束
        if (now.isBefore(session.getStartTime().minusHours(2))) {
            return ApiResponse.fail("未到检票时间 (开演前2小时开放)");
        }
        if (now.isAfter(session.getEndTime())) {
            return ApiResponse.fail("演出已结束，票据已过期");
        }

        // 5. 校验操作员权限（改进：允许场地管理员、演出举办者、组织成员、超级管理员）
        boolean isSuperAdmin = "SUPER_ADMIN".equalsIgnoreCase(operator.getRole());
        boolean isAuthorized = false;

        if (isSuperAdmin) {
            isAuthorized = true;
        } else {
            // 场地管理员检查
            Venue venue = session.getVenue();
            if (venue != null) {
                UserInfo manager = venue.getManager();
                if (manager != null && manager.getId().equals(operator.getId())) {
                    isAuthorized = true;
                }
            } else {
                log.error("数据异常：场次 [{}] 未关联有效场地", session.getId());
                return ApiResponse.fail("数据异常：场次未关联场地");
            }

            // 演出举办者（个人）或组织（组织成员）检查
            if (!isAuthorized) {
                Performance performance = session.getPerformance();
                if (performance != null && checkIsOrganizerForTicket(operator, performance)) {
                    isAuthorized = true;
                }
            }
        }

        if (!isAuthorized) {
            return ApiResponse.fail("无权核销：仅超级管理员、场地负责人、演出举办者及其组织成员可操作");
        }

        // 6. 执行核销
        ticket.setStatus(1); // 1-已核销
        ticket.setCheckInTime(now);
        ticket.setCheckInOperatorId(operator.getId());

        ticketRepository.save(ticket);
        log.info("票据 [{}] 核销成功，操作员: {}", ticketCode, operator.getNickname());

        return ApiResponse.success(null);
    }


    /**
     * 获取某场次实际到场人员名单（状态为 1 已核销的票）
     * @param operatorOpenId
     * @param sessionId
     * @param page
     * @param size
     * @return
     */
    @Transactional
    public ApiResponse<Page<TicketAttendanceDTO>> getAttendanceList(
            @NotBlank String operatorOpenId,
            @NotNull Long sessionId,
            int page,
            int size
    ) {
        // 1. 基础校验
        UserInfo operator = userRepository.findByOpenid(operatorOpenId).orElse(null);
        if (operator == null) return ApiResponse.fail("操作员不存在");

        PerformanceSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) return ApiResponse.fail("场次不存在");

        // 2. 权限校验逻辑（保持原样）
        boolean hasPermission = false;

        if ("ADMIN".equalsIgnoreCase(operator.getRole()) || "SUPER_ADMIN".equalsIgnoreCase(operator.getRole())) {
            hasPermission = true;
        } else {
            Performance performance = session.getPerformance();
            if (performance != null) hasPermission = checkIsOrganizer(operator, performance);
        }

        if (!hasPermission) {
            return ApiResponse.fail("无权查看：您不是该演出的组织者或管理员");
        }

        // 3. 分页查询：状态为 1 (已核销)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "checkInTime"));
        Page<Ticket> ticketPage = ticketRepository.findBySessionIdAndStatus(sessionId, 1, pageable);

        // 4. 转换为 DTO（用 Page.map）
        Page<TicketAttendanceDTO> dtoPage = ticketPage.map(ticket -> {
            UserInfo user = ticket.getUser();
            TicketAttendanceDTO dto = new TicketAttendanceDTO();

            dto.setTicketId(ticket.getId());
            dto.setUserId(user.getId());
            dto.setNickname(user.getNickname());
            dto.setAvatar(AvatarUrlUtil.buildAvatarUrl(user.getAvatar(), fileBaseUrl));
            dto.setMajor(user.getMajor());
            dto.setCollege(user.getCollege());
            dto.setStatus(user.getStatus());
            dto.setUserIdentity(user.getUserIdentity());

            dto.setStudentNo(user.getStudentNo());
            dto.setCheckInTime(ticket.getCheckInTime());

            return dto;
        });

        return ApiResponse.success(dtoPage);
    }

    /**
     * 导出某场次实际到场人员名单为 Excel 文件
     * 权限：同 getAttendanceList
     * @param operatorOpenId 操作员 OpenID
     * @param sessionId 场次 ID
     * @param response HTTP 响应对象，用于写出 Excel 文件
     */
    public void exportAttendanceExcelForWeixin(String operatorOpenId, Long sessionId, HttpServletResponse response) throws Exception {

        // 1. 基础校验 + 权限校验（自包含）
        UserInfo operator = userRepository.findByOpenid(operatorOpenId).orElse(null);
        if (operator == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "操作员不存在");
            return;
        }

        PerformanceSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "场次不存在");
            return;
        }

        boolean hasPermission = false;
        if ("ADMIN".equalsIgnoreCase(operator.getRole()) || "SUPER_ADMIN".equalsIgnoreCase(operator.getRole())) {
            hasPermission = true;
        } else {
            Performance performance = session.getPerformance();
            if (performance != null) {
                hasPermission = checkIsOrganizer(operator, performance);
            }
        }

        if (!hasPermission) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "无权导出：您不是该演出的组织者或管理员");
            return;
        }

        // 2. 查询数据：状态=1(已核销) 的票据（全量，用于导出）
        List<Ticket> tickets = ticketRepository.findBySessionIdAndStatusWithUser(sessionId, 1);

        // 3. 响应头
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "attendance_session_" + sessionId + "_" + ts + ".xlsx";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");

        // 4. 写 Excel（直接从 tickets 写，不依赖 DTO 方法）
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("attendance");

            CellStyle headerStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            String[] headers = new String[] { "序号", "昵称", "学号", "学院", "专业", "身份", "签到时间" };
            for (int i = 0; i < headers.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (int i = 0; i < tickets.size(); i++) {
                Ticket ticket = tickets.get(i);
                UserInfo user = ticket.getUser();

                Row row = sheet.createRow(rowIdx++);

                // 序号
                row.createCell(0).setCellValue(i + 1);

                // 昵称/学号/学院/专业
                row.createCell(1).setCellValue(safeStr(user != null ? user.getNickname() : null));
                row.createCell(2).setCellValue(safeStr(user != null ? user.getStudentNo() : null));
                row.createCell(3).setCellValue(safeStr(user != null ? user.getCollege() : null));
                row.createCell(4).setCellValue(safeStr(user != null ? user.getMajor() : null));

                // 身份（复用你 DTO 的描述逻辑：用 TicketAttendanceDTO 来生成 desc）
                TicketAttendanceDTO tmp = new TicketAttendanceDTO();
                tmp.setUserIdentity(user != null ? user.getUserIdentity() : null);
                row.createCell(5).setCellValue(safeStr(tmp.getUserIdentityDesc()));

                // 签到时间（核销时间）
                row.createCell(6).setCellValue(ticket.getCheckInTime() == null ? "" : ticket.getCheckInTime().toString());
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            wb.write(response.getOutputStream());
            response.flushBuffer();
        }
    }


    private static String safeStr(String s) {
        return s == null ? "" : s;
    }

    /**
     * 辅助方法：检查用户是否为演出的组织者
     * (逻辑复用于 TicketTemplateService，建议后期重构提取)
     */
    private boolean checkIsOrganizer(UserInfo user, Performance performance) {
        String type = performance.getOrganizerType();
        Long organizerId = performance.getOrganizerId();

        // 场景 1: 个人举办
        if ("USER".equalsIgnoreCase(type)) {
            // 必须是本人，且状态正常
            return organizerId.equals(user.getId()) && user.getStatus() == 1;
        }

        // 场景 2: 组织举办
        if ("ORGANIZATION".equalsIgnoreCase(type)) {
            // 查询组织信息
            Optional<OrganizationInfo> orgOpt = organizationInfoRepository.findById(organizerId);
            if (orgOpt.isEmpty()) return false;

            OrganizationInfo org = orgOpt.get();
            // 组织必须状态正常，且当前用户是 Leader
            if (org.getStatus() != 1) return false;
            if (org.getLeader() == null) return false;

            return org.getLeader().getId().equals(user.getId());
        }

        return false;
    }



    /**
     * 用于检票的权限检查：除了超级管理员和场地管理员外，如果是演出组织者，还要考虑组织成员的权限
     */
    private boolean checkIsOrganizerForTicket(UserInfo user, Performance performance) {
        String type = performance.getOrganizerType();
        Long organizerId = performance.getOrganizerId();

        if ("USER".equalsIgnoreCase(type)) {
            return organizerId.equals(user.getId()) && user.getStatus() == 1;
        }

        if ("ORGANIZATION".equalsIgnoreCase(type)) {
            Optional<OrganizationInfo> orgOpt = organizationInfoRepository.findById(organizerId);
            if (orgOpt.isEmpty()) return false;

            OrganizationInfo org = orgOpt.get();
            if (org.getStatus() != 1) return false;

            // 组织负责人始终可操作
            if (org.getLeader() != null && org.getLeader().getId().equals(user.getId())) return true;

            // 新增：如果用户是组织成员也可操作（使用 repository 快速判断，需新增 existsByIdAndMembers_Id）
            try {
                if (organizationMemberRepository.existsByOrganizationIdAndUserId(organizerId, user.getId())) {
                    return true;
                }
            } catch (Exception e) {
                logger.warning("权限检查异常：查询组织成员关系失败，组织ID: " + organizerId + ", 用户ID: " + user.getId() + ", 错误: " + e.getMessage());
                return false;
            }
        }

        return false;
    }

    /**
     * 演出结束后，将该场次所有未核销的票据置为失效
     */
    @Transactional(rollbackOn = Exception.class)
    public void expireUnusedTickets(Long sessionId) {
        PerformanceSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            log.error("尝试处理过期票据失败：场次ID [{}] 不存在", sessionId);
            return;
        }

        // 只有演出结束了才能批量失效
        if (LocalDateTime.now().isBefore(session.getEndTime())) {
            log.warn("尝试处理过期票据被拒绝：场次 [{}] 尚未结束", sessionId);
            return;
        }

        int count = ticketRepository.expireTicketsBySessionId(sessionId);
        log.info("场次 [{}] 结算完成，共失效 {} 张未核销票据", sessionId, count);
    }

    /**
     * 辅助方法：Entity 转 DTO
     */
    private TicketDetailDTO convertToDetailDTO(Ticket ticket) {
        PerformanceSession session = ticket.getSession();

        TicketDetailDTO dto = new TicketDetailDTO();
        dto.setId(ticket.getId());
        dto.setTicketCode(ticket.getTicketCode());
        dto.setStatus(ticket.getStatus());
        dto.setBookingTime(ticket.getCreatedAt());

        // 演出信息
        if (ticket.getPerformance() != null) {
            dto.setPerformanceId(ticket.getPerformance().getId());
            dto.setPerformanceTitle(ticket.getPerformance().getTitle());
            dto.setPerformancePosterUrl(AvatarUrlUtil.buildAvatarUrl(ticket.getPerformance().getPosterUrl(), fileBaseUrl));
        }

        // 场次与场地信息
        if (session != null) {
            dto.setSessionId(session.getId());
            dto.setStartTime(session.getStartTime());
            dto.setEndTime(session.getEndTime());

            if (session.getVenue() != null) {
                dto.setVenueName(session.getVenue().getName());
                dto.setVenueAddress(session.getVenue().getAddress());
            }

            // 补充：查询该场次对应的电子票背景图 (仅上架状态)
            ticketTemplateRepository.findBySessionIdAndStatus(session.getId(), 1)
                    .ifPresent(tpl -> dto.setTicketBgUrl(AvatarUrlUtil.buildAvatarUrl(tpl.getBackgroundImgUrl(), fileBaseUrl)));
        }

        return dto;
    }

    /**
     * 生成唯一核销码
     */
    private String generateTicketCode() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

}