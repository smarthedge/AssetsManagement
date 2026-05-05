package com.assetsmanagement.service;

import com.assetsmanagement.dto.PageResponse;
import com.assetsmanagement.dto.request.UserRequest;
import com.assetsmanagement.dto.response.RoleResponse;
import com.assetsmanagement.dto.response.UserResponse;
import com.assetsmanagement.entity.Role;
import com.assetsmanagement.entity.User;
import com.assetsmanagement.exception.BadRequestException;
import com.assetsmanagement.exception.ResourceNotFoundException;
import com.assetsmanagement.repository.RoleRepository;
import com.assetsmanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages user lifecycle with audit tracking, soft delete, and optimistic locking.
 */
@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        log.info("Fetching users — page: {}, size: {}", page, size);
        Page<User> userPage = userRepository.findByStatusTrue(
                PageRequest.of(page, size, Sort.by("username").ascending()));

        return new PageResponse<>(
                userPage.map(this::toResponse).getContent(),
                userPage.getNumber(), userPage.getSize(),
                userPage.getTotalElements(), userPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.info("Fetching user by ID: {}", id);
        return toResponse(findActiveUser(id));
    }

    /**
     * Creates a new user with encoded password and assigned roles.
     */
    public UserResponse createUser(UserRequest request) {
        log.info("Creating user: {}", request.username());

        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already registered: " + request.email());
        }

        Set<Role> roles = resolveRoles(request.roleIds());

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .roles(roles)
                .build();

        User saved = userRepository.save(user);
        log.info("User created: ID={}, username={}", saved.getId(), saved.getUsername());
        return toResponse(saved);
    }

    /**
     * Updates an existing user. Version checking handles optimistic locking.
     */
    public UserResponse updateUser(Long id, UserRequest request) {
        log.info("Updating user ID: {}", id);
        User user = findActiveUser(id);

        if (!user.getUsername().equals(request.username())
                && userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already taken: " + request.username());
        }
        if (!user.getEmail().equals(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already registered: " + request.email());
        }

        user.setUsername(request.username());
        user.setEmail(request.email());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.roleIds() != null) {
            user.setRoles(resolveRoles(request.roleIds()));
        }

        User saved = userRepository.save(user);
        log.info("User updated: ID={}, username={}, version={}", saved.getId(), saved.getUsername(), saved.getVersion());
        return toResponse(saved);
    }

    /**
     * Soft-deletes a user by setting status=false.
     */
    public void deleteUser(Long id) {
        log.info("Soft-deleting user ID: {}", id);
        User user = findActiveUser(id);
        user.setStatus(false);
        userRepository.save(user);
        log.info("User soft-deleted: ID={}, username={}", user.getId(), user.getUsername());
    }

    private User findActiveUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        if (Boolean.FALSE.equals(user.getStatus())) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        return user;
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
        if (roles.size() != roleIds.size()) {
            throw new BadRequestException("One or more role IDs are invalid");
        }
        return roles;
    }

    private UserResponse toResponse(User user) {
        var roles = user.getRoles().stream()
                .map(r -> new RoleResponse(r.getId(), r.getName(), r.getDescription(), r.getStatus(), r.getVersion()))
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(), user.getUsername(), user.getEmail(), roles,
                user.getStatus(), user.getCreatedByUsername(),
                user.getCreatedDateTime(), user.getLastChangedByUsername(),
                user.getLastChangedDateTime(), user.getVersion());
    }
}
