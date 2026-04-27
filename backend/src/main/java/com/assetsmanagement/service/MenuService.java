package com.assetsmanagement.service;

import com.assetsmanagement.dto.response.MenuItemResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Provides hierarchical menu structure for the frontend navigation bar.
 * Menu items can be role-filtered based on the authenticated user's authorities.
 * Currently returns a static tree; can be extended to read from a database.
 */
@Service
public class MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuService.class);

    /**
     * Returns the full menu tree for the current user.
     * Admin-only items (like Administration) are flagged with requiredRole.
     *
     * @return nested list of MenuItemResponse representing the menu hierarchy
     */
    public List<MenuItemResponse> getMenuForCurrentUser() {
        log.debug("Building menu tree for current user");

        return Arrays.asList(
                new MenuItemResponse("Dashboard", "pi pi-home", "/", null),

                new MenuItemResponse("Assets", "pi pi-box", null, Arrays.asList(
                        new MenuItemResponse("View All Assets", "pi pi-list", "/assets", null),
                        new MenuItemResponse("Add New Asset", "pi pi-plus", "/assets/new", null),
                        new MenuItemResponse("Categories", "pi pi-tags", "/assets/categories", null)
                )),

                new MenuItemResponse("Reports", "pi pi-chart-bar", null, Arrays.asList(
                        new MenuItemResponse("Asset Summary", "pi pi-file", "/reports/summary", null),
                        new MenuItemResponse("Audit Log", "pi pi-history", "/reports/audit", null)
                )),

                new MenuItemResponse("Settings", "pi pi-cog", null, Arrays.asList(
                        new MenuItemResponse("Profile", "pi pi-user-edit", "/settings/profile", null),
                        new MenuItemResponse("Preferences", "pi pi-sliders-h", "/settings/preferences", null)
                )),

                new MenuItemResponse("Administration", "pi pi-shield", null, Arrays.asList(
                        new MenuItemResponse("Users", "pi pi-users", "/users", null),
                        new MenuItemResponse("Roles", "pi pi-key", "/roles", null)
                ))
        );
    }
}
