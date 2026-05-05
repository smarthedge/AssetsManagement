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
import com.assetsmanagement.exception.ResourceNotFoundException;
import com.assetsmanagement.repository.RoleRepository;
import com.assetsmanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String DEFAULT_ROLE = "ROLE_USER";
    private static final int RESET_TOKEN_EXPIRY_HOURS = 1;
    private static final int USERNAME_SUFFIX_MAX_ATTEMPTS = 10;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final SocialTokenVerifier socialTokenVerifier;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.mail.reset-password-base-url}")
    private String resetPasswordBaseUrl;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService,
                       SocialTokenVerifier socialTokenVerifier, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.socialTokenVerifier = socialTokenVerifier;
        this.mailSender = mailSender;
    }

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already registered: " + request.email());
        }
        Role userRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", DEFAULT_ROLE));

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .roles(Set.of(userRole))
                .build();
        userRepository.save(user);
        log.info("User registered: username={}", user.getUsername());
        return new RegisterResponse("User registered successfully.");
    }

    public LoginResponse socialLogin(SocialLoginRequest request) {
        String provider = request.provider().toLowerCase();
        SocialTokenVerifier.SocialUserInfo info = socialTokenVerifier.verify(provider, request.token());

        Optional<User> existing = userRepository.findByProviderAndProviderAccountId(provider, info.providerAccountId());
        User user;
        if (existing.isPresent()) {
            user = existing.get();
            if (Boolean.FALSE.equals(user.getStatus())) {
                throw new BadRequestException("Account is disabled.");
            }
        } else {
            user = findOrCreateSocialUser(provider, info);
        }
        log.info("Social login: provider={}, userId={}", provider, user.getId());
        return jwtTokenService.generateTokenForUser(user);
    }

    private User findOrCreateSocialUser(String provider, SocialTokenVerifier.SocialUserInfo info) {
        Optional<User> byEmail = userRepository.findByEmail(info.email());
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            existing.setProvider(provider);
            existing.setProviderAccountId(info.providerAccountId());
            return userRepository.save(existing);
        }
        Role userRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", DEFAULT_ROLE));
        String username = deriveUniqueUsername(info.email());
        User newUser = User.builder()
                .username(username)
                .email(info.email())
                .provider(provider)
                .providerAccountId(info.providerAccountId())
                .roles(Set.of(userRole))
                .build();
        return userRepository.save(newUser);
    }

    private String deriveUniqueUsername(String email) {
        String base = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        base = base.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
        if (base.length() > 45) {
            base = base.substring(0, 45);
        }
        if (!userRepository.existsByUsername(base)) {
            return base;
        }
        for (int i = 1; i <= USERNAME_SUFFIX_MAX_ATTEMPTS; i++) {
            String candidate = base + i;
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }
        throw new BadRequestException("Unable to auto-generate a unique username. Please register manually.");
    }

    public RegisterResponse forgotPassword(ForgotPasswordRequest request) {
        Optional<User> optUser = userRepository.findByEmail(request.email());
        String genericMessage = "If that email is registered, a reset link has been sent.";
        if (optUser.isEmpty() || Boolean.FALSE.equals(optUser.get().getStatus())) {
            log.warn("Forgot password request for unknown/inactive email: {}", request.email());
            return new RegisterResponse(genericMessage);
        }
        User user = optUser.get();
        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpires(LocalDateTime.now().plusHours(RESET_TOKEN_EXPIRY_HOURS));
        userRepository.save(user);
        log.info("Password reset token generated for user: {}", user.getEmail());
        sendResetEmail(user.getEmail(), token);
        return new RegisterResponse(genericMessage);
    }

    public RegisterResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.token())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token."));
        if (user.getPasswordResetExpires() == null
                || LocalDateTime.now().isAfter(user.getPasswordResetExpires())) {
            throw new BadRequestException("Invalid or expired reset token.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpires(null);
        userRepository.save(user);
        log.info("Password reset completed for user: {}", user.getEmail());
        return new RegisterResponse("Password has been reset successfully.");
    }

    private void sendResetEmail(String email, String token) {
        try {
            String resetLink = resetPasswordBaseUrl + "?token=" + token;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("Reset your password");
            message.setText("Click the link below to reset your password. This link expires in "
                    + RESET_TOKEN_EXPIRY_HOURS + " hour(s).\n\n" + resetLink
                    + "\n\nIf you did not request a password reset, please ignore this email.");
            mailSender.send(message);
            log.info("Password reset email sent to: {}", email);
        } catch (MailException ex) {
            log.error("Failed to send password reset email to {}: {}", email, ex.getMessage());
        }
    }
}
