package com.project.unifiedMarketingGateway.repository;

import com.project.unifiedMarketingGateway.entity.WhatsappMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WhatsappMessageRepository extends JpaRepository<WhatsappMessageEntity, Long> {
    Optional<WhatsappMessageEntity> findByWaMessageId(String waMessageId);
}