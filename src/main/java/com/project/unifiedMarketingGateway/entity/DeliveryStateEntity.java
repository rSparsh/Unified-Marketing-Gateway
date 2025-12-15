package com.project.unifiedMarketingGateway.entity;

import com.project.unifiedMarketingGateway.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "delivery_state",
        indexes = {
                @Index(name = "idx_delivery_request", columnList = "requestId"),
                @Index(name = "idx_delivery_channel", columnList = "channel")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requestId;
    private String channel;
    private String recipient;
    private String mediaType;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    private String providerMessageId; // telegram msg id, wamid, twilio sid
    private String failureReason;

    private long createdAtEpochMillis;
    private Long updatedAtEpochMillis;
}

