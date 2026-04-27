package com.assetsmanagement.dto.response;

import java.util.List;

public record MenuItemResponse(
        String label,
        String icon,
        String routerLink,
        List<MenuItemResponse> items
) {}
