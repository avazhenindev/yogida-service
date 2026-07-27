package com.yogida.meditation.repository;

import com.yogida.meditation.entity.BreathingPhaseAudioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BreathingPhaseAudioRepository extends JpaRepository<BreathingPhaseAudioEntity, Long> {

    Optional<BreathingPhaseAudioEntity> findByPhaseIdAndAudioObjectId(Long phaseId, Long audioObjectId);
}
