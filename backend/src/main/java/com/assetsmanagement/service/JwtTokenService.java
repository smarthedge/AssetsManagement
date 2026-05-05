package com.assetsmanagement.service;

import com.assetsmanagement.dto.request.LoginRequest;
import com.assetsmanagement.dto.response.LoginResponse;
import com.assetsmanagement.dto.response.RoleResponse;
import com.assetsmanagement.dto.response.UserResponse;
import com.assetsmanagement.entity.User;
import com.assetsmanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

/**
 * Handles JWT token generation and authentication.
 */
@Service
public class JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);
    private static final long ACCESS_TOKEN_EXPIRY_MINUTES = 60;

    private final JwtEncoder jwtEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public JwtTokenService(JwtEncoder jwtEncoder, AuthenticationManager authenticationManager,
                           UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.jwtEncoder = jwtEncoder;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates a user via username/password and returns a signed JWT access token.
     *
     * @param request login credentials
     * @return LoginResponse containing the JWT and user details
     */
    public LoginResponse authenticate(LoginRequest request) {
        log.info("Authenticating user: {}", request.username());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.error("User '{}' authenticated but not found in database", request.username());
                    return new IllegalStateException("Authenticated user not found");
                });

        String token = generateToken(authentication);
        log.info("User '{}' authenticated successfully, token generated", request.username());

        return new LoginResponse(token, toUserResponse(user));
    }

    public LoginResponse generateTokenForUser(User user) {
        log.info("Generating token for user: {}", user.getUsername());
        var authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority(r.getName()))
                .collect(Collectors.toList());
        var auth = new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
        String token = generateToken(auth);
        return new LoginResponse(token, toUserResponse(user));
    }

    /**
     * Generates a JWT access token containing the user's authorities as claims.
     */
    private String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        Instant expiry = now.plus(ACCESS_TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES);

        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("assets-management-api")
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(authentication.getName())
                .claim("scope", scope)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private UserResponse toUserResponse(User user) {
        var roles = user.getRoles().stream()
                .map(r -> new RoleResponse(r.getId(), r.getName(), r.getDescription(), r.getStatus(), r.getVersion()))
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(), user.getUsername(), user.getEmail(), roles,
                user.getStatus(), user.getCreatedByUserId(), user.getCreatedByUsername(),
                user.getCreatedDateTime(), user.getLastChangedByUserId(), user.getLastChangedByUsername(),
                user.getLastChangedDateTime(), user.getVersion());
    }
}
