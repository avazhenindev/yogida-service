package com.yogida.meditation.service;

import com.yogida.meditation.dto.SubscriptionDto;
import com.yogida.meditation.entity.SubscriptionEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.mapper.SubscriptionMapper;
import com.yogida.meditation.repository.AppUserRepository;
import com.yogida.meditation.repository.SubscriptionRepository;
import com.yogida.meditation.service.api.SubscriptionApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class SubscriptionService implements SubscriptionApi {

    private final SubscriptionRepository subscriptionRepository;
    private final AppUserRepository appUserRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Override
    public List<SubscriptionDto> findAll() {
        return subscriptionRepository.findAll().stream().map(subscriptionMapper::toDto).toList();
    }

    @Override
    public SubscriptionDto findById(Long id) {
        return subscriptionRepository.findById(id)
                .map(subscriptionMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Subscription", id));
    }

    @Override
    public SubscriptionDto create(SubscriptionDto dto) {
        validateUserExists(dto.getUserId());
        SubscriptionEntity entity = subscriptionMapper.toEntity(dto);
        entity.setSubscriptionId(null);
        SubscriptionEntity saved = subscriptionRepository.save(entity);
        log.info("SubscriptionService > Created subscription with id: {}", saved.getSubscriptionId());
        return subscriptionMapper.toDto(saved);
    }

    @Override
    public SubscriptionDto update(Long id, SubscriptionDto dto) {
        SubscriptionEntity existing = subscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subscription", id));
        if (dto.getUserId() != null) {
            validateUserExists(dto.getUserId());
            existing.setUser(subscriptionMapper.toEntity(dto).getUser());
        }
        existing.setPlanType(dto.getPlanType());
        existing.setStatus(dto.getStatus());
        existing.setStartDate(dto.getStartDate());
        existing.setEndDate(dto.getEndDate());
        existing.setAutoRenew(dto.getAutoRenew());
        SubscriptionEntity saved = subscriptionRepository.save(existing);
        log.info("SubscriptionService > Updated subscription with id: {}", saved.getSubscriptionId());
        return subscriptionMapper.toDto(saved);
    }

    @Override
    public void delete(Long id) {
        if (!subscriptionRepository.existsById(id)) {
            throw new EntityNotFoundException("Subscription", id);
        }
        subscriptionRepository.deleteById(id);
        log.info("SubscriptionService > Deleted subscription with id: {}", id);
    }

    private void validateUserExists(Long userId) {
        if (userId == null || !appUserRepository.existsById(userId)) {
            throw new EntityNotFoundException("AppUser", userId);
        }
    }
}

