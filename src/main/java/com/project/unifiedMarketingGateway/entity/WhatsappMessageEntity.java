package com.project.unifiedMarketingGateway.entity;

import com.project.unifiedMarketingGateway.enums.WhatsappMessageStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "whatsapp_message", indexes = {
        @Index(name = "idx_wa_msg_id", columnList = "wa_message_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsappMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wa_message_id", unique = true, length = 128)
    private String waMessageId;

    @Column(name = "recipient", length = 64)
    private String recipient;

    @Column(name = "media_type", length = 32)
    private String mediaType;

    @Column(name = "request_id", length = 128)
    private String requestId;

    @Column(name = "created_at_epoch_ms")
    private Long createdAtEpochMillis;

    @Column(name = "last_updated_epoch_ms")
    private Long lastUpdatedEpochMillis;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private WhatsappMessageStatus status;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;
}
