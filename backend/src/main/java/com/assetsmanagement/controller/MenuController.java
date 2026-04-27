package com.assetsmanagement.controller;

import com.assetsmanagement.dto.response.MenuItemResponse;
import com.assetsmanagement.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Provides the hierarchical menu structure for the frontend navigation bar.
 * Menu items can be role-filtered based on the authenticated user.
 */
@RestController
@RequestMapping("/api/menu")
@Tag(name = "Menu", description = "Navigation menu endpoints")
@SecurityRequirement(name = "Bearer")
public class MenuController {

    private static final Logger log = LoggerFactory.getLogger(MenuController.class);
    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @Operation(summary = "Get navigation menu items for the current user")
    @GetMapping
    public ResponseEntity<List<MenuItemResponse>> getMenu() {
        log.info("GET /api/menu");
        return ResponseEntity.ok(menuService.getMenuForCurrentUser());
    }
}
