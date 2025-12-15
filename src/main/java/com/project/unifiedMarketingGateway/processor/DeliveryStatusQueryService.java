package com.project.unifiedMarketingGateway.processor;

import com.project.unifiedMarketingGateway.entity.DeliveryStateEntity;
import com.project.unifiedMarketingGateway.metrics.dto.ChannelDeliveryStatus;
import com.project.unifiedMarketingGateway.metrics.dto.DeliveryStatusResponse;
import com.project.unifiedMarketingGateway.repository.DeliveryStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DeliveryStatusQueryService {

    @Autowired
    DeliveryStateRepository repository;

    public DeliveryStatusResponse getStatus(String requestId) {

        List<DeliveryStateEntity> entities =
                repository.findByRequestId(requestId);

        if (entities.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No delivery state found for requestId=" + requestId
            );
        }

        List<ChannelDeliveryStatus> deliveries = entities.stream()
                .map(e -> ChannelDeliveryStatus.builder()
                        .channel(e.getChannel())
                        .recipient(e.getRecipient())
                        .mediaType(e.getMediaType())
                        .status(e.getStatus())
                        .providerMessageId(e.getProviderMessageId())
                        .failureReason(e.getFailureReason())
                        .updatedAtEpochMillis(e.getUpdatedAtEpochMillis())
                        .build())
                .toList();

        return DeliveryStatusResponse.builder()
                .requestId(requestId)
                .deliveries(deliveries)
                .build();
    }
}

