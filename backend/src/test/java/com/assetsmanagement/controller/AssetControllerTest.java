package com.assetsmanagement.controller;

import com.assetsmanagement.config.JwtConfig;
import com.assetsmanagement.config.SecurityConfig;
import com.assetsmanagement.config.SecurityBeans;
import com.assetsmanagement.dto.PageResponse;
import com.assetsmanagement.dto.response.AssetResponse;
import com.assetsmanagement.repository.UserRepository;
import com.assetsmanagement.service.AssetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityBeans.class})
class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetService assetService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser
    void getAssets_shouldReturnPaginatedResults() throws Exception {
        AssetResponse asset = new AssetResponse(1L, "Laptop", "A laptop", "IT",
                "SN-001", LocalDate.now(), new BigDecimal("1500.00"),
                true, 1L, "admin", LocalDateTime.now(), 1L, "admin", LocalDateTime.now(), 0);
        PageResponse<AssetResponse> page = new PageResponse<>(List.of(asset), 0, 20, 1, 1);

        when(assetService.getAllAssets(anyInt(), anyInt(), isNull())).thenReturn(page);

        mockMvc.perform(get("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Laptop"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAsset_shouldReturnCreated() throws Exception {
        AssetResponse asset = new AssetResponse(1L, "New Asset", "Desc", "IT",
                "SN-002", LocalDate.now(), new BigDecimal("999.99"),
                true, 1L, "admin", LocalDateTime.now(), 1L, "admin", LocalDateTime.now(), 0);

        when(assetService.createAsset(any())).thenReturn(asset);

        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Asset\",\"description\":\"Desc\",\"category\":\"IT\",\"serialNumber\":\"SN-002\",\"value\":999.99}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Asset"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createAsset_shouldReturn403ForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Asset\",\"description\":\"Desc\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAssets_shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/assets"))
                .andExpect(status().isUnauthorized());
    }
}
