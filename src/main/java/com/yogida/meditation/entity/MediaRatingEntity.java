package com.yogida.meditation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Stores a single user rating (1–5) for a media item.
 * One rating per (user, media) pair — enforced by {@code uq_media_rating_user_media}.
 */
@Data
@Entity
@Table(name = "media_rating")
public class MediaRatingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_media_rating_user"))
    private AppUserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false, foreignKey = @ForeignKey(name = "fk_media_rating_media"))
    private MediaEntity media;

    /** Rating value, constrained to 1–5 by a DB check constraint. */
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
