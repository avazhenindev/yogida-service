package com.yogida.meditation.service;

import com.yogida.meditation.dto.ProfileDto;
import com.yogida.meditation.entity.ProfileEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.mapper.ProfileMapper;
import com.yogida.meditation.repository.AppUserRepository;
import com.yogida.meditation.repository.ProfileRepository;
import com.yogida.meditation.service.api.ProfileApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class ProfileService implements ProfileApi {

    private final ProfileRepository profileRepository;
    private final AppUserRepository appUserRepository;
    private final ProfileMapper profileMapper;

    @Override
    public List<ProfileDto> findAll() {
        return profileRepository.findAll().stream().map(profileMapper::toDto).toList();
    }

    @Override
    public ProfileDto findById(Long id) {
        return profileRepository.findById(id)
                .map(profileMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Profile", id));
    }

    @Override
    public ProfileDto create(ProfileDto dto) {
        validateUserExists(dto.getUserId());
        ProfileEntity entity = profileMapper.toEntity(dto);
        entity.setProfileId(null);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        ProfileEntity saved = profileRepository.save(entity);
        log.info("ProfileService > Created profile with id: {}", saved.getProfileId());
        return profileMapper.toDto(saved);
    }

    @Override
    public ProfileDto update(Long id, ProfileDto dto) {
        ProfileEntity existing = profileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Profile", id));
        if (dto.getUserId() != null) {
            validateUserExists(dto.getUserId());
        }
        existing.setTwoFactorEnabled(dto.getTwoFactorEnabled());
        existing.setNotificationPreferences(dto.getNotificationPreferences());
        existing.setThemePreference(dto.getThemePreference());
        existing.setLanguage(dto.getLanguage());
        existing.setUpdatedAt(LocalDateTime.now());
        ProfileEntity saved = profileRepository.save(existing);
        log.info("ProfileService > Updated profile with id: {}", saved.getProfileId());
        return profileMapper.toDto(saved);
    }

    @Override
    public void delete(Long id) {
        if (!profileRepository.existsById(id)) {
            throw new EntityNotFoundException("Profile", id);
        }
        profileRepository.deleteById(id);
        log.info("ProfileService > Deleted profile with id: {}", id);
    }

    private void validateUserExists(Long userId) {
        if (userId == null || !appUserRepository.existsById(userId)) {
            throw new EntityNotFoundException("AppUser", userId);
        }
    }
}

