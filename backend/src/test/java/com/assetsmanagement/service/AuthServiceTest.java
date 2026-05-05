package com.assetsmanagement.service;

import com.assetsmanagement.dto.request.ForgotPasswordRequest;
import com.assetsmanagement.dto.request.RegisterRequest;
import com.assetsmanagement.dto.request.ResetPasswordRequest;
import com.assetsmanagement.dto.request.SocialLoginRequest;
import com.assetsmanagement.dto.response.LoginResponse;
import com.assetsmanagement.dto.response.RegisterResponse;
import com.assetsmanagement.entity.Role;
import com.assetsmanagement.entity.User;
import com.assetsmanagement.exception.BadRequestException;
import com.assetsmanagement.repository.RoleRepository;
import com.assetsmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private SocialTokenVerifier socialTokenVerifier;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AuthService authService;

    private Role userRole;
    private User activeUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "mailFrom", "noreply@test.com");
        ReflectionTestUtils.setField(authService, "resetPasswordBaseUrl", "http://localhost:4200/reset-password");

        userRole = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .description("Standard user")
                .build();
        userRole.setStatus(true);
        userRole.setVersion(0);
        userRole.setCreatedDateTime(LocalDateTime.now());
        userRole.setLastChangedDateTime(LocalDateTime.now());

        activeUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$10$hashed")
                .roles(new HashSet<>(Set.of(userRole)))
                .build();
        activeUser.setStatus(true);
        activeUser.setVersion(0);
        activeUser.setCreatedDateTime(LocalDateTime.now());
        activeUser.setLastChangedDateTime(LocalDateTime.now());
    }

    // -------------------------------------------------------------------------
    // register tests
    // -------------------------------------------------------------------------

    @Test
    void register_shouldSaveUserWithEncodedPassword() {
        RegisterRequest request = new RegisterRequest("newuser", "new@example.com", "password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("User registered successfully.", response.message());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("newuser", savedUser.getUsername());
        assertEquals("new@example.com", savedUser.getEmail());
        assertEquals("$2a$10$encoded", savedUser.getPasswordHash());
        assertTrue(savedUser.getRoles().contains(userRole));

        verify(passwordEncoder).encode("password123");
        verify(roleRepository).findByName("ROLE_USER");
    }

    @Test
    void register_shouldThrowWhenUsernameTaken() {
        RegisterRequest request = new RegisterRequest("takenuser", "new@example.com", "password123");

        when(userRepository.existsByUsername("takenuser")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.register(request));

        assertTrue(ex.getMessage().contains("takenuser"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldThrowWhenEmailTaken() {
        RegisterRequest request = new RegisterRequest("newuser", "taken@example.com", "password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.register(request));

        assertTrue(ex.getMessage().contains("taken@example.com"));
        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // socialLogin tests
    // -------------------------------------------------------------------------

    @Test
    void socialLogin_shouldReturnTokenForExistingUser() {
        SocialLoginRequest request = new SocialLoginRequest("google", "valid-google-token");
        SocialTokenVerifier.SocialUserInfo info =
                new SocialTokenVerifier.SocialUserInfo("google-id-123", "test@example.com", "Test User");
        LoginResponse expectedLogin = new LoginResponse("jwt-token", null);

        when(socialTokenVerifier.verify("google", "valid-google-token")).thenReturn(info);
        when(userRepository.findByProviderAndProviderAccountId("google", "google-id-123"))
                .thenReturn(Optional.of(activeUser));
        when(jwtTokenService.generateTokenForUser(activeUser)).thenReturn(expectedLogin);

        LoginResponse response = authService.socialLogin(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.accessToken());
        verify(jwtTokenService).generateTokenForUser(activeUser);
    }

    @Test
    void socialLogin_shouldThrowForDisabledUser() {
        SocialLoginRequest request = new SocialLoginRequest("google", "valid-google-token");
        SocialTokenVerifier.SocialUserInfo info =
                new SocialTokenVerifier.SocialUserInfo("google-id-123", "disabled@example.com", "Disabled User");

        User disabledUser = User.builder()
                .id(2L)
                .username("disableduser")
                .email("disabled@example.com")
                .build();
        disabledUser.setStatus(false);
        disabledUser.setVersion(0);

        when(socialTokenVerifier.verify("google", "valid-google-token")).thenReturn(info);
        when(userRepository.findByProviderAndProviderAccountId("google", "google-id-123"))
                .thenReturn(Optional.of(disabledUser));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.socialLogin(request));

        assertEquals("Account is disabled.", ex.getMessage());
        verify(jwtTokenService, never()).generateTokenForUser(any());
    }

    @Test
    void socialLogin_shouldCreateNewUserWhenNotFound() {
        SocialLoginRequest request = new SocialLoginRequest("github", "valid-github-token");
        SocialTokenVerifier.SocialUserInfo info =
                new SocialTokenVerifier.SocialUserInfo("gh-id-456", "newgithub@example.com", "New GitHub User");
        LoginResponse expectedLogin = new LoginResponse("jwt-token-new", null);

        when(socialTokenVerifier.verify("github", "valid-github-token")).thenReturn(info);
        when(userRepository.findByProviderAndProviderAccountId("github", "gh-id-456"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("newgithub@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.existsByUsername("newgithub")).thenReturn(false);

        User savedNewUser = User.builder()
                .id(3L)
                .username("newgithub")
                .email("newgithub@example.com")
                .provider("github")
                .providerAccountId("gh-id-456")
                .roles(new HashSet<>(Set.of(userRole)))
                .build();
        savedNewUser.setStatus(true);
        savedNewUser.setVersion(0);

        when(userRepository.save(any(User.class))).thenReturn(savedNewUser);
        when(jwtTokenService.generateTokenForUser(savedNewUser)).thenReturn(expectedLogin);

        LoginResponse response = authService.socialLogin(request);

        assertNotNull(response);
        assertEquals("jwt-token-new", response.accessToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals("github", capturedUser.getProvider());
        assertEquals("gh-id-456", capturedUser.getProviderAccountId());
        assertEquals("newgithub@example.com", capturedUser.getEmail());
    }

    // -------------------------------------------------------------------------
    // forgotPassword tests
    // -------------------------------------------------------------------------

    @Test
    void forgotPassword_shouldReturnGenericMessageForUnknownEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("unknown@example.com");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RegisterResponse response = authService.forgotPassword(request);

        assertNotNull(response);
        assertEquals("If that email is registered, a reset link has been sent.", response.message());
        verify(userRepository, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void forgotPassword_shouldSaveTokenAndSendEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        RegisterResponse response = authService.forgotPassword(request);

        assertNotNull(response);
        assertEquals("If that email is registered, a reset link has been sent.", response.message());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertNotNull(savedUser.getPasswordResetToken(), "passwordResetToken must be set");
        assertNotNull(savedUser.getPasswordResetExpires(), "passwordResetExpires must be set");
        assertTrue(savedUser.getPasswordResetExpires().isAfter(LocalDateTime.now()),
                "passwordResetExpires must be in the future");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    // -------------------------------------------------------------------------
    // resetPassword tests
    // -------------------------------------------------------------------------

    @Test
    void resetPassword_shouldUpdatePasswordAndClearToken() {
        String token = "valid-reset-token";
        ResetPasswordRequest request = new ResetPasswordRequest(token, "newpassword123");

        activeUser.setPasswordResetToken(token);
        activeUser.setPasswordResetExpires(LocalDateTime.now().plusHours(1));

        when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.encode("newpassword123")).thenReturn("$2a$10$newencoded");
        when(userRepository.save(any(User.class))).thenReturn(activeUser);

        RegisterResponse response = authService.resetPassword(request);

        assertNotNull(response);
        assertEquals("Password has been reset successfully.", response.message());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("$2a$10$newencoded", savedUser.getPasswordHash());
        assertNull(savedUser.getPasswordResetToken(), "passwordResetToken must be cleared");
        assertNull(savedUser.getPasswordResetExpires(), "passwordResetExpires must be cleared");

        verify(passwordEncoder).encode("newpassword123");
    }

    @Test
    void resetPassword_shouldThrowWhenTokenNotFound() {
        ResetPasswordRequest request = new ResetPasswordRequest("nonexistent-token", "newpassword123");

        when(userRepository.findByPasswordResetToken("nonexistent-token")).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.resetPassword(request));

        assertEquals("Invalid or expired reset token.", ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void resetPassword_shouldThrowWhenTokenExpired() {
        String token = "expired-reset-token";
        ResetPasswordRequest request = new ResetPasswordRequest(token, "newpassword123");

        activeUser.setPasswordResetToken(token);
        activeUser.setPasswordResetExpires(LocalDateTime.now().minusHours(2));

        when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(activeUser));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.resetPassword(request));

        assertEquals("Invalid or expired reset token.", ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }
}
