package com.yogida.meditation.service;

import com.yogida.meditation.dto.ProfileDto;
import com.yogida.meditation.entity.ProfileEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.mapper.ProfileMapper;
import com.yogida.meditation.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final CurrentUserService currentUserService;

    /**
     * Scoped to the authenticated caller, like favourites.
     *
     * <p>These methods used to read, write and delete any profile by id, and take the owning
     * user from the request body. Someone else's profile now reports 404 rather than 403 so
     * ids stay unenumerable.
     *
     * <p>The write paths also had no {@code @Transactional} at all, so a multi-statement
     * update had no atomicity and every call ran in its own auto-commit.
     */
    @Transactional(readOnly = true)
    public List<ProfileDto> findAll() {
        return profileRepository.findByUserUserId(currentUserService.getCurrentUserId()).stream()
                .map(profileMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfileDto findById(Long id) {
        return profileMapper.toDto(findOwnedOrThrow(id));
    }

    /**
     * Creates the caller's profile, or returns the one they already have.
     *
     * <p>Idempotent by necessity, not by preference. {@code JwtUserProvisioner} creates a
     * profile the first time it sees any token, and {@code CurrentUserService} provisions on
     * every authenticated request — so by the time anyone can call this endpoint they already
     * have a profile. Left as a plain insert, it was the source of the duplicate rows that
     * make {@code profile} a 1:1 in the entity model and a 1:N in the database; once the unique
     * constraint exists it would instead be a permanent 409 for every caller.
     *
     * <p>Mirrors the same decision already made in {@code FavouriteService.create}.
     */
    @Transactional
    public ProfileDto create(ProfileDto dto) {
        Long userId = currentUserService.getCurrentUserId();

        Optional<ProfileEntity> existing = profileRepository.findFirstByUserUserIdOrderByProfileIdAsc(userId);
        if (existing.isPresent()) {
            log.debug("ProfileService > Profile already exists for user {}; returning it", userId);
            return profileMapper.toDto(existing.get());
        }

        // Ownership comes from the token; the body's userId is ignored.
        dto.setUserId(userId);
        ProfileEntity entity = profileMapper.toEntity(dto);
        entity.setProfileId(null);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        ProfileEntity saved = profileRepository.save(entity);
        log.info("ProfileService > Created profile with id: {}", saved.getProfileId());
        return profileMapper.toDto(saved);
    }

    @Transactional
    public ProfileDto update(Long id, ProfileDto dto) {
        ProfileEntity existing = findOwnedOrThrow(id);
        // The mapper ignores the owner, so an update cannot move the profile to another user.
        profileMapper.updateEntity(dto, existing);
        existing.setUpdatedAt(LocalDateTime.now());
        ProfileEntity saved = profileRepository.save(existing);
        log.info("ProfileService > Updated profile with id: {}", saved.getProfileId());
        return profileMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        ProfileEntity owned = findOwnedOrThrow(id);
        profileRepository.delete(owned);
        log.info("ProfileService > Deleted profile with id: {}", id);
    }


    private ProfileEntity findOwnedOrThrow(Long id) {
        return profileRepository.findById(id)
                .filter(p -> p.getUser() != null && currentUserService.getCurrentUserId().equals(p.getUser().getUserId()))
                .orElseThrow(() -> new EntityNotFoundException("Profile", id));
    }
}

