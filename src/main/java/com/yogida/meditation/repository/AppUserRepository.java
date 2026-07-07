package com.yogida.meditation.repository;

import com.yogida.meditation.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {

    Optional<AppUserEntity> findByEmail(String email);

    /**
     * Find an app user by their Keycloak user ID (JWT 'sub' claim).
     */
    Optional<AppUserEntity> findByKeycloakUserId(String keycloakUserId);
}

