package com.project.unifiedMarketingGateway.processor;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class IdempotencyServiceTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Test
    void firstAttempt_isAllowed() {
        boolean allowed = idempotencyService.tryStart(
                "req-1", "telegram", "123", "TEXT");

        assertTrue(allowed);
    }

    @Test
    void duplicateAttempt_isBlocked() {
        idempotencyService.tryStart("req-1", "telegram", "123", "TEXT");

        boolean second = idempotencyService.tryStart(
                "req-1", "telegram", "123", "TEXT");

        assertFalse(second);
    }

    @Test
    void failedAttempt_canBeRetried() {
        idempotencyService.tryStart("req-1", "telegram", "123", "TEXT");
        idempotencyService.markFailed("req-1", "telegram", "123", "TEXT");

        boolean retry = idempotencyService.tryStart(
                "req-1", "telegram", "123", "TEXT");

        assertTrue(retry);
    }
}

