package com.lottery.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            SecurityContext context = SecurityContextHolder.getContext();
            if (context == null) {
                return Optional.of("system");
            }
            Authentication authentication = context.getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }
            String username = authentication.getName();
            if (username == null || username.isEmpty() || "anonymousUser".equals(username)) {
                return Optional.of("system");
            }
            return Optional.of(username);
        };
    }
}
