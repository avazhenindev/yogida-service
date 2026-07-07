package com.yogida.meditation.scheduler;

import com.yogida.meditation.service.RevenueCatReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically reconciles stale active auto-renewing subscriptions against RevenueCat.
 */
@Component
@RequiredArgsConstructor
public class RevenueCatReconciliationScheduler {

    private final RevenueCatReconciliationService reconciliationService;

    @Scheduled(fixedDelayString = "${app.revenuecat.reconciliation-delay-ms:3600000}")
    public void reconcile() {
        reconciliationService.reconcileStaleSubscriptions();
    }
}
