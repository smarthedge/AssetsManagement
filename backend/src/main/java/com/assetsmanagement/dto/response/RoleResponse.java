package com.assetsmanagement.dto.response;

public record RoleResponse(
        Long id,
        String name,
        String description,
        Boolean status,
        Integer version
) {}
