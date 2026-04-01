package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.FavouriteControllerApi;
import com.yogida.meditation.dto.FavouriteDto;
import com.yogida.meditation.service.FavouriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FavouriteController implements FavouriteControllerApi {

    private final FavouriteService favouriteService;

    @Override
    public ResponseEntity<List<FavouriteDto>> getAll() {
        return ResponseEntity.ok(favouriteService.findAll());
    }

    @Override
    public ResponseEntity<FavouriteDto> getById(Long id) {
        return ResponseEntity.ok(favouriteService.findById(id));
    }

    @Override
    public ResponseEntity<FavouriteDto> create(FavouriteDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(favouriteService.create(dto));
    }

    @Override
    public ResponseEntity<FavouriteDto> update(Long id, FavouriteDto dto) {
        return ResponseEntity.ok(favouriteService.update(id, dto));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        favouriteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

