package com.yogida.meditation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "favourite")
public class FavouriteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favourite_id")
    private Long favouriteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_favourite_user"))
    private AppUserEntity user;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(name = "content_id")
    private Long contentId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

