package com.yogida.meditation.repository;

import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.enums.MediaStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaRepository extends JpaRepository<MediaEntity, Long> {

    @EntityGraph(attributePaths = "mediaObject")
    List<MediaEntity> findAllWithMediaObjectBy();

    List<MediaEntity> findAllByStatus(MediaStatus status);

    /**
     * Find all ACTIVE media with eager-loaded relationships for user-facing endpoints.
     */
    @EntityGraph(attributePaths = {"mediaObject", "mediaSubscriptions", "mediaSubscriptions.subscription"})
    List<MediaEntity> findAllByStatusEquals(MediaStatus status);

    /**
     * Find a single ACTIVE media by ID with eager-loaded subscriptions for detail screen.
     */
    @EntityGraph(attributePaths = {"mediaObject", "mediaSubscriptions", "mediaSubscriptions.subscription"})
    Optional<MediaEntity> findById(Long id);
}
