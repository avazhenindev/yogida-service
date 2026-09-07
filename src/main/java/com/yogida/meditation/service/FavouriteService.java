package com.yogida.meditation.service;

import com.yogida.meditation.dto.FavouriteDto;
import com.yogida.meditation.entity.FavouriteEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.mapper.FavouriteMapper;
import com.yogida.meditation.repository.FavouriteRepository;
import com.yogida.meditation.service.api.FavouriteApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class FavouriteService implements FavouriteApi {

    private final FavouriteRepository favouriteRepository;
    private final FavouriteMapper favouriteMapper;
    private final CurrentUserService currentUserService;

    /**
     * Every method here is scoped to the authenticated caller.
     *
     * <p>They used to operate on any row by id, with identity taken from the request body and
     * only checked for existence — so a signed-in user could list, read, edit and delete
     * anyone's favourites, and create favourites owned by someone else. Rows belonging to
     * another user now report 404 rather than 403, so ids stay unenumerable.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FavouriteDto> findAll() {
        return favouriteRepository.findByUserUserId(currentUserId()).stream()
                .map(favouriteMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FavouriteDto findById(Long id) {
        return favouriteMapper.toDto(findOwnedOrThrow(id));
    }

    /**
     * Create a favourite. Idempotent: if a duplicate favourite already exists for the same
     * user/content pair, returns the existing favourite instead of creating a new one.
     */
    @Override
    @Transactional
    public FavouriteDto create(FavouriteDto dto) {
        // Ownership comes from the token. Whatever userId the body carries is ignored.
        Long ownerId = currentUserId();
        dto.setUserId(ownerId);
        validateContentData(dto);

        // Check for existing favourite
        var existing = favouriteRepository.findByUserUserIdAndContentTypeAndContentId(
                ownerId,
                dto.getContentType(),
                dto.getContentId()
        );

        if (existing.isPresent()) {
            log.info("FavouriteService > Duplicate favourite found for userId={}, contentType={}, contentId={}. Returning existing favourite.",
                    dto.getUserId(), dto.getContentType(), dto.getContentId());
            return favouriteMapper.toDto(existing.get());
        }

        // Create new favourite
        FavouriteEntity entity = favouriteMapper.toEntity(dto);
        entity.setFavouriteId(null);
        entity.setCreatedAt(LocalDateTime.now());
        FavouriteEntity saved = favouriteRepository.save(entity);
        log.info("FavouriteService > Created favourite with id: {}", saved.getFavouriteId());
        return favouriteMapper.toDto(saved);
    }

    @Override
    @Transactional
    public FavouriteDto update(Long id, FavouriteDto dto) {
        FavouriteEntity existing = findOwnedOrThrow(id);
        // The mapper ignores the owner, so an update cannot reassign the row.
        favouriteMapper.updateEntity(dto, existing);
        FavouriteEntity saved = favouriteRepository.save(existing);
        log.info("FavouriteService > Updated favourite with id: {}", saved.getFavouriteId());
        return favouriteMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FavouriteEntity owned = findOwnedOrThrow(id);
        favouriteRepository.delete(owned);
        log.info("FavouriteService > Deleted favourite with id: {}", id);
    }



    private Long currentUserId() {
        return currentUserService.getCurrentUserOrThrow().getUserId();
    }

    /**
     * Loads a favourite only if it belongs to the caller. Reports "not found" for someone
     * else's row, deliberately: a 403 would confirm the id exists.
     */
    private FavouriteEntity findOwnedOrThrow(Long id) {
        return favouriteRepository.findById(id)
                .filter(f -> f.getUser() != null && currentUserId().equals(f.getUser().getUserId()))
                .orElseThrow(() -> new EntityNotFoundException("Favourite", id));
    }

    private void validateContentData(FavouriteDto dto) {
        if (dto.getContentType() == null || dto.getContentType().isBlank()) {
            throw new IllegalArgumentException("Content type cannot be null or empty");
        }
        if (dto.getContentId() == null) {
            throw new IllegalArgumentException("Content ID cannot be null");
        }
    }
}

