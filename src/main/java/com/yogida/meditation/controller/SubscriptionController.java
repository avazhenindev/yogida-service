package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.SubscriptionControllerApi;
import com.yogida.meditation.dto.SubscriptionDto;
import com.yogida.meditation.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SubscriptionController implements SubscriptionControllerApi {

    private final SubscriptionService subscriptionService;

    @Override
    public ResponseEntity<List<SubscriptionDto>> getAll() {
        return ResponseEntity.ok(subscriptionService.findAll());
    }

    @Override
    public ResponseEntity<SubscriptionDto> getById(Long id) {
        return ResponseEntity.ok(subscriptionService.findById(id));
    }

    @Override
    public ResponseEntity<SubscriptionDto> create(SubscriptionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.create(dto));
    }

    @Override
    public ResponseEntity<SubscriptionDto> update(Long id, SubscriptionDto dto) {
        return ResponseEntity.ok(subscriptionService.update(id, dto));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        subscriptionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
