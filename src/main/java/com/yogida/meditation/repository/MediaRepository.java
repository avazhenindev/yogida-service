package com.yogida.meditation.repository;

import com.yogida.meditation.entity.MediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<MediaEntity, Long> {
    boolean existsByBucketNameAndName(String bucketName, String name);
}
