package com.assetsmanagement.controller;

import com.assetsmanagement.dto.request.ForgotPasswordRequest;
import com.assetsmanagement.dto.request.LoginRequest;
import com.assetsmanagement.dto.request.RegisterRequest;
import com.assetsmanagement.dto.request.ResetPasswordRequest;
import com.assetsmanagement.dto.request.SocialLoginRequest;
import com.assetsmanagement.dto.response.ErrorResponse;
import com.assetsmanagement.dto.response.LoginResponse;
import com.assetsmanagement.dto.response.RegisterResponse;
import com.assetsmanagement.service.AuthService;
import com.assetsmanagement.service.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public auth endpoints — no authentication required.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login, registration, social auth, and password reset")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtTokenService jwtTokenService;
    private final AuthService authService;

    public AuthController(JwtTokenService jwtTokenService, AuthService authService) {
        this.jwtTokenService = jwtTokenService;
        this.authService = authService;
    }

    @Operation(summary = "Authenticate and obtain JWT access token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for user: {}", request.username());
        LoginResponse response = jwtTokenService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Register a new user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or username/email already taken",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for username: {}", request.username());
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Authenticate via social provider token (PKCE flow)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or unsupported provider token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/social-login")
    public ResponseEntity<LoginResponse> socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        log.info("Social login request for provider: {}", request.provider());
        LoginResponse response = authService.socialLogin(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Request a password reset email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reset email sent if address is registered",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class)))
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<RegisterResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.email());
        RegisterResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reset password using a valid reset token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired reset token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<RegisterResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request received");
        RegisterResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }
}
