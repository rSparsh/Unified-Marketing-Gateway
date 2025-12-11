package com.project.unifiedMarketingGateway.controllers;

import com.project.unifiedMarketingGateway.webhook.WhatsappWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhook/whatsapp")
@Slf4j
public class WhatsappWebhookController {

    @Value("${whatsapp.webhook.verify-token:PLEASE_ADD_SECRETS}")
    private String verifyToken;

    @Autowired WhatsappWebhookService webhookService;

    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody Map<String, Object> payload) {
        try {
            webhookService.processWebhookPayload(payload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing WhatsApp webhook: {}", e.toString(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}