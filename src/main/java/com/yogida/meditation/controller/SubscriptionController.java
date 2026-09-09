package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.SubscriptionControllerApi;
import com.yogida.meditation.dto.SubscriptionDto;
import com.yogida.meditation.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SubscriptionController implements SubscriptionControllerApi {

    private final SubscriptionService subscriptionService;

    /*
     * The reads stay open: the mobile paywall lists plans to decide what to offer.
     *
     * The writes did not. /subscriptions/** sat under a plain .authenticated() rule with no
     * method-level guard anywhere on this class, so any signed-in mobile token could create,
     * edit or delete a shared plan row — the same defect AppUserController documents closing
     * for /users, in a sibling that was missed.
     */

    @Override
    public ResponseEntity<List<SubscriptionDto>> getAll() {
        return ResponseEntity.ok(subscriptionService.findAll());
    }

    @Override
    public ResponseEntity<SubscriptionDto> getByName(String name) {
        return ResponseEntity.ok(subscriptionService.findByName(name));
    }

    @Override
    public ResponseEntity<SubscriptionDto> getById(Long id) {
        return ResponseEntity.ok(subscriptionService.findById(id));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionDto> create(SubscriptionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.create(dto));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionDto> update(Long id, SubscriptionDto dto) {
        return ResponseEntity.ok(subscriptionService.update(id, dto));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(Long id) {
        subscriptionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
