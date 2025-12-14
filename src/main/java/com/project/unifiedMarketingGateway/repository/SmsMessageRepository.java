package com.project.unifiedMarketingGateway.repository;

import com.project.unifiedMarketingGateway.entity.SmsMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmsMessageRepository extends JpaRepository<SmsMessageEntity, Long> {

    Optional<SmsMessageEntity> findBySid(String sid);
}

