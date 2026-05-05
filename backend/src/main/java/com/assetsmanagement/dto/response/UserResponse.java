package com.assetsmanagement.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        Set<RoleResponse> roles,
        Boolean status,
        String createdByUsername,
        LocalDateTime createdDateTime,
        String lastChangedByUsername,
        LocalDateTime lastChangedDateTime,
        Integer version
) {}
