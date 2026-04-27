package com.assetsmanagement.controller;

import com.assetsmanagement.config.JwtConfig;
import com.assetsmanagement.config.SecurityConfig;
import com.assetsmanagement.config.SecurityBeans;
import com.assetsmanagement.dto.request.LoginRequest;
import com.assetsmanagement.dto.response.LoginResponse;
import com.assetsmanagement.dto.response.RoleResponse;
import com.assetsmanagement.dto.response.UserResponse;
import com.assetsmanagement.repository.UserRepository;
import com.assetsmanagement.service.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityBeans.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void login_shouldReturnTokenWhenCredentialsValid() throws Exception {
        UserResponse userResponse = new UserResponse(1L, "admin", "admin@test.com",
                Set.of(new RoleResponse(1L, "ROLE_ADMIN", "Admin", true, 0)),
                true, null, "admin", LocalDateTime.now(), null, "admin", LocalDateTime.now(), 0);
        LoginResponse loginResponse = new LoginResponse("test-jwt-token", userResponse);

        when(jwtTokenService.authenticate(any(LoginRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("test-jwt-token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("admin"));
    }

    @Test
    void login_shouldReturn401WhenCredentialsInvalid() throws Exception {
        when(jwtTokenService.authenticate(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"wrong\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_shouldReturn400WhenValidationFails() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
