package com.yogida.meditation.service;

import com.yogida.meditation.dto.AppUserDto;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.mapper.AppUserMapper;
import com.yogida.meditation.repository.AppUserRepository;
import com.yogida.meditation.repository.UserEntitlementRepository;
import com.yogida.meditation.service.api.AppUserApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class AppUserService implements AppUserApi {

    private final AppUserRepository appUserRepository;
    private final AppUserMapper appUserMapper;
    private final UserEntitlementRepository userEntitlementRepository;
    private final EntitlementService entitlementService;

    @Override
    @Transactional(readOnly = true)
    public List<AppUserDto> findAll() {
        return appUserRepository.findAll().stream().map(appUserMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
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
    /**
     * Deletes a user and everything keyed to them.
     *
     * <p>Profile, favourites and reviews go with the row by {@code ON DELETE CASCADE}. The
     * entitlement projection does not: {@code user_entitlement} is keyed by the Keycloak subject
     * rather than by a foreign key to {@code app_user}, so nothing in the schema would remove it
     * and the row would outlive the account — and then be found by the next account issued that
     * same subject, which is exactly the kind of stale-entitlement bug the projection exists to
     * prevent.
     */
    public void delete(Long id) {
        AppUserEntity user = appUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AppUser", id));

        String keycloakUserId = user.getKeycloakUserId();
        appUserRepository.delete(user);
        if (keycloakUserId != null) {
            userEntitlementRepository.deleteById(keycloakUserId);
            entitlementService.evictUserEntitlement(keycloakUserId);
        }
        log.info("AppUserService > Deleted user with id: {} and its entitlement projection", id);
    }

    @Override
    @Transactional(readOnly = true)
    public AppUserDto findByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .map(appUserMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("AppUser with email " + email + " not found"));
    }
}

