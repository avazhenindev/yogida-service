package com.yogida.meditation.repository;

import com.yogida.meditation.entity.FavouriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavouriteRepository extends JpaRepository<FavouriteEntity, Long> {
}

