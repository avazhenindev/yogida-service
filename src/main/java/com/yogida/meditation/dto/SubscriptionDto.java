package com.yogida.meditation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto {
    private Long subscriptionId;
    private Long userId;
    private String planType;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean autoRenew;
}

