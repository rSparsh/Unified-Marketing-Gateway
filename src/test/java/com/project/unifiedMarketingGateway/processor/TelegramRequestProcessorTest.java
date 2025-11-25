package com.project.unifiedMarketingGateway.processor;

import com.project.unifiedMarketingGateway.builders.TelegramPayloadBuilder;
import com.project.unifiedMarketingGateway.connectors.TelegramHttpConnector;
import com.project.unifiedMarketingGateway.enums.MediaType;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import com.project.unifiedMarketingGateway.builders.SendNotificationResponseBuilder;
import com.project.unifiedMarketingGateway.processor.telegram.TelegramRequestProcessor;
import com.project.unifiedMarketingGateway.responseStore.TelegramResponseStore;
import com.project.unifiedMarketingGateway.retryHandler.TelegramReactiveRetryHandler;
import com.project.unifiedMarketingGateway.validators.TelegramSendNotificationRequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
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
    private SendNotificationResponseBuilder responseBuilder;

    @Mock
    private TelegramSendNotificationRequestValidator requestValidator;

    @Mock
    TelegramReactiveRetryHandler reactiveRetryHandler;

    @Mock
    TelegramPayloadBuilder payloadBuilder;


    @BeforeEach
    void setUp() throws Exception {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Set up the processor with mocks using reflection
        telegramRequestProcessor = new TelegramRequestProcessor();
        setField(telegramRequestProcessor, "telegramHttpConnector", telegramHttpConnector);
        setField(telegramRequestProcessor, "telegramResponseStore", telegramResponseStore);
        setField(telegramRequestProcessor, "responseBuilder", responseBuilder);
        setField(telegramRequestProcessor, "requestValidator", requestValidator);
        setField(telegramRequestProcessor, "maxConcurrency", 3);
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

        when(requestValidator.validateSendNotificationRequest(request))
                .thenReturn(validationErrors);
        when(responseBuilder.buildFailureResponse("Request Validation Failed: [Invalid chat ID]"))
                .thenReturn(SendNotificationResponse.builder()
                        .responseStatus("400")
                        .customMessage("Request Validation Failed: [Invalid chat ID]")
                        .build());
        // Act
        SendNotificationResponse response = telegramRequestProcessor.processNotificationRequest(request);

        // Assert
        verify(responseBuilder).buildFailureResponse("Request Validation Failed: " + validationErrors);
        assertEquals("Request Validation Failed: [Invalid chat ID]", response.getCustomMessage());
    }

    @Test
    void testTextMediaProcessing() {
        SendNotificationRequest baseRequest = getBaseRequest(List.of(MediaType.TEXT));
        SendNotificationResponse successResponse = getSuccessResponse();

        when(requestValidator.validateSendNotificationRequest(any())).thenReturn(Collections.emptyList());
        when(responseBuilder.buildSuccessResponse(anyString())).thenReturn(successResponse);
        when(payloadBuilder.buildTextPayload(eq("111"), eq("hello"))).thenReturn(Map.of("chat_id", "111", "text", "hello"));
        when(payloadBuilder.buildTextPayload(eq("222"), eq("hello"))).thenReturn(Map.of("chat_id", "222", "text", "hello"));
        when(reactiveRetryHandler.withRetry(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    java.util.function.Supplier<Mono<String>> supplier = (java.util.function.Supplier<Mono<String>>) invocation.getArgument(0);
                    return supplier.get().onErrorResume(e -> Mono.just("error")); // supplier returns whatever connector returns
                });

        when(telegramHttpConnector.sendMarketingRequest(eq("sendMessage"), anyMap())).thenReturn(Mono.just("{\"ok\":true}"));

        // Act
        SendNotificationResponse resp = telegramRequestProcessor.processNotificationRequest(baseRequest);

        // Assert returned success
        assertSame(successResponse, resp);
        verify(responseBuilder, times(1)).buildSuccessResponse(contains("added to queue"));
    }



    @Test
    void testImageMediaProcessing() {
        SendNotificationRequest baseRequest = getBaseRequest(List.of(MediaType.TEXT));
        SendNotificationResponse successResponse = getSuccessResponse();

        when(requestValidator.validateSendNotificationRequest(any())).thenReturn(Collections.emptyList());
        when(responseBuilder.buildSuccessResponse(anyString())).thenReturn(successResponse);
        when(payloadBuilder.buildImagePayload(eq("111"), eq("https://example.com/img.jpg"), eq("caption")))
                .thenReturn(Map.of("chat_id", "111", "photo", "https://example.com/img.jpg", "caption", "caption"));
        when(payloadBuilder.buildImagePayload(eq("222"), eq("https://example.com/img.jpg"), eq("caption")))
                .thenReturn(Map.of("chat_id", "222", "photo", "https://example.com/img.jpg", "caption", "caption"));

        when(reactiveRetryHandler.withRetry(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    java.util.function.Supplier<Mono<String>> supplier =
                            (java.util.function.Supplier<Mono<String>>) invocation.getArgument(0);
                    return supplier.get();
                });

        when(telegramHttpConnector.sendMarketingRequest(eq("sendPhoto"), anyMap())).thenReturn(Mono.just("{\"ok\":true}"));

        // Act
        SendNotificationResponse resp = telegramRequestProcessor.processNotificationRequest(baseRequest);

        // Assert returned success
        assertSame(successResponse, resp);
        verify(responseBuilder, times(1)).buildSuccessResponse(contains("added to queue"));
    }

    @Test
    void testVideoMediaProcessing() {
        SendNotificationRequest baseRequest = getBaseRequest(List.of(MediaType.VIDEO));
        SendNotificationResponse successResponse = getSuccessResponse();

        when(requestValidator.validateSendNotificationRequest(any())).thenReturn(Collections.emptyList());
        when(responseBuilder.buildSuccessResponse(anyString())).thenReturn(successResponse);
        when(payloadBuilder.buildVideoPayload(eq("111"), eq("https://example.com/vid.mp4"), eq("vcaption")))
                .thenReturn(Map.of("chat_id", "111", "video", "https://example.com/vid.mp4", "caption", "vcaption"));
        when(payloadBuilder.buildVideoPayload(eq("222"), eq("https://example.com/vid.mp4"), eq("vcaption")))
                .thenReturn(Map.of("chat_id", "222", "video", "https://example.com/vid.mp4", "caption", "vcaption"));

        // reactive retry returns supplier-invoked Mono -> simulate connector behavior
        when(reactiveRetryHandler.withRetry(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    java.util.function.Supplier<Mono<String>> supplier =
                            (java.util.function.Supplier<Mono<String>>) invocation.getArgument(0);
                    return supplier.get();
                });

        // connector returns successful Mono for video send
        when(telegramHttpConnector.sendMarketingRequest(eq("sendVideo"), anyMap())).thenReturn(Mono.just("{\"ok\":true}"));

        // Act
        SendNotificationResponse resp = telegramRequestProcessor.processNotificationRequest(baseRequest);

        // Assert returned success
        assertSame(successResponse, resp);
        verify(responseBuilder, times(1)).buildSuccessResponse(contains("added to queue"));

    }

    private SendNotificationRequest getBaseRequest(List<MediaType> mediaTypeList)
    {
        return SendNotificationRequest.builder()
                .recipientList(List.of("111", "222"))
                .mediaTypeList(mediaTypeList)
                .textMessage("hello")
                .imageUrl("http://example.com/image.jpg")
                .imageCaption("image caption")
                .videoUrl("http://example.com/video.mp4")
                .videoCaption("video caption")
                .build();
    }

    private SendNotificationResponse getSuccessResponse()
    {
        return SendNotificationResponse.builder()
                .responseStatus("200")
                .customMessage("Notification queued")
                .build();
    }
}
