package com.yogida.meditation.repository;

import com.yogida.meditation.entity.S3ObjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface S3ObjectRepository extends JpaRepository<S3ObjectEntity, Long> {
}

