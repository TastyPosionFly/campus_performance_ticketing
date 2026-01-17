package org.example.campus_performance_ticketing.config;

import org.example.campus_performance_ticketing.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

@Configuration
public class ProfileConfig {
    @Autowired
    private Environment env;

    @PostConstruct
    public void init() {
        String[] profiles = env.getActiveProfiles();
        if (profiles.length > 0) {
            FileUtil.setActiveProfile(profiles[0]);
        }
    }
}
