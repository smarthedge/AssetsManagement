package com.assetsmanagement.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        Set<RoleResponse> roles,
        Boolean status,
        Long createdByUserId,
        String createdByUsername,
        LocalDateTime createdDateTime,
        Long lastChangedByUserId,
        String lastChangedByUsername,
        LocalDateTime lastChangedDateTime,
        Integer version
) {}
