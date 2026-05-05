package com.assetsmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

/**
 * Base entity providing audit fields, soft-delete status, and optimistic locking.
 * All domain entities extend this class to inherit consistent audit behavior.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    private static final Logger log = LoggerFactory.getLogger(BaseEntity.class);

    @Column(name = "created_by_username", length = 50)
    private String createdByUsername;

    @Column(name = "created_datetime", nullable = false, updatable = false)
    private LocalDateTime createdDateTime;

    @Column(name = "last_changed_by_username", length = 50)
    private String lastChangedByUsername;

    @Column(name = "last_changed_datetime", nullable = false)
    private LocalDateTime lastChangedDateTime;

    /**
     * Soft-delete flag. When false, the record is considered deleted.
     */
    @Column(name = "status", nullable = false)
    private Boolean status = true;

    /**
     * Optimistic locking version. Incremented by JPA on each update.
     * Stale updates throw OptimisticLockException, resulting in HTTP 409.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @PrePersist
    protected void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdDateTime = now;
        this.lastChangedDateTime = now;
        populateAuditFields();
        log.trace("Entity {} pre-persist: audit fields populated", this.getClass().getSimpleName());
    }

    @PreUpdate
    protected void preUpdate() {
        this.lastChangedDateTime = LocalDateTime.now();
        populateLastChangedFields();
        log.trace("Entity {} pre-update: last-changed audit fields refreshed", this.getClass().getSimpleName());
    }

    private void populateAuditFields() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            this.createdByUsername = auth.getName();
            this.lastChangedByUsername = auth.getName();
        }
    }

    private void populateLastChangedFields() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            this.lastChangedByUsername = auth.getName();
        }
    }
}
