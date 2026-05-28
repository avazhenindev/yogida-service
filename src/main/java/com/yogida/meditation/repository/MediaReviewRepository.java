package com.yogida.meditation.repository;

import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.MediaReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MediaReviewRepository extends JpaRepository<MediaReviewEntity, Long> {

    Optional<MediaReviewEntity> findByUserAndMedia(AppUserEntity user, MediaEntity media);

    Page<MediaReviewEntity> findAllByMediaOrderByCreatedAtDesc(MediaEntity media, Pageable pageable);
}
