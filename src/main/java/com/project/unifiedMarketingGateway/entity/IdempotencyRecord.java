package com.project.unifiedMarketingGateway.entity;

import com.project.unifiedMarketingGateway.enums.IdempotencyStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "idempotency_record",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"requestId", "channel", "recipient", "mediaType"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requestId;
    private String channel;
    private String recipient;
    private String mediaType;

    @Enumerated(EnumType.STRING)
    private IdempotencyStatus status;
    private long createdAtEpochMillis;
    private Long completedAtEpochMillis;
}
