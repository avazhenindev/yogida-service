package com.yogida.meditation.repository;

import com.yogida.meditation.entity.FavouriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavouriteRepository extends JpaRepository<FavouriteEntity, Long> {

    /**
     * Find favourites by user ID.
     */
    List<FavouriteEntity> findByUserUserId(Long userId);

    /**
     * Find a favourite by user ID, content type, and content ID.
     * Used for duplicate detection and delete operations.
     */
    Optional<FavouriteEntity> findByUserUserIdAndContentTypeAndContentId(Long userId, String contentType, Long contentId);

    /**
     * Check if a favourite exists for the given user, content type, and content ID.
     */
    boolean existsByUserUserIdAndContentTypeAndContentId(Long userId, String contentType, Long contentId);
}

