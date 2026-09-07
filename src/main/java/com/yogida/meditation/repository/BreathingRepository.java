package com.yogida.meditation.repository;

import com.yogida.meditation.entity.BreathingEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BreathingRepository extends JpaRepository<BreathingEntity, Long> {

    /**
     * Exactly two attribute paths. Adding {@code phases.audioFiles} would be a second collection
     * in the same fetch and throw MultipleBagFetchException; the nested audio and its S3 objects
     * are batched by {@code @BatchSize} instead.
     */
    @EntityGraph(attributePaths = {"iconObject", "phases"})
    List<BreathingEntity> findAllByOrderByDisplayOrderAsc();

    @EntityGraph(attributePaths = {"iconObject", "phases"})
    Optional<BreathingEntity> findWithGraphById(Long id);
}
