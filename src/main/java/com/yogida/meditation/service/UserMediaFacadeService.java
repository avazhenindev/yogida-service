package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.enums.MediaStatus;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.mapper.MediaUserMapper;
import com.yogida.meditation.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for user-facing media operations with entitlement applied.
 * Always resolves the current authenticated user and applies per-user DTO transformations.
 * Returns 401 for unauthenticated requests (handled by Spring Security).
 */
@Service
@RequiredArgsConstructor
public class UserMediaFacadeService {

    private final MediaRepository mediaRepository;
    private final CurrentUserService currentUserService;
    private final MediaUserMapper mediaUserMapper;

    /**
     * Returns all active media with entitlement applied for the current user.
     * Each media item has isPremium set based on the user's subscriptions.
     * Premium media without user entitlement have their URLs withheld.
     *
     * @return list of media DTOs with entitlement applied
     */
    @Transactional(readOnly = true)
    public List<MediaDto> findAllActive() {
        AppUserEntity currentUser = currentUserService.getCurrentUserOrThrow();
        List<MediaEntity> allActive = mediaRepository.findAllByStatusEquals(MediaStatus.ACTIVE);
        return mediaUserMapper.toDtoListForUser(allActive, currentUser);
    }

    /**
     * Returns a single active media by ID with entitlement applied for the current user.
     *
     * @param id the media ID
     * @return Optional containing the user-facing media DTO if found and active
     */
    @Transactional(readOnly = true)
    public Optional<MediaDto> findById(Long id) {
        AppUserEntity currentUser = currentUserService.getCurrentUserOrThrow();
        Optional<MediaEntity> media = mediaRepository.findById(id)
            .filter(m -> m.getStatus() == MediaStatus.ACTIVE);
        return media.map(entity -> mediaUserMapper.toDtoForUser(entity, currentUser));
    }

    /**
     * Returns a single media by ID or throws EntityNotFoundException if not found.
     *
     * @param id the media ID
     * @return the user-facing media DTO
     * @throws EntityNotFoundException if media not found
     */
    @Transactional(readOnly = true)
    public MediaDto findByIdOrThrow(Long id) {
        return findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Media", id));
    }
}
