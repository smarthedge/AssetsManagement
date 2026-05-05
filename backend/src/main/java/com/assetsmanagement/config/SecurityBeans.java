package com.assetsmanagement.config;

import com.assetsmanagement.entity.User;
import com.assetsmanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.stream.Collectors;

/**
 * Provides the UserDetailsService needed by Spring Security for authentication.
 * Loads users from the database and maps their roles to GrantedAuthority instances.
 */
@Configuration
public class SecurityBeans {

    private static final Logger log = LoggerFactory.getLogger(SecurityBeans.class);

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            log.debug("Loading user details for: {}", username);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        log.warn("User not found: {}", username);
                        return new UsernameNotFoundException("User not found: " + username);
                    });

            if (Boolean.FALSE.equals(user.getStatus())) {
                log.warn("Attempted login for deactivated user: {}", username);
                throw new UsernameNotFoundException("User account is disabled: " + username);
            }

            var authorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                    .collect(Collectors.toSet());

            log.debug("User '{}' loaded with authorities: {}", username, authorities);

            boolean hasPassword = user.getPasswordHash() != null;
            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    hasPassword ? user.getPasswordHash() : "",
                    user.getStatus(),
                    true,
                    hasPassword,
                    true,
                    authorities
            );
        };
    }
}
