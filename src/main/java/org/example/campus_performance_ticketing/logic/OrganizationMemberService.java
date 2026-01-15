package org.example.campus_performance_ticketing.logic;

import jakarta.transaction.Transactional;
import org.example.campus_performance_ticketing.dao.OrganizationMemberRepository;
import org.example.campus_performance_ticketing.model.OrganizationInfo;
import org.example.campus_performance_ticketing.model.OrganizationMember;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class OrganizationMemberService {
    private final OrganizationMemberRepository organizationMemberRepository;


    private static final Logger logger = Logger.getLogger(OrganizationMemberService.class.getName());

    public OrganizationMemberService (OrganizationMemberRepository organizationMemberRepository) {
        this.organizationMemberRepository = organizationMemberRepository;
    }


}
