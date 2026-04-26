package com.yogida.meditation.service;

import com.yogida.meditation.dto.AppUserDto;
import com.yogida.meditation.dto.UserSubscriptionDto;
import com.yogida.meditation.entity.SubscriptionEntity;
import com.yogida.meditation.enums.SubscriptionStatus;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.repository.SubscriptionRepository;
import com.yogida.meditation.service.api.AppUserApi;
import com.yogida.meditation.service.api.UserFacadeApi;
import com.yogida.meditation.service.api.UserSubscriptionApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserFacadeService implements UserFacadeApi {

    private static final String FREE_SUBSCRIPTION_NAME = "FREE";

    private final AppUserApi appUserApi;
    private final UserSubscriptionApi userSubscriptionApi;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional
    public AppUserDto onboardUser(AppUserDto dto) {
        AppUserDto createdUser = appUserApi.create(dto);
        log.info("UserFacadeService > User created with id: {}", createdUser.getUserId());

        SubscriptionEntity freeSubscription = subscriptionRepository.findByName(FREE_SUBSCRIPTION_NAME)
                .orElseThrow(() -> new EntityNotFoundException("Subscription with name " + FREE_SUBSCRIPTION_NAME + " not found"));

        UserSubscriptionDto subscriptionDto = new UserSubscriptionDto();
        subscriptionDto.setUserId(createdUser.getUserId());
        subscriptionDto.setSubscriptionId(freeSubscription.getSubscriptionId());
        subscriptionDto.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionDto.setStartDate(LocalDate.now());
        subscriptionDto.setAutoRenew(false);

        userSubscriptionApi.create(subscriptionDto);
        log.info("UserFacadeService > FREE subscription assigned to user id: {}", createdUser.getUserId());

        return createdUser;
    }
}

