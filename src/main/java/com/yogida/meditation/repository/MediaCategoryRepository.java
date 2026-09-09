package com.yogida.meditation.repository;

import com.yogida.meditation.entity.MediaCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaCategoryRepository extends JpaRepository<MediaCategoryEntity, Long> {
}
