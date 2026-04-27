package com.yogida.meditation.repository;

import com.yogida.meditation.entity.MediaCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MediaCategoryRepository extends JpaRepository<MediaCategoryEntity, Long> {

    Optional<MediaCategoryEntity> findByName(String name);

    boolean existsByName(String name);
}

