package com.yogida.meditation.service;

import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.enums.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Service for checking media entitlement.
 * Determines if a user has a valid subscription that grants access to a premium media item.
 */
@Service
@RequiredArgsConstructor
public class EntitlementService {

    /**
     * Checks if the media is premium (has associated subscriptions).
     *
     * @param media the media entity to check
     * @return true if the media has subscriptions (is premium), false otherwise
     */
    public boolean isPremium(MediaEntity media) {
        return media.getMediaSubscriptions() != null && !media.getMediaSubscriptions().isEmpty();
    }

    /**
     * Checks if a user has entitlement (valid subscription) to access premium media.
     * Returns true if:
     * - The media is not premium (no subscriptions), OR
     * - The user has at least one ACTIVE subscription that includes the media
     *
     * @param user the app user entity
     * @param media the media entity
     * @return true if the user is entitled to access the media, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isEntitled(AppUserEntity user, MediaEntity media) {
        // If media is not premium, everyone has access
        if (!isPremium(media)) {
            return true;
        }

        // User must have an ACTIVE subscription that grants access to this media
        if (user.getSubscriptions() == null || user.getSubscriptions().isEmpty()) {
            return false;
        }

        LocalDate today = LocalDate.now();

        // Check if user has any ACTIVE subscription that covers this media
        return user.getSubscriptions().stream()
            .filter(userSub -> userSub.getStatus() == SubscriptionStatus.ACTIVE)
            .filter(userSub -> isSubscriptionValid(userSub.getStartDate(), userSub.getEndDate(), today))
            .anyMatch(userSub -> media.getMediaSubscriptions().stream()
                .anyMatch(mediaSub -> mediaSub.getSubscription().getSubscriptionId()
                    .equals(userSub.getSubscription().getSubscriptionId()))
            );
    }

    /**
     * Checks if a subscription is valid on a given date.
     * A subscription is valid if today is between start_date (inclusive) and end_date (exclusive, or null = no end date).
     *
     * @param startDate the subscription start date
     * @param endDate the subscription end date (null = no end date)
     * @param today the date to check
     * @return true if the subscription is valid on the given date
     */
    private boolean isSubscriptionValid(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (startDate != null && today.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && !today.isBefore(endDate)) {
            return false;
        }
        return true;
    }
}
