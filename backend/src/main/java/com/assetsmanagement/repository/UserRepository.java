package com.assetsmanagement.repository;

import com.assetsmanagement.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<User> findByStatusTrue(Pageable pageable);

    Optional<User> findByProviderAndProviderAccountId(String provider, String providerAccountId);

    Optional<User> findByPasswordResetToken(String passwordResetToken);
}
