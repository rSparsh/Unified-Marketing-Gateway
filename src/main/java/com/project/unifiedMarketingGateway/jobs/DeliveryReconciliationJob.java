package com.project.unifiedMarketingGateway.jobs;

import com.project.unifiedMarketingGateway.entity.DeliveryStateEntity;
import com.project.unifiedMarketingGateway.enums.DeliveryStatus;
import com.project.unifiedMarketingGateway.repository.DeliveryStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class DeliveryReconciliationJob {

    @Autowired
    DeliveryStateRepository repository;

    @Scheduled(fixedDelay = 300000) // every 5 mins
    public void reconcile() {

        long cutoff = System.currentTimeMillis() - Duration.ofMinutes(10).toMillis();

        List<DeliveryStateEntity> stuck =
                repository.findByStatus(DeliveryStatus.SENT)
                        .stream()
                        .filter(e -> e.getUpdatedAtEpochMillis() < cutoff)
                        .toList();

        for (DeliveryStateEntity e : stuck) {
            log.warn("Reconciliation needed: requestId={} channel={} recipient={} status={}",
                    e.getRequestId(), e.getChannel(), e.getRecipient(), e.getStatus());
        }
    }
}

