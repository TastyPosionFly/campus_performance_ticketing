package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * 检查用户是否已在某场次拥有特定状态的票
     * 场景：防止重复预约
     */


    boolean existsByUserIdAndSessionIdAndStatusIn(Long userId, Long sessionId, Integer[] statuses);


    /**
     * 分页查找某用户的票据，按创建时间倒序
     * @param userId
     * @param pageable
     * @return
     */
    Page<Ticket> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 分页查找某用户在特定状态下的票据，按创建时间倒序
     * @param userId
     * @param status
     * @param pageable
     * @return
     */
    Page<Ticket> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Integer status, Pageable pageable);


    /**
     * 根据票据码查找票据
     * 场景：扫码核销时使用
     */
    Optional<Ticket> findByTicketCode(String ticketCode);

    /**
     * 批量将某场次下所有状态为“已预约(0)”的票据更新为“已失效(3)”
     * 场景：演出结束后清理未核销的票
     */
    @Modifying
    @Query("UPDATE Ticket t SET t.status = 3 WHERE t.session.id = :sessionId AND t.status = 0")
    int expireTicketsBySessionId(Long sessionId);

    /**
     * 统计某场次特定状态的票数
     * 场景：统计“实际到场人数” (status=1) 或 “总预约人数” (status=0)
     */
    long countBySessionIdAndStatus(Long sessionId, Integer status);

    /**
     * 查找某场次下特定状态的所有票据 (带分页或不带分页)
     * 这里为了导出名单，通常是全量查询，不分页
     * 为了性能优化，使用了 JOIN FETCH 预加载 UserInfo
     */
    @Query("SELECT t FROM Ticket t JOIN FETCH t.user WHERE t.session.id = :sessionId AND t.status = :status")
    List<Ticket> findBySessionIdAndStatusWithUser(Long sessionId, Integer status);

    /**
     * 【新增】统计某演出(跨场次)所有实际核销的票数
     * 状态说明：1 (已使用/已核销), 2 (已评价) - 都视为有效入场
     * 关联路径：Ticket -> PerformanceSession -> Performance
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.session.performance.id = :pid AND t.status IN (1, 2)")
    int countActualCheckInByPerformanceId(@Param("pid") Long performanceId);
}