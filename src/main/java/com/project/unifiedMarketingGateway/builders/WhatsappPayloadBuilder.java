package com.project.unifiedMarketingGateway.builders;

import org.springframework.stereotype.Service;

import java.util.Map;

import static com.project.unifiedMarketingGateway.enums.MediaType.*;

@Service
public class WhatsappPayloadBuilder {

    private final static String WHATSAPP = "whatsapp";
    private final static String INDIVIDUAL = "individual";

    public Map<String, Object> buildTextPayload(String recipient, String textMessage) {
        return Map.of(
                "messaging_product", WHATSAPP,
                "recipient_type", INDIVIDUAL,
                "to", recipient,
                "type", String.valueOf(TEXT).toLowerCase(),
                "text", Map.of(
                        "body", textMessage
                )
        );
    }

    public Map<String, Object> buildImagePayload(String recipient, String imageUrl, String caption) {
        return Map.of(
                "messaging_product", WHATSAPP,
                "recipient_type", INDIVIDUAL,
                "to", recipient,
                "type", String.valueOf(IMAGE).toLowerCase(),
                "image", Map.of(
                        "link", imageUrl,
                        "caption", caption
                )
        );
    }

    public Map<String, Object> buildVideoPayload(String recipient, String videoUrl, String caption) {
        return Map.of(
                "messaging_product", WHATSAPP,
                "recipient_type", INDIVIDUAL,
                "to", recipient,
                "type", String.valueOf(VIDEO).toLowerCase(),
                "video", Map.of(
                        "link", videoUrl,
                        "caption", caption
                )
        );
    }
}
