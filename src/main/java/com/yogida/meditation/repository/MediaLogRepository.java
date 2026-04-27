package com.yogida.meditation.repository;

import com.yogida.meditation.entity.MediaLogEntity;
import com.yogida.meditation.enums.MediaLogAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaLogRepository extends JpaRepository<MediaLogEntity, Long> {

    List<MediaLogEntity> findAllByMediaIdOrderByCreatedAtDesc(Long mediaId);

    List<MediaLogEntity> findAllByAction(MediaLogAction action);
}

