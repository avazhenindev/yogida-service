package com.yogida.meditation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Stores a single written review per (user, media) pair.
 * Enforced unique by {@code uq_media_review_user_media}.
 */
@Data
@Entity
@Table(name = "media_review")
public class MediaReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_media_review_user"))
    private AppUserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false, foreignKey = @ForeignKey(name = "fk_media_review_media"))
    private MediaEntity media;

    /** Review body, max 2 000 characters. */
    @Column(name = "review_text", nullable = false, length = 2000)
    private String reviewText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
