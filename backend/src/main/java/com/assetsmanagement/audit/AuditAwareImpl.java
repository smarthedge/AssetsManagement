package com.assetsmanagement.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Provides the current authenticated user for JPA auditing.
 * Extracts the username from the Spring Security context.
 */
@Component
public class AuditAwareImpl implements AuditorAware<String> {

    private static final Logger log = LoggerFactory.getLogger(AuditAwareImpl.class);

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            log.debug("No authenticated user found for audit");
            return Optional.empty();
        }
        String username = authentication.getName();
        log.trace("Current auditor resolved to: {}", username);
        return Optional.of(username);
    }
}
