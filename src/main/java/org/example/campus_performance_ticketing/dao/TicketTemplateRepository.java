package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.TicketTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketTemplateRepository extends JpaRepository<TicketTemplate, Long> {

    /**
     * 根据场次ID查找当前“上架”的电子票模板
     * 用于前端展示电子票时获取背景图
     *
     * @param sessionId 场次 ID
     * @param status    状态 (通常传 1-上架)
     * @return 模板信息
     */
    Optional<TicketTemplate> findBySessionIdAndStatus(Long sessionId, Integer status);

    /**
     * 根据场次ID查找电子票模板
     * 不区分状态，用于后台管理
     *
     * @param sessionId 场次 ID
     * @return 模板信息
     */
    Optional<TicketTemplate> findBySessionId(Long sessionId);
}