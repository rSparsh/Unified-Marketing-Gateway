package com.project.unifiedMarketingGateway.controllers;

import com.project.unifiedMarketingGateway.models.WhatsappMessage;
import com.project.unifiedMarketingGateway.store.messageStore.WhatsappMessageStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whatsapp")
@RequiredArgsConstructor
public class WhatsappMessageController {

    private final WhatsappMessageStore messageStore;

    @GetMapping("/status/{waMessageId}")
    public ResponseEntity<WhatsappMessage> getStatus(@PathVariable String waMessageId) {
        return messageStore.findByWaMessageId(waMessageId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}