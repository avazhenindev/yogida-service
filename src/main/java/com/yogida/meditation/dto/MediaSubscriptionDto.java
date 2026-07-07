package com.yogida.meditation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaSubscriptionDto {
    private Long mediaSubscriptionId;
    private Long mediaId;
    private SubscriptionDto subscription;
    private LocalDateTime createdAt;
}

