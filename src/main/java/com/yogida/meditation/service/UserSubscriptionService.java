package com.yogida.meditation.service;

import com.yogida.meditation.dto.UserSubscriptionDto;
import com.yogida.meditation.entity.UserSubscriptionEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.mapper.UserSubscriptionMapper;
import com.yogida.meditation.repository.AppUserRepository;
import com.yogida.meditation.repository.SubscriptionRepository;
import com.yogida.meditation.repository.UserSubscriptionRepository;
import com.yogida.meditation.service.api.UserSubscriptionApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserSubscriptionService implements UserSubscriptionApi {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final AppUserRepository appUserRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserSubscriptionMapper userSubscriptionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<UserSubscriptionDto> findAll() {
        return userSubscriptionMapper.toDtoList(userSubscriptionRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public UserSubscriptionDto findById(Long id) {
        return userSubscriptionRepository.findById(id)
                .map(userSubscriptionMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("UserSubscription", id));
    }

    @Override
    @Transactional
    public UserSubscriptionDto create(UserSubscriptionDto dto) {
        validateUserExists(dto.getUserId());
        validateSubscriptionExists(dto.getSubscriptionId());
        UserSubscriptionEntity entity = userSubscriptionMapper.toEntity(dto);
        entity.setUserSubscriptionId(null);
        UserSubscriptionEntity saved = userSubscriptionRepository.save(entity);
        log.info("UserSubscriptionService > Created user subscription with id: {}", saved.getUserSubscriptionId());
        return userSubscriptionMapper.toDto(saved);
    }

    @Override
    @Transactional
    public UserSubscriptionDto update(Long id, UserSubscriptionDto dto) {
        UserSubscriptionEntity existing = userSubscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("UserSubscription", id));
        if (dto.getUserId() != null) {
            validateUserExists(dto.getUserId());
            existing.setUser(userSubscriptionMapper.toEntity(dto).getUser());
        }
        if (dto.getSubscriptionId() != null) {
            validateSubscriptionExists(dto.getSubscriptionId());
            existing.setSubscription(userSubscriptionMapper.toEntity(dto).getSubscription());
        }
        existing.setPlanType(dto.getPlanType());
        existing.setStatus(dto.getStatus());
        existing.setStartDate(dto.getStartDate());
        existing.setEndDate(dto.getEndDate());
        existing.setAutoRenew(dto.getAutoRenew());
        UserSubscriptionEntity saved = userSubscriptionRepository.save(existing);
        log.info("UserSubscriptionService > Updated user subscription with id: {}", saved.getUserSubscriptionId());
        return userSubscriptionMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!userSubscriptionRepository.existsById(id)) {
            throw new EntityNotFoundException("UserSubscription", id);
        }
        userSubscriptionRepository.deleteById(id);
        log.info("UserSubscriptionService > Deleted user subscription with id: {}", id);
    }

    private void validateUserExists(Long userId) {
        if (userId == null || !appUserRepository.existsById(userId)) {
            throw new EntityNotFoundException("AppUser", userId);
        }
    }

    private void validateSubscriptionExists(Long subscriptionId) {
        if (subscriptionId == null || !subscriptionRepository.existsById(subscriptionId)) {
            throw new EntityNotFoundException("Subscription", subscriptionId);
        }
    }
}

