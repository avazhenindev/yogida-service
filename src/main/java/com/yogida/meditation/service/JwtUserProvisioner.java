package com.yogida.meditation.service;

import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.ProfileEntity;
import com.yogida.meditation.repository.AppUserRepository;
import com.yogida.meditation.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Creates the local record for a user the identity provider already knows.
 *
 * <p>Provisioning used to happen in the mobile app: it searched every account by email through
 * a public endpoint, then posted a {@code keycloakUserId} it chose itself. That made the user
 * directory readable by anyone signed in, let a caller claim any subject, and left the whole
 * thing racy — two requests during a cold start both saw "no user" and both tried to create
 * one. It also never created a {@link ProfileEntity}, so the profile feature had no rows to
 * work with on either side.
 *
 * <p>Identity now comes from the verified token and nowhere else.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtUserProvisioner {

    private final AppUserRepository appUserRepository;
    private final ProfileRepository profileRepository;

    /**
     * Returns the user for this token, creating it on first sight.
     *
     * <p>Runs in its own transaction: the caller is usually inside a read-only one, and an
     * insert there would fail. {@code REQUIRES_NEW} also means the row is committed and
     * visible even if the surrounding request later rolls back.
     *
     * <p>Two concurrent first requests can both miss the lookup. The unique constraint on
     * {@code keycloak_user_id} settles it, and the loser reads the winner's row rather than
     * failing — which is what made the client-side version flaky.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AppUserEntity provision(Jwt jwt) {
        String subject = jwt.getSubject();
        return appUserRepository.findByKeycloakUserId(subject)
            .orElseGet(() -> createFor(jwt, subject));
    }

    private AppUserEntity createFor(Jwt jwt, String subject) {
        AppUserEntity user = new AppUserEntity();
        user.setKeycloakUserId(subject);
        user.setEmail(claim(jwt, "email"));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());

        try {
            AppUserEntity saved = appUserRepository.save(user);
            createProfileFor(saved);
            log.info("JwtUserProvisioner > Provisioned user for subject {}", subject);
            return saved;
        } catch (DataIntegrityViolationException raced) {
            // Another request won. Its row is the right one.
            log.debug("JwtUserProvisioner > Lost the provisioning race for {}; reading the winner", subject);
            return appUserRepository.findByKeycloakUserId(subject)
                .orElseThrow(() -> raced);
        }
    }

    private void createProfileFor(AppUserEntity user) {
        ProfileEntity profile = new ProfileEntity();
        profile.setUser(user);
        profile.setTwoFactorEnabled(false);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);
    }

    private static String claim(Jwt jwt, String name) {
        Object value = jwt.getClaim(name);
        return value == null ? null : value.toString();
    }
}
