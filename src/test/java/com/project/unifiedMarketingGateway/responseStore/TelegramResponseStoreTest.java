package com.project.unifiedMarketingGateway.responseStore;

import com.project.unifiedMarketingGateway.store.responseStore.TelegramResponseStore;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;

public class TelegramResponseStoreTest {

    @Test
    public void testStoreResponseWithValidInputs() throws Exception {
        TelegramResponseStore store = new TelegramResponseStore();
        String recipientId = "123";
        String responseBody = "Test response";

        store.storeResponse(recipientId, responseBody);

        Field field = TelegramResponseStore.class.getDeclaredField("responses");
        field.setAccessible(true);
        ConcurrentMap<String, String> responses = (ConcurrentMap<String, String>) field.get(store);

        assertEquals("Test response", responses.get(recipientId));
    }

    @Test
    public void testStoreResponseWithNullRecipientId() throws Exception {
        TelegramResponseStore store = new TelegramResponseStore();
        String recipientId = null;
        String responseBody = "Test response";

        store.storeResponse(recipientId, responseBody);

        Field field = TelegramResponseStore.class.getDeclaredField("responses");
        field.setAccessible(true);
        ConcurrentMap<String, String> responses = (ConcurrentMap<String, String>) field.get(store);

        assertEquals("Test response", responses.get("unknown"));
    }

    @Test
    public void testStoreResponseWithNullResponseBody() throws Exception {
        TelegramResponseStore store = new TelegramResponseStore();
        String recipientId = "456";
        String responseBody = null;

        store.storeResponse(recipientId, responseBody);

        Field field = TelegramResponseStore.class.getDeclaredField("responses");
        field.setAccessible(true);
        ConcurrentMap<String, String> responses = (ConcurrentMap<String, String>) field.get(store);

        assertEquals("", responses.get(recipientId));
    }
}
