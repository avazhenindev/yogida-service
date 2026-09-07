package com.yogida.meditation.repository;

import com.yogida.meditation.entity.UserEntitlementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEntitlementRepository extends JpaRepository<UserEntitlementEntity, String> {
}
