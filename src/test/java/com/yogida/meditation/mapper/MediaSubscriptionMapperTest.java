package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.MediaSubscriptionDto;
import com.yogida.meditation.dto.SubscriptionDto;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.MediaSubscriptionEntity;
import com.yogida.meditation.entity.SubscriptionEntity;
import com.yogida.meditation.enums.Currency;
import com.yogida.meditation.enums.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MediaSubscriptionMapper to verify nested subscription mapping.
 * Uses MapStruct instantiation via Spring Boot test context.
 */
@SpringBootTest
class MediaSubscriptionMapperTest {

    @Autowired
    private MediaSubscriptionMapper mediaSubscriptionMapper;

    private MediaSubscriptionEntity entity;
    private SubscriptionEntity subscription;
    private MediaEntity media;

    @BeforeEach
    void setUp() {
        subscription = new SubscriptionEntity();
        subscription.setSubscriptionId(1L);
        subscription.setName("Premium Plan");
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPeriodDays(30);
        subscription.setDetails("Premium subscription benefits");
        subscription.setPrice(new BigDecimal("9.99"));
        subscription.setCurrency(Currency.USD);

        media = new MediaEntity();
        media.setId(1L);

        entity = new MediaSubscriptionEntity();
        entity.setMediaSubscriptionId(100L);
        entity.setMedia(media);
        entity.setSubscription(subscription);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0, 0));
    }

    @Test
    void toDto_withNestedSubscription_shouldMapSubscriptionObject() {
        // When
        MediaSubscriptionDto dto = mediaSubscriptionMapper.toDto(entity);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getMediaSubscriptionId()).isEqualTo(100L);
        assertThat(dto.getMediaId()).isEqualTo(1L);
        assertThat(dto.getSubscription()).isNotNull();
        assertThat(dto.getSubscription().getSubscriptionId()).isEqualTo(1L);
        assertThat(dto.getSubscription().getName()).isEqualTo("Premium Plan");
        assertThat(dto.getSubscription().getPrice()).isEqualTo(new BigDecimal("9.99"));
        assertThat(dto.getSubscription().getCurrency()).isEqualTo(Currency.USD);
    }

    @Test
    void toDto_withMultipleSubscriptions_shouldMapAllSubscriptions() {
        // Given
        SubscriptionEntity sub2 = new SubscriptionEntity();
        sub2.setSubscriptionId(2L);
        sub2.setName("Standard Plan");
        sub2.setStatus(SubscriptionStatus.ACTIVE);
        sub2.setPrice(new BigDecimal("4.99"));
        sub2.setCurrency(Currency.USD);
        sub2.setPeriodDays(30);

        MediaSubscriptionEntity entity2 = new MediaSubscriptionEntity();
        entity2.setMediaSubscriptionId(101L);
        entity2.setMedia(media);
        entity2.setSubscription(sub2);

        List<MediaSubscriptionEntity> entities = Arrays.asList(entity, entity2);

        // When
        List<MediaSubscriptionDto> dtos = mediaSubscriptionMapper.toDtoList(entities);

        // Then
        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getSubscription().getSubscriptionId()).isEqualTo(1L);
        assertThat(dtos.get(0).getSubscription().getName()).isEqualTo("Premium Plan");
        assertThat(dtos.get(1).getSubscription().getSubscriptionId()).isEqualTo(2L);
        assertThat(dtos.get(1).getSubscription().getName()).isEqualTo("Standard Plan");
    }

    @Test
    void toEntity_withNestedSubscriptionId_shouldMapSubscriptionIdToSubscriptionEntity() {
        // Given
        SubscriptionDto subscriptionDto = new SubscriptionDto();
        subscriptionDto.setSubscriptionId(2L);
        subscriptionDto.setName("Mapped Plan");

        MediaSubscriptionDto dto = new MediaSubscriptionDto();
        dto.setMediaId(1L);
        dto.setSubscription(subscriptionDto);

        // When
        MediaSubscriptionEntity mappedEntity = mediaSubscriptionMapper.toEntity(dto);

        // Then
        assertThat(mappedEntity).isNotNull();
        assertThat(mappedEntity.getMedia()).isNotNull();
        assertThat(mappedEntity.getMedia().getId()).isEqualTo(1L);
        assertThat(mappedEntity.getSubscription()).isNotNull();
        assertThat(mappedEntity.getSubscription().getSubscriptionId()).isEqualTo(2L);
    }

    @Test
    void toEntity_withNullSubscription_shouldHandleNull() {
        // Given
        MediaSubscriptionDto dto = new MediaSubscriptionDto();
        dto.setMediaId(1L);
        dto.setSubscription(null);

        // When
        MediaSubscriptionEntity mappedEntity = mediaSubscriptionMapper.toEntity(dto);

        // Then
        assertThat(mappedEntity).isNotNull();
        assertThat(mappedEntity.getSubscription()).isNull();
    }

    @Test
    void updateEntity_withNestedSubscription_shouldUpdateSubscriptionReference() {
        // Given
        SubscriptionDto newSubscriptionDto = new SubscriptionDto();
        newSubscriptionDto.setSubscriptionId(3L);
        newSubscriptionDto.setName("New Plan");

        MediaSubscriptionDto updateDto = new MediaSubscriptionDto();
        updateDto.setSubscription(newSubscriptionDto);

        LocalDateTime originalCreatedAt = entity.getCreatedAt();

        // When
        mediaSubscriptionMapper.updateEntity(updateDto, entity);

        // Then
        assertThat(entity.getSubscription()).isNotNull();
        assertThat(entity.getSubscription().getSubscriptionId()).isEqualTo(3L);
        assertThat(entity.getCreatedAt()).isEqualTo(originalCreatedAt); // Ensure createdAt is not overwritten
    }

    @Test
    void toDto_subscriptionPresence_indicatesMediaHasAssignedSubscription() {
        // This test verifies the key behavior change: subscription is now a full object
        MediaSubscriptionDto dto = mediaSubscriptionMapper.toDto(entity);

        // Before fix: only subscriptionId field was available
        // After fix: full subscription object including name, price, status, etc
        assertThat(dto.getSubscription()).isNotNull();
        assertThat(dto.getSubscription().getName()).isNotBlank();
        assertThat(dto.getSubscription().getPrice()).isNotNull();
        assertThat(dto.getSubscription().getCurrency()).isNotNull();
    }
}


