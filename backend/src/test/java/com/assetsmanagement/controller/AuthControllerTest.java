package com.assetsmanagement.controller;

import com.assetsmanagement.config.JwtConfig;
import com.assetsmanagement.config.SecurityConfig;
import com.assetsmanagement.config.SecurityBeans;
import com.assetsmanagement.dto.request.LoginRequest;
import com.assetsmanagement.dto.response.LoginResponse;
import com.assetsmanagement.dto.response.RegisterResponse;
import com.assetsmanagement.dto.response.RoleResponse;
import com.assetsmanagement.dto.response.UserResponse;
import com.assetsmanagement.exception.BadRequestException;
import com.assetsmanagement.repository.UserRepository;
import com.assetsmanagement.service.AuthService;
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
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    private UserResponse buildUserResponse() {
        return new UserResponse(1L, "admin", "admin@test.com",
                Set.of(new RoleResponse(1L, "ROLE_USER", "User", true, 0)),
                true, "admin", LocalDateTime.now(), "admin", LocalDateTime.now(), 0);
    }

    private LoginResponse buildLoginResponse() {
        return new LoginResponse("test-jwt-token", buildUserResponse());
    }

    // --- /login ---

    @Test
    void login_shouldReturnTokenWhenCredentialsValid() throws Exception {
        when(jwtTokenService.authenticate(any(LoginRequest.class))).thenReturn(buildLoginResponse());

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

    // --- /register ---

    @Test
    void register_shouldReturn201WhenSuccessful() throws Exception {
        when(authService.register(any())).thenReturn(new RegisterResponse("User registered successfully."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"email\":\"new@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully."));
    }

    @Test
    void register_shouldReturn400WhenUsernameTaken() throws Exception {
        when(authService.register(any())).thenThrow(new BadRequestException("Username already taken: newuser"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"email\":\"new@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn400WhenValidationFails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ab\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- /social-login ---

    @Test
    void socialLogin_shouldReturn200WithToken() throws Exception {
        when(authService.socialLogin(any())).thenReturn(buildLoginResponse());

        mockMvc.perform(post("/api/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"google\",\"token\":\"valid-google-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("test-jwt-token"));
    }

    @Test
    void socialLogin_shouldReturn400ForInvalidToken() throws Exception {
        when(authService.socialLogin(any())).thenThrow(new BadRequestException("Invalid Google token."));

        mockMvc.perform(post("/api/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"google\",\"token\":\"bad-token\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- /forgot-password ---

    @Test
    void forgotPassword_shouldAlwaysReturn200() throws Exception {
        when(authService.forgotPassword(any()))
                .thenReturn(new RegisterResponse("If that email is registered, a reset link has been sent."));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If that email is registered, a reset link has been sent."));
    }

    @Test
    void forgotPassword_shouldReturn400WhenInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- /reset-password ---

    @Test
    void resetPassword_shouldReturn200WhenSuccessful() throws Exception {
        when(authService.resetPassword(any()))
                .thenReturn(new RegisterResponse("Password has been reset successfully."));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"some-uuid-token\",\"newPassword\":\"newpassword123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password has been reset successfully."));
    }

    @Test
    void resetPassword_shouldReturn400ForExpiredToken() throws Exception {
        when(authService.resetPassword(any()))
                .thenThrow(new BadRequestException("Invalid or expired reset token."));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"expired-token\",\"newPassword\":\"newpassword123\"}"))
                .andExpect(status().isBadRequest());
    }
}
