package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * 根据核销码查找票据
     * 场景：检票员扫码时调用
     */
    Optional<Ticket> findByTicketCode(String ticketCode);

    /**
     * 查找某用户的所有票据（通常用于“我的票夹”）
     * 可以配合 Sort 按时间倒序
     */
    List<Ticket> findByUserId(Long userId);

    /**
     * 查找某用户在特定场次的票
     * 场景：检查用户是否重复预约
     */
    Optional<Ticket> findByUserIdAndSessionId(Long userId, Long sessionId);

    /**
     * 查找某用户在特定状态下的票
     * 场景：查找“待参加”的票
     */
    List<Ticket> findByUserIdAndStatus(Long userId, Integer status);

    /**
     * 统计某场次特定状态的票数
     * 场景：统计“实际到场人数” (status=1) 或 “总预约人数” (status=0)
     */
    long countBySessionIdAndStatus(Long sessionId, Integer status);

    /**
     * 自定义查询：获取某场次的详细到场人员信息（用于导出）
     * 包含：学号、姓名、入场时间
     * 注意：这里返回的是 Object[]，业务层可能需要封装为 DTO
     */
    @Query("SELECT t.checkInTime, u.studentNo, u.nickname, u.college " +
            "FROM Ticket t JOIN t.user u " +
            "WHERE t.session.id = :sessionId AND t.status = 1")
    List<Object[]> findAttendanceListBySessionId(Long sessionId);
}