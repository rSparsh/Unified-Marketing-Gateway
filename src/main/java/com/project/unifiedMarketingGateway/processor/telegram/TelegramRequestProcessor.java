package com.project.unifiedMarketingGateway.processor.telegram;

import com.project.unifiedMarketingGateway.builders.TelegramPayloadBuilder;
import com.project.unifiedMarketingGateway.connectors.TelegramHttpConnector;
import com.project.unifiedMarketingGateway.contexts.SendContext;
import com.project.unifiedMarketingGateway.dto.SendResultDTO;
import com.project.unifiedMarketingGateway.enums.MediaType;
import com.project.unifiedMarketingGateway.metrics.MetricsService;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import com.project.unifiedMarketingGateway.processor.RequestProcessorInterface;
import com.project.unifiedMarketingGateway.builders.SendNotificationResponseBuilder;
import com.project.unifiedMarketingGateway.store.responseStore.TelegramResponseStore;
import com.project.unifiedMarketingGateway.retryHandler.TelegramReactiveRetryHandler;
import com.project.unifiedMarketingGateway.validators.TelegramSendNotificationRequestValidator;
import io.micrometer.core.instrument.Timer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.project.unifiedMarketingGateway.constants.Constants.*;
import static com.project.unifiedMarketingGateway.enums.ClientType.TELEGRAM;

@Slf4j
@Service
public class TelegramRequestProcessor implements RequestProcessorInterface {

    @Autowired
    TelegramHttpConnector telegramHttpConnector;

    @Autowired
    TelegramResponseStore telegramResponseStore;

    @Autowired
    SendNotificationResponseBuilder responseBuilder;

    @Autowired
    TelegramSendNotificationRequestValidator requestValidator;

    @Autowired
    TelegramReactiveRetryHandler reactiveRetryHandler;

    @Autowired
    TelegramPayloadBuilder payloadBuilder;
    @Autowired
    MetricsService metricsService;

    @Value("${telegram.maxConcurrency:5}")
    private int maxConcurrency;

    @Value("${telegram.contentBasedResource.text.isEnabled:false}")
    private boolean isTextEnabled;

    @Value("${telegram.contentBasedResource.image.isEnabled:false}")
    private boolean isImageEnabled;

    @Value("${telegram.contentBasedResource.video.isEnabled:false}")
    private boolean isVideoEnabled;

//    private record SendResult(String chatId, boolean success, String responseBody, String errorMessage) {}

    @Override
    public SendNotificationResponse processNotificationRequest(@NonNull SendNotificationRequest sendNotificationRequest) {
        List<String> validationErrorList = requestValidator.validateSendNotificationRequest(sendNotificationRequest);
        if (!validationErrorList.isEmpty()) {
            return responseBuilder.buildFailureResponse("Request Validation Failed: " + validationErrorList.toString());
        }

        List<String> recipientList = sendNotificationRequest.getRecipientList();
        List<MediaType> mediaTypeList = sendNotificationRequest.getMediaTypeList();

        String textMessage = sendNotificationRequest.getTextMessage();
        String imageUrl = sendNotificationRequest.getImageUrl();
        String imageCaption = sendNotificationRequest.getImageCaption();
        String videoUrl = sendNotificationRequest.getVideoUrl();
        String videoCaption = sendNotificationRequest.getVideoCaption();

        boolean allQueued = true;
        List<String> mediaDisabledErrorList = new ArrayList<>();

        // iterate media types and queue work (synchronous control only indicates queuing success)
        for (MediaType mediaType : mediaTypeList) {
            boolean ok;
            switch (mediaType) {
                case TEXT -> {
                    if(isTextEnabled)
                        ok = prepareAndSendTextMedia(recipientList, textMessage);
                    else {
                        mediaDisabledErrorList.add(TEXT_MEDIA_DISABLED_ERROR);
                        ok = false;
                    }
                }
                case IMAGE -> {
                    if(isImageEnabled)
                        ok = prepareAndSendImageMedia(recipientList, imageUrl, imageCaption);
                    else {
                        mediaDisabledErrorList.add(IMAGE_MEDIA_DISABLED_ERROR);
                        ok = false;
                    }
                }
                case VIDEO -> {
                    if(isVideoEnabled)
                        ok = prepareAndSendVideoMedia(recipientList, videoUrl, videoCaption);
                    else {
                        mediaDisabledErrorList.add(VIDEO_MEDIA_DISABLED_ERROR);
                        ok = false;
                    }
                }
                default -> ok = false;
            }
            allQueued = allQueued && ok;
        }

        if (allQueued) {
            return responseBuilder.buildSuccessResponse("Notification request added to queue successfully");
        } else {
            return responseBuilder.buildFailureResponse("Notification request couldn't be processed." + mediaDisabledErrorList.toString());
        }
    }

    private boolean prepareAndSendMedia(List<String> recipientList,
            Function<String, Map<String, Object>> payloadForChat,
            String method) {
        int concurrency = Math.min(maxConcurrency, recipientList.size());

        Flux.fromIterable(recipientList)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .flatMap(chatId -> {
                    Map<String, Object> payload = payloadForChat.apply(chatId);
                    return executeRequestReactive(chatId, payload, method);
                }, concurrency)
                .doOnSubscribe(s -> log.info("Dispatching {} sends (method={} concurrency={})",
                        recipientList.size(), method, concurrency))
                .subscribe(
                        result -> {
                            if (result.isSuccess()) {
                                log.info("[{}] sent successfully", result.getChatId());
                            } else {
                                log.warn("[{}] failed: {}", result.getChatId(), result.getErrorMessage());
                            }
                        },
                        err -> log.error("Reactive pipeline error (should not cancel others): {}", err.toString()),
                        () -> log.info("All send requests dispatched for method {}", method)
                );

        return true;
    }

    private Mono<SendResultDTO> executeRequestReactive(String chatId, Map<String, Object> payload, String method) {
        SendContext ctx = SendContext.builder()
                .channel(TELEGRAM.getValue())
                .method(method)
                .recipient(chatId)
                .requestId(UUID.randomUUID().toString())
                .build();
        metricsService.incrementSendAttempt(ctx.getChannel(), ctx.getMethod());
        metricsService.incrementInFlight(ctx.getChannel());
        ctx.markStart();

        Mono<String> httpCall = reactiveRetryHandler.withRetry(() -> telegramHttpConnector.sendMarketingRequest(method, payload));

        return httpCall
                .publishOn(Schedulers.boundedElastic())
                .flatMap(body -> Mono.fromCallable(() -> {
                            // persist and metrics in sync block (boundedElastic)
                            recordSuccess(ctx, body);
                            return new SendResultDTO(chatId, true, body, null);
                        })
                )
                .onErrorResume(err -> Mono.fromCallable(() -> {
                            String msg = err.getMessage() == null ? err.toString() : err.getMessage();
                            recordFailure(ctx, msg);
                            return new SendResultDTO(chatId, false, null, msg);
                        })
                );
    }

    private boolean prepareAndSendTextMedia(List<String> recipientList, String textMessage) {
        return prepareAndSendMedia(recipientList,
                chatId -> payloadBuilder.buildTextPayload(chatId, textMessage),
                TELEGRAM_SEND_MESSAGE_METHOD);
    }

    private boolean prepareAndSendImageMedia(List<String> recipientList, String imageUrl, String imageCaption) {
        return prepareAndSendMedia(recipientList,
                chatId -> payloadBuilder.buildImagePayload(chatId, imageUrl, imageCaption),
                TELEGRAM_SEND_PHOTO_METHOD);
    }

    private boolean prepareAndSendVideoMedia(List<String> recipientList, String videoUrl, String videoCaption) {
        return prepareAndSendMedia(recipientList,
                chatId -> payloadBuilder.buildVideoPayload(chatId, videoUrl, videoCaption),
                TELEGRAM_SEND_VIDEO_METHOD);
    }

    private void recordSuccess(SendContext ctx, String body) {
        try {
            telegramResponseStore.storeResponse(ctx.getRecipient(), body);
        } catch (Exception e) {
            log.warn("[{}] failed to persist success response: {}", ctx.getRecipient(), e.toString());
        }

        // metrics
        metricsService.incrementSendSuccess(ctx.getChannel(), ctx.getMethod());
        metricsService.recordHttpLatency(ctx.getChannel(), ctx.getMethod(), ctx.elapsed());
        metricsService.decrementInFlight(ctx.getChannel());
    }

    private void recordFailure(SendContext ctx, String errorMessage) {
        try {
            telegramResponseStore.storeResponse(ctx.getRecipient(), "{\"ok\":false,\"error\":\"" + errorMessage + "\"}");
        } catch (Exception e) {
            log.warn("[{}] failed to persist error response: {}", ctx.getRecipient(), e.toString());
        }

        // metrics
        metricsService.incrementSendFailure(ctx.getChannel(), ctx.getMethod(), errorMessage);
        metricsService.recordHttpLatency(ctx.getChannel(), ctx.getMethod(), ctx.elapsed());
        metricsService.decrementInFlight(ctx.getChannel());
    }
}
