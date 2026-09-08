package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.AdminStorageReconciliationControllerApi;
import com.yogida.meditation.dto.StorageReconciliationReport;
import com.yogida.meditation.service.StorageReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminStorageReconciliationController implements AdminStorageReconciliationControllerApi {

    private final StorageReconciliationService storageReconciliationService;

    @Override
    public ResponseEntity<StorageReconciliationReport> reconcile() {
        return ResponseEntity.ok(storageReconciliationService.reconcile());
    }
}
