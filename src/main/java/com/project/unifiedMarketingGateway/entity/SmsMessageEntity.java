package com.project.unifiedMarketingGateway.entity;

import com.project.unifiedMarketingGateway.enums.SmsMessageStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sms_message")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sid", unique = true)
    private String sid;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Enumerated(EnumType.STRING)
    private SmsMessageStatus status;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    private long createdAtEpochMillis;
    private Long lastUpdatedEpochMillis;
}

