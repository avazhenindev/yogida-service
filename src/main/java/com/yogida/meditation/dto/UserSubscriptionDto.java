package com.yogida.meditation.dto;

import com.yogida.meditation.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscriptionDto {
    private Long userSubscriptionId;
    private Long userId;
    private Long subscriptionId;
    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean autoRenew;
    private SubscriptionDto subscription;
}

