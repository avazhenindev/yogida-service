package com.yogida.meditation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "profile")
public class ProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_profile_user"))
    private AppUserEntity user;

    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled;

    @Column(name = "notification_preferences")
    private String notificationPreferences;

    @Column(name = "theme_preference", length = 50)
    private String themePreference;

    @Column(name = "language", length = 50)
    private String language;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

