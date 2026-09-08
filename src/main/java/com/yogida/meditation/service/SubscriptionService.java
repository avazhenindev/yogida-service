package com.yogida.meditation.service;

import com.yogida.meditation.dto.SubscriptionDto;
import com.yogida.meditation.entity.SubscriptionEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.repository.Repositories;
import com.yogida.meditation.mapper.SubscriptionMapper;
import com.yogida.meditation.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Transactional(readOnly = true)
    public List<SubscriptionDto> findAll() {
        return subscriptionMapper.toDtoList(subscriptionRepository.findAll());
    }

    @Transactional(readOnly = true)
    public SubscriptionDto findByName(String name) {
        return subscriptionRepository.findByName(name)
                .map(subscriptionMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Subscription with name '" + name + "' not found"));
    }

    @Transactional(readOnly = true)
    public SubscriptionDto findById(Long id) {
        return subscriptionRepository.findById(id)
                .map(subscriptionMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Subscription", id));
    }

    @Transactional
    public SubscriptionDto create(SubscriptionDto dto) {
        SubscriptionEntity entity = subscriptionMapper.toEntity(dto);
        entity.setSubscriptionId(null);
        SubscriptionEntity saved = subscriptionRepository.save(entity);
        log.info("SubscriptionService > Created subscription plan with id: {}", saved.getSubscriptionId());
        return subscriptionMapper.toDto(saved);
    }

    @Transactional
    public SubscriptionDto update(Long id, SubscriptionDto dto) {
        SubscriptionEntity existing = subscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subscription", id));
        subscriptionMapper.updateEntity(dto, existing);
        SubscriptionEntity saved = subscriptionRepository.save(existing);
        log.info("SubscriptionService > Updated subscription plan with id: {}", saved.getSubscriptionId());
        return subscriptionMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Repositories.deleteOrThrow(subscriptionRepository, id, "Subscription");
        log.info("SubscriptionService > Deleted subscription plan with id: {}", id);
    }
}
