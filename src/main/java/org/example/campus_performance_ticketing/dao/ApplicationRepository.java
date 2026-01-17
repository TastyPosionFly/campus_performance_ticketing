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
     * 根据申请人ID和申请类型查询该用户的申请记录，按申请时间倒序排列
     * @param applicantId
     * @param applicationType
     * @return
     */
    List<Application> findByApplicantIdAndApplicationTypeOrderByApplyTimeDesc(Long applicantId, String applicationType);

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
     * 根据申请类型和目标ID列表查询申请记录
     * @param applicationType
     * @param targetIds
     * @return
     */
    List<Application> findByApplicationTypeAndTargetIdIn(String applicationType, List<Long> targetIds);

    /**
     * 根据申请类型、目标ID列表和状态查询申请记录
     * @param applicationType
     * @param targetIds
     * @param status
     * @return
     */
    List<Application> findByApplicationTypeAndTargetIdInAndStatus(String applicationType, List<Long> targetIds, Integer status);
}
