package com.yogida.meditation.repository;

import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.enums.MediaStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaRepository extends JpaRepository<MediaEntity, Long> {

    @EntityGraph(attributePaths = "mediaObject")
    List<MediaEntity> findAllWithMediaObjectBy();


    List<MediaEntity> findAllByStatus(MediaStatus status);
}
