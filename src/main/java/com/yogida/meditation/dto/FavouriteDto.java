package com.yogida.meditation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavouriteDto {
    private Long favouriteId;
    private Long userId;
    private String contentType;
    private Long contentId;
    private LocalDateTime createdAt;
}

