package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.UserSubscriptionControllerApi;
import com.yogida.meditation.dto.UserSubscriptionDto;
import com.yogida.meditation.service.UserSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserSubscriptionController implements UserSubscriptionControllerApi {

    private final UserSubscriptionService userSubscriptionService;

    @Override
    public ResponseEntity<List<UserSubscriptionDto>> getAll() {
        return ResponseEntity.ok(userSubscriptionService.findAll());
    }

    @Override
    public ResponseEntity<UserSubscriptionDto> getById(Long id) {
        return ResponseEntity.ok(userSubscriptionService.findById(id));
    }

    @Override
    public ResponseEntity<UserSubscriptionDto> create(UserSubscriptionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userSubscriptionService.create(dto));
    }

    @Override
    public ResponseEntity<UserSubscriptionDto> update(Long id, UserSubscriptionDto dto) {
        return ResponseEntity.ok(userSubscriptionService.update(id, dto));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        userSubscriptionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

