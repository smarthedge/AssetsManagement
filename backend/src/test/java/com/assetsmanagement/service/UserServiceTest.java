package com.assetsmanagement.service;

import com.assetsmanagement.dto.request.UserRequest;
import com.assetsmanagement.dto.response.UserResponse;
import com.assetsmanagement.entity.Role;
import com.assetsmanagement.entity.User;
import com.assetsmanagement.exception.BadRequestException;
import com.assetsmanagement.exception.ResourceNotFoundException;
import com.assetsmanagement.repository.RoleRepository;
import com.assetsmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder()
                .id(1L)
                .name("ROLE_ADMIN")
                .description("Administrator")
                .build();
        adminRole.setStatus(true);
        adminRole.setVersion(0);
        adminRole.setCreatedDateTime(LocalDateTime.now());
        adminRole.setLastChangedDateTime(LocalDateTime.now());

        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$10$hashed")
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();
        user.setStatus(true);
        user.setCreatedByUsername("admin");
        user.setCreatedDateTime(LocalDateTime.now());
        user.setLastChangedByUsername("admin");
        user.setLastChangedDateTime(LocalDateTime.now());
        user.setVersion(0);
    }

    @Test
    void getUserById_shouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L);

        assertNotNull(response);
        assertEquals("testuser", response.username());
        assertEquals("test@example.com", response.email());
    }

    @Test
    void getUserById_shouldThrowWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    void createUser_shouldSaveWithEncodedPassword() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findAllById(any())).thenReturn(List.of(adminRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserRequest request = new UserRequest("newuser", "new@example.com", "password123", Set.of(1L));
        UserResponse response = userService.createUser(request);

        assertNotNull(response);
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldThrowWhenUsernameTaken() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        UserRequest request = new UserRequest("testuser", "new@example.com", "password123", null);

        assertThrows(BadRequestException.class, () -> userService.createUser(request));
    }

    @Test
    void deleteUser_shouldSoftDelete() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.deleteUser(1L);

        assertEquals(false, user.getStatus());
        verify(userRepository).save(user);
    }
}
