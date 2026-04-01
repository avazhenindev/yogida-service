package com.yogida.meditation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {
    private Long profileId;
    private Long userId;
    private Boolean twoFactorEnabled;
    private String notificationPreferences;
    private String themePreference;
    private String language;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

