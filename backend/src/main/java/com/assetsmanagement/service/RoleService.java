package com.assetsmanagement.service;

import com.assetsmanagement.dto.PageResponse;
import com.assetsmanagement.dto.request.RoleRequest;
import com.assetsmanagement.dto.response.RoleResponse;
import com.assetsmanagement.entity.Role;
import com.assetsmanagement.exception.ResourceNotFoundException;
import com.assetsmanagement.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages role lifecycle with audit tracking and soft delete.
 */
@Service
@Transactional
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<RoleResponse> getAllRoles(int page, int size) {
        log.info("Fetching roles — page: {}, size: {}", page, size);
        Page<Role> rolePage = roleRepository.findByStatusTrue(
                PageRequest.of(page, size, Sort.by("name").ascending()));

        return new PageResponse<>(
                rolePage.map(this::toResponse).getContent(),
                rolePage.getNumber(), rolePage.getSize(),
                rolePage.getTotalElements(), rolePage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        log.info("Fetching role by ID: {}", id);
        return toResponse(findActiveRole(id));
    }

    public RoleResponse createRole(RoleRequest request) {
        log.info("Creating role: {}", request.name());
        Role role = Role.builder()
                .name(request.name())
                .description(request.description())
                .build();
        Role saved = roleRepository.save(role);
        log.info("Role created: ID={}, name={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    public RoleResponse updateRole(Long id, RoleRequest request) {
        log.info("Updating role ID: {}", id);
        Role role = findActiveRole(id);
        role.setName(request.name());
        role.setDescription(request.description());
        Role saved = roleRepository.save(role);
        log.info("Role updated: ID={}, name={}, version={}", saved.getId(), saved.getName(), saved.getVersion());
        return toResponse(saved);
    }

    public void deleteRole(Long id) {
        log.info("Soft-deleting role ID: {}", id);
        Role role = findActiveRole(id);
        role.setStatus(false);
        roleRepository.save(role);
        log.info("Role soft-deleted: ID={}, name={}", role.getId(), role.getName());
    }

    private Role findActiveRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
        if (Boolean.FALSE.equals(role.getStatus())) {
            throw new ResourceNotFoundException("Role", "id", id);
        }
        return role;
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(), role.getName(), role.getDescription(),
                role.getStatus(), role.getVersion());
    }
}
