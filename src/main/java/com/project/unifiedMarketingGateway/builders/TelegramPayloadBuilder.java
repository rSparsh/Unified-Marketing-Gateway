package com.project.unifiedMarketingGateway.builders;

import com.project.unifiedMarketingGateway.enums.MediaType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.project.unifiedMarketingGateway.constants.Constants.*;
import static com.project.unifiedMarketingGateway.constants.Constants.CAPTION;
import static com.project.unifiedMarketingGateway.enums.MediaType.TEXT;

@Service
public class TelegramPayloadBuilder {

    public Map<String, Object> buildTextPayload(String chatId, String textMessage)
    {
        return buildPayloadObject(chatId, String.valueOf(TEXT).toLowerCase(), textMessage, null);
    }

    public Map<String, Object> buildImagePayload(String chatId, String imageUrl, String caption)
    {
        return buildPayloadObject(chatId, PHOTO, imageUrl, caption);
    }

    public Map<String, Object> buildVideoPayload(String chatId, String imageUrl, String caption)
    {
        return buildPayloadObject(chatId, VIDEO, imageUrl, caption);
    }

    private Map<String, Object> buildPayloadObject(String chatId, String mediaType, String mediaValue, String caption)
    {
        Map<String, Object> payload = new HashMap<>();
        payload.put(CHAT_ID, chatId);
        payload.put(mediaType, mediaValue);
        if(mediaType.equals(PHOTO) || mediaType.equals(VIDEO))
            payload.put(CAPTION, caption);

        return payload;
    }
}
