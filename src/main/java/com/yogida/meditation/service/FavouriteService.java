package com.yogida.meditation.service;

import com.yogida.meditation.dto.FavouriteDto;
import com.yogida.meditation.entity.FavouriteEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.mapper.FavouriteMapper;
import com.yogida.meditation.repository.AppUserRepository;
import com.yogida.meditation.repository.FavouriteRepository;
import com.yogida.meditation.service.api.FavouriteApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class FavouriteService implements FavouriteApi {

    private final FavouriteRepository favouriteRepository;
    private final AppUserRepository appUserRepository;
    private final FavouriteMapper favouriteMapper;

    @Override
    public List<FavouriteDto> findAll() {
        return favouriteRepository.findAll().stream().map(favouriteMapper::toDto).toList();
    }

    @Override
    public FavouriteDto findById(Long id) {
        return favouriteRepository.findById(id)
                .map(favouriteMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Favourite", id));
    }

    @Override
    public FavouriteDto create(FavouriteDto dto) {
        validateUserExists(dto.getUserId());
        FavouriteEntity entity = favouriteMapper.toEntity(dto);
        entity.setFavouriteId(null);
        entity.setCreatedAt(LocalDateTime.now());
        FavouriteEntity saved = favouriteRepository.save(entity);
        log.info("FavouriteService > Created favourite with id: {}", saved.getFavouriteId());
        return favouriteMapper.toDto(saved);
    }

    @Override
    public FavouriteDto update(Long id, FavouriteDto dto) {
        FavouriteEntity existing = favouriteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Favourite", id));
        if (dto.getUserId() != null) {
            validateUserExists(dto.getUserId());
            existing.setUser(favouriteMapper.toEntity(dto).getUser());
        }
        existing.setContentType(dto.getContentType());
        existing.setContentId(dto.getContentId());
        FavouriteEntity saved = favouriteRepository.save(existing);
        log.info("FavouriteService > Updated favourite with id: {}", saved.getFavouriteId());
        return favouriteMapper.toDto(saved);
    }

    @Override
    public void delete(Long id) {
        if (!favouriteRepository.existsById(id)) {
            throw new EntityNotFoundException("Favourite", id);
        }
        favouriteRepository.deleteById(id);
        log.info("FavouriteService > Deleted favourite with id: {}", id);
    }

    private void validateUserExists(Long userId) {
        if (userId == null || !appUserRepository.existsById(userId)) {
            throw new EntityNotFoundException("AppUser", userId);
        }
    }
}

