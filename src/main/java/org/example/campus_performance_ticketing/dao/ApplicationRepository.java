package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByApplicantId(Long applicantId);
}
