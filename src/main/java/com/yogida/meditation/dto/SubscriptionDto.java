package com.yogida.meditation.dto;

import com.yogida.meditation.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto {
    private Long subscriptionId;
    private String name;
    private String status;
    private Integer periodDays;
    private String details;
    private BigDecimal price;
    private Currency currency;
}
