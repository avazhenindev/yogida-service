package com.yogida.meditation.repository;

import com.yogida.meditation.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {

    /**
     * A profile is a 1:1 for an app user, so this returns at most one — but the database has
     * no unique constraint on {@code profile.user_id} yet, so a list is the honest signature
     * until one is added.
     */
    List<ProfileEntity> findByUserUserId(Long userId);

    Optional<ProfileEntity> findFirstByUserUserIdOrderByProfileIdAsc(Long userId);
}
