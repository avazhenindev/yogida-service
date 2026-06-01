package com.yogida.meditation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Stores a user's rating (1–5) and/or written review per (user, media) pair.
 * Both fields are optional; at least one must be set at the application level.
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

    /** Rating value 1–5. Null when the user wrote a review without submitting a star rating. */
    @Column(name = "rating")
    private Integer rating;

    /** Review body, max 2 000 characters. Null when the user only submitted a star rating. */
    @Column(name = "review_text", length = 2000)
    private String reviewText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
