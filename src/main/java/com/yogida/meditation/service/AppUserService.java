package com.yogida.meditation.service;

import com.yogida.meditation.dto.AppUserDto;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.mapper.AppUserMapper;
import com.yogida.meditation.repository.AppUserRepository;
import com.yogida.meditation.service.api.AppUserApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class AppUserService implements AppUserApi {

    private final AppUserRepository appUserRepository;
    private final AppUserMapper appUserMapper;

    @Override
    public List<AppUserDto> findAll() {
        return appUserRepository.findAll().stream().map(appUserMapper::toDto).toList();
    }

    @Override
    public AppUserDto findById(Long id) {
        return appUserRepository.findById(id)
                .map(appUserMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("AppUser", id));
    }

    @Override
    public AppUserDto create(AppUserDto dto) {
        AppUserEntity entity = appUserMapper.toEntity(dto);
        entity.setUserId(null);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        AppUserEntity saved = appUserRepository.save(entity);
        log.info("AppUserService > Created user with id: {}", saved.getUserId());
        return appUserMapper.toDto(saved);
    }

    @Override
    public AppUserDto update(Long id, AppUserDto dto) {
        AppUserEntity existing = appUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AppUser", id));
        appUserMapper.updateEntity(dto, existing);
        existing.setUpdatedAt(LocalDateTime.now());
        AppUserEntity saved = appUserRepository.save(existing);
        log.info("AppUserService > Updated user with id: {}", saved.getUserId());
        return appUserMapper.toDto(saved);
    }

    @Override
    public void delete(Long id) {
        if (!appUserRepository.existsById(id)) {
            throw new EntityNotFoundException("AppUser", id);
        }
        appUserRepository.deleteById(id);
        log.info("AppUserService > Deleted user with id: {}", id);
    }
}

