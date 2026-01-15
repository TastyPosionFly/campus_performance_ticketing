package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByApplicantId(Long applicantId);

    /**
     * 根据申请类型和状态查询申请记录
     * @param applicationType
     * @param status
     * @return
     */
    List<Application> findByApplicationTypeAndStatus(String applicationType, Integer status);

    /**
     * 根据申请人ID查询该用户所有申请记录，按申请时间倒序排列
     *
     * @param applicantId 用户主键ID
     * @return 申请列表（最新的在前）
     */
    List<Application> findByApplicantIdOrderByApplyTimeDesc(Long applicantId);

    /**
     * 根据申请类型查询申请记录
     * @param applicationType
     * @return
     */
    List<Application> findByApplicationType(String applicationType);

    /**
     * 根据状态查询申请记录
     * @param status
     * @return
     */
    List<Application> findByStatus(Integer status);

    /**
     * 根据ID查找申请类型
     * @param id
     * @return
     */
    String findApplicationTypeById(Long id);
}
