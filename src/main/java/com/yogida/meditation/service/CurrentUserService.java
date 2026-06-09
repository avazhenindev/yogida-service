package com.yogida.meditation.service;

import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for resolving the current authenticated user from JWT token.
 * Extracts the 'sub' claim (subject/user ID) and looks up the AppUserEntity.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    /**
     * Resolves the current authenticated user from the JWT token.
     * The 'sub' claim contains the Keycloak user ID.
     *
     * @return Optional containing the AppUserEntity if authenticated and found in database
     */
    @Transactional(readOnly = true)
    public Optional<AppUserEntity> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }

        String keycloakUserId = jwt.getSubject();
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            return Optional.empty();
        }

        return appUserRepository.findByKeycloakUserId(keycloakUserId);
    }

    /**
     * Resolves the current authenticated user or throws an exception if not found.
     *
     * @return the AppUserEntity for the current authenticated user
     * @throws IllegalStateException if user is not authenticated or not found in database
     */
    @Transactional(readOnly = true)
    public AppUserEntity getCurrentUserOrThrow() {
        return getCurrentUser()
            .orElseThrow(() -> new IllegalStateException(
                "Current user not found or not authenticated"
            ));
    }

    /**
     * Returns the Keycloak user ID (JWT 'sub' claim) of the current user.
     *
     * @return the Keycloak user ID if authenticated
     */
    public Optional<String> getCurrentKeycloakUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }

        String sub = jwt.getSubject();
        return sub != null && !sub.isBlank() ? Optional.of(sub) : Optional.empty();
    }
}
