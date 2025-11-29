package com.project.unifiedMarketingGateway.builders;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class TelegramPayloadBuilderTest {

    private final TelegramPayloadBuilder payloadBuilder = new TelegramPayloadBuilder();

    @Test
    void testBuildTextPayload() {
        String chatId = "12345";
        String textMessage = "Hello, Telegram!";
        Map<String, Object> payload = payloadBuilder.buildTextPayload(chatId, textMessage);

        assertEquals(2, payload.size());
        assertEquals(chatId, payload.get("chat_id"));
        assertEquals(textMessage, payload.get("text"));
        assertNull(payload.get("caption"));
    }

    @Test
    void testBuildImagePayload() {
        String chatId = "12345";
        String imageUrl = "http://example.com/image.jpg";
        String caption = "Image caption";
        Map<String, Object> payload = payloadBuilder.buildImagePayload(chatId, imageUrl, caption);

        assertEquals(3, payload.size());
        assertEquals(chatId, payload.get("chat_id"));
        assertEquals(imageUrl, payload.get("photo"));
        assertEquals(caption, payload.get("caption"));
    }

    @Test
    void testBuildVideoPayload() {
        String chatId = "12345";
        String videoUrl = "http://example.com/video.mp4";
        String caption = "Video caption";
        Map<String, Object> payload = payloadBuilder.buildVideoPayload(chatId, videoUrl, caption);

        assertEquals(3, payload.size());
        assertEquals(chatId, payload.get("chat_id"));
        assertEquals(videoUrl, payload.get("video"));
        assertEquals(caption, payload.get("caption"));
    }
}

