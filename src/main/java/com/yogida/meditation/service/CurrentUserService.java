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
    private final JwtUserProvisioner provisioner;

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
     * Resolves the current authenticated user, creating the local record on first sight.
     *
     * <p>A valid token whose subject had no row used to throw, which the error handler turned
     * into a 500 — and provisioning was the mobile app's job, done over endpoints that let it
     * claim any identity. The token is authoritative now: if the identity provider vouches for
     * this subject, the row is created here.
     *
     * @throws IllegalStateException when there is no authenticated JWT at all
     */
    @Transactional
    public AppUserEntity getCurrentUserOrThrow() {
        return currentJwt()
            .map(provisioner::provision)
            .orElseThrow(() -> new IllegalStateException(
                "Current user not found or not authenticated"
            ));
    }

    /** The verified JWT of the caller, if this request carries one. */
    private Optional<Jwt> currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        String subject = jwt.getSubject();
        return subject == null || subject.isBlank() ? Optional.empty() : Optional.of(jwt);
    }


}
