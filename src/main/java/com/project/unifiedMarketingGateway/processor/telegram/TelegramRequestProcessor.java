package com.project.unifiedMarketingGateway.processor.telegram;

import com.project.unifiedMarketingGateway.builders.TelegramPayloadBuilder;
import com.project.unifiedMarketingGateway.connectors.TelegramHttpConnector;
import com.project.unifiedMarketingGateway.contexts.SendContext;
import com.project.unifiedMarketingGateway.dto.SendResultDTO;
import com.project.unifiedMarketingGateway.enums.MediaType;
import com.project.unifiedMarketingGateway.metrics.MetricsService;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import com.project.unifiedMarketingGateway.processor.DeliveryStateService;
import com.project.unifiedMarketingGateway.processor.IdempotencyService;
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

import java.util.*;
import java.util.function.Function;

import static com.project.unifiedMarketingGateway.constants.Constants.*;
import static com.project.unifiedMarketingGateway.enums.ClientType.TELEGRAM;
import static com.project.unifiedMarketingGateway.enums.ClientType.WHATSAPP;

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
    @Autowired
    IdempotencyService idempotencyService;
    @Autowired
    DeliveryStateService deliveryStateService;

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
        String requestId = UUID.randomUUID().toString();

        boolean allQueued = true;
        List<String> mediaDisabledErrorList = new ArrayList<>();

        // iterate media types and queue work (synchronous control only indicates queuing success)
        for (MediaType mediaType : mediaTypeList) {
            boolean ok;
            switch (mediaType) {
                case TEXT -> {
                    if(isTextEnabled)
                        ok = prepareAndSendTextMedia(recipientList, textMessage, requestId);
                    else {
                        mediaDisabledErrorList.add(TEXT_MEDIA_DISABLED_ERROR);
                        ok = false;
                    }
                }
                case IMAGE -> {
                    if(isImageEnabled)
                        ok = prepareAndSendImageMedia(recipientList, imageUrl, imageCaption, requestId);
                    else {
                        mediaDisabledErrorList.add(IMAGE_MEDIA_DISABLED_ERROR);
                        ok = false;
                    }
                }
                case VIDEO -> {
                    if(isVideoEnabled)
                        ok = prepareAndSendVideoMedia(recipientList, videoUrl, videoCaption, requestId);
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
            String method, String requestId) {
        int concurrency = Math.min(maxConcurrency, recipientList.size());

        Flux.fromIterable(recipientList)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .flatMap(chatId -> {
                    Map<String, Object> payload = payloadForChat.apply(chatId);
                    return executeRequestReactive(chatId, payload, method, requestId);
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

    private Mono<SendResultDTO> executeRequestReactive(String chatId, Map<String, Object> payload, String method, String requestId) {
        Optional<SendContext> ctxOpt = preSendChecksAndRecords(chatId, method, requestId);
        if (ctxOpt.isEmpty()) {
            return Mono.just(
                    new SendResultDTO(
                            chatId,
                            true,
                            "DUPLICATE",
                            null
                    )
            );
        }
        SendContext ctx = ctxOpt.get();


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

    private boolean prepareAndSendTextMedia(List<String> recipientList, String textMessage, String requestId) {
        return prepareAndSendMedia(recipientList,
                chatId -> payloadBuilder.buildTextPayload(chatId, textMessage),
                TELEGRAM_SEND_MESSAGE_METHOD, requestId);
    }

    private boolean prepareAndSendImageMedia(List<String> recipientList, String imageUrl, String imageCaption, String requestId) {
        return prepareAndSendMedia(recipientList,
                chatId -> payloadBuilder.buildImagePayload(chatId, imageUrl, imageCaption),
                TELEGRAM_SEND_PHOTO_METHOD, requestId);
    }

    private boolean prepareAndSendVideoMedia(List<String> recipientList, String videoUrl, String videoCaption, String requestId) {
        return prepareAndSendMedia(recipientList,
                chatId -> payloadBuilder.buildVideoPayload(chatId, videoUrl, videoCaption),
                TELEGRAM_SEND_VIDEO_METHOD, requestId);
    }

    private Optional<SendContext> preSendChecksAndRecords(String chatId, String method, String requestId) {
        SendContext ctx = SendContext.builder()
                .channel(TELEGRAM.getValue())
                .method(method)
                .recipient(chatId)
                .requestId(requestId)
                .build();

        boolean allowed = idempotencyService.tryStart(
                ctx.getRequestId(),
                ctx.getChannel(),
                ctx.getRecipient(),
                ctx.getMethod()
        );

        if (!allowed) {
            log.info("[{}] Duplicate request blocked for {}", ctx.getRequestId(), chatId);
            return Optional.empty();
        }

        metricsService.incrementSendAttempt(ctx.getChannel(), ctx.getMethod());
        metricsService.incrementInFlight(ctx.getChannel());
        deliveryStateService.markQueued(ctx);
        ctx.markStart();

        return Optional.of(ctx);
    }

    private void recordSuccess(SendContext ctx, String body) {
        idempotencyService.markCompleted(
                ctx.getRequestId(),
                ctx.getChannel(),
                ctx.getRecipient(),
                ctx.getMethod()
        );
        try {
            telegramResponseStore.storeResponse(ctx.getRecipient(), body);
        } catch (Exception e) {
            log.warn("[{}] failed to persist success response: {}", ctx.getRecipient(), e.toString());
        }

        // metrics
        metricsService.incrementSendSuccess(ctx.getChannel(), ctx.getMethod());
        metricsService.recordHttpLatency(ctx.getChannel(), ctx.getMethod(), ctx.elapsed());
        metricsService.decrementInFlight(ctx.getChannel());
        deliveryStateService.markSent(ctx, ctx.getRecipient());
    }

    private void recordFailure(SendContext ctx, String errorMessage) {
        idempotencyService.markFailed(
                ctx.getRequestId(),
                ctx.getChannel(),
                ctx.getRecipient(),
                ctx.getMethod()
        );

        try {
            telegramResponseStore.storeResponse(ctx.getRecipient(), "{\"ok\":false,\"error\":\"" + errorMessage + "\"}");
        } catch (Exception e) {
            log.warn("[{}] failed to persist error response: {}", ctx.getRecipient(), e.toString());
        }

        // metrics
        metricsService.incrementSendFailure(ctx.getChannel(), ctx.getMethod(), errorMessage);
        metricsService.recordHttpLatency(ctx.getChannel(), ctx.getMethod(), ctx.elapsed());
        metricsService.decrementInFlight(ctx.getChannel());
        deliveryStateService.markFailed(ctx, errorMessage);
    }
}
