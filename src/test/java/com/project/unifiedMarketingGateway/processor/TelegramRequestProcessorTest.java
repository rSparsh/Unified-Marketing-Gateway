package com.project.unifiedMarketingGateway.processor.telegram;

import com.project.unifiedMarketingGateway.connectors.TelegramHttpConnector;
import com.project.unifiedMarketingGateway.enums.MediaType;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import com.project.unifiedMarketingGateway.processor.RequestProcessorInterface;
import com.project.unifiedMarketingGateway.responseBuilders.SendNotificationResponseBuilder;
import com.project.unifiedMarketingGateway.responseStore.TelegramResponseStore;
import com.project.unifiedMarketingGateway.validators.TelegramSendNotificationRequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TelegramRequestProcessorTest {

    @InjectMocks
    private TelegramRequestProcessor telegramRequestProcessor;

    @Mock
    private TelegramHttpConnector telegramHttpConnector;

    @Mock
    private TelegramResponseStore telegramResponseStore;

    @Mock
    private SendNotificationResponseBuilder sendNotificationResponseBuilder;

    @Mock
    private TelegramSendNotificationRequestValidator telegramSendNotificationRequestValidator;


    @BeforeEach
    void setUp() throws Exception {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Set up the processor with mocks using reflection
        telegramRequestProcessor = new TelegramRequestProcessor();
        setField(telegramRequestProcessor, "telegramHttpConnector", telegramHttpConnector);
        setField(telegramRequestProcessor, "telegramResponseStore", telegramResponseStore);
        setField(telegramRequestProcessor, "sendNotificationResponseBuilder", sendNotificationResponseBuilder);
        setField(telegramRequestProcessor, "telegramSendNotificationRequestValidator", telegramSendNotificationRequestValidator);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void testValidationFailure() {
        // Arrange
        SendNotificationRequest request = SendNotificationRequest.builder().build();
        List<String> validationErrors = Arrays.asList("Invalid chat ID");

        when(telegramSendNotificationRequestValidator.validateSendNotificationRequest(request))
                .thenReturn(validationErrors);
        when(sendNotificationResponseBuilder.buildFailureResponse("Request Validation Failed: [Invalid chat ID]"))
                .thenReturn(SendNotificationResponse.builder()
                        .responseStatus("400")
                        .customMessage("Request Validation Failed: [Invalid chat ID]")
                        .build());
        // Act
        SendNotificationResponse response = telegramRequestProcessor.processNotificationRequest(request);

        // Assert
        verify(sendNotificationResponseBuilder).buildFailureResponse("Request Validation Failed: " + validationErrors);
        assertEquals("Request Validation Failed: [Invalid chat ID]", response.getCustomMessage());
    }

    @Test
    void testTextMediaProcessing() {
        // Arrange
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientList(Arrays.asList("123", "456"));
        request.setMediaTypeList(Collections.singletonList(MediaType.TEXT));
        request.setTextMessage("Hello, Telegram!");

        when(telegramSendNotificationRequestValidator.validateSendNotificationRequest(request))
                .thenReturn(Collections.emptyList());

        // Mock sendMarketingRequest to return success
        when(telegramHttpConnector.sendMarketingRequest(anyString(), anyMap()))
                .thenReturn(Mono.just("Success"));
        when(sendNotificationResponseBuilder.buildSuccessResponse("Notification request added to queue successfully"))
                .thenReturn(SendNotificationResponse.builder()
                        .responseStatus("200")
                        .customMessage("Notification request added to queue successfully")
                        .build());
        // Act
        SendNotificationResponse response = telegramRequestProcessor.processNotificationRequest(request);

        // Assert
        verify(telegramHttpConnector, times(2)).sendMarketingRequest(anyString(), anyMap());
        assertEquals("Notification request added to queue successfully", response.getCustomMessage());
    }

    @Test
    void testImageMediaProcessing() {
        // Arrange
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientList(Collections.singletonList("789"));
        request.setMediaTypeList(Collections.singletonList(MediaType.IMAGE));
        request.setImageUrl("http://example.com/image.jpg");
        request.setImageCaption("Caption");

        when(telegramSendNotificationRequestValidator.validateSendNotificationRequest(request))
                .thenReturn(Collections.emptyList());

        when(telegramHttpConnector.sendMarketingRequest(anyString(), anyMap()))
                .thenReturn(Mono.just("Success"));
        when(sendNotificationResponseBuilder.buildSuccessResponse("Notification request added to queue successfully"))
                .thenReturn(SendNotificationResponse.builder()
                        .responseStatus("200")
                        .customMessage("Notification request added to queue successfully")
                        .build());
        // Act
        SendNotificationResponse response = telegramRequestProcessor.processNotificationRequest(request);

        // Assert
        verify(telegramHttpConnector, times(1)).sendMarketingRequest(anyString(), anyMap());
        assertEquals("Notification request added to queue successfully", response.getCustomMessage());
    }

    @Test
    void testErrorDuringSend() {
        // Arrange
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientList(Collections.singletonList("123"));
        request.setMediaTypeList(Collections.singletonList(MediaType.TEXT));
        request.setTextMessage("Hello");

        when(telegramSendNotificationRequestValidator.validateSendNotificationRequest(request))
                .thenReturn(Collections.emptyList());

        when(telegramHttpConnector.sendMarketingRequest(anyString(), anyMap()))
                .thenReturn(Mono.error(new RuntimeException("Send failed")));

        when(sendNotificationResponseBuilder.buildSuccessResponse("Notification request added to queue successfully"))
                .thenReturn(SendNotificationResponse.builder()
                        .responseStatus("200")
                        .customMessage("Notification request added to queue successfully")
                        .build());
        // Act
        SendNotificationResponse response = telegramRequestProcessor.processNotificationRequest(request);

        // Assert
        verify(telegramResponseStore).storeResponse("123", "{\"ok\":false, \"error\":\"Send failed\"}");
        assertEquals("Notification request added to queue successfully", response.getCustomMessage());
    }

    @Test
    void testVideoMediaProcessing() {
        // Arrange
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientList(Collections.singletonList("789"));
        request.setMediaTypeList(Collections.singletonList(MediaType.VIDEO));
        request.setVideoUrl("http://example.com/video.mp4");
        request.setVideoCaption("Caption");

        when(telegramSendNotificationRequestValidator.validateSendNotificationRequest(request))
                .thenReturn(Collections.emptyList());

        when(telegramHttpConnector.sendMarketingRequest(anyString(), anyMap()))
                .thenReturn(Mono.just("Success"));
        when(sendNotificationResponseBuilder.buildSuccessResponse("Notification request added to queue successfully"))
                .thenReturn(SendNotificationResponse.builder()
                        .responseStatus("200")
                        .customMessage("Notification request added to queue successfully")
                        .build());
        // Act
        SendNotificationResponse response = telegramRequestProcessor.processNotificationRequest(request);

        // Assert
        verify(telegramHttpConnector, times(1)).sendMarketingRequest(anyString(), anyMap());
        assertEquals("Notification request added to queue successfully", response.getCustomMessage());
    }
}
