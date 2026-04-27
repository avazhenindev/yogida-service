package com.yogida.meditation.dto;

import com.yogida.meditation.enums.MediaLogAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaLogDto {
    private Long id;
    private Long mediaId;
    private String mediaName;
    private MediaLogAction action;
    private String message;
    private LocalDateTime createdAt;
}

