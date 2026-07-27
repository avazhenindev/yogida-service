package com.yogida.meditation.repository;

import com.yogida.meditation.entity.BreathingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BreathingRepository extends JpaRepository<BreathingEntity, Long> {

    List<BreathingEntity> findAllByOrderByDisplayOrderAsc();
}
