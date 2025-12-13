package com.project.unifiedMarketingGateway.processor.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.unifiedMarketingGateway.builders.SendNotificationResponseBuilder;
import com.project.unifiedMarketingGateway.builders.WhatsappPayloadBuilder;
import com.project.unifiedMarketingGateway.connectors.WhatsappHttpConnector;
import com.project.unifiedMarketingGateway.contexts.SendContext;
import com.project.unifiedMarketingGateway.dto.SendResultDTO;
import com.project.unifiedMarketingGateway.enums.MediaType;
import com.project.unifiedMarketingGateway.enums.WhatsappMessageStatus;
import com.project.unifiedMarketingGateway.metrics.MetricsService;
import com.project.unifiedMarketingGateway.store.messageStore.WhatsappMessageStore;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import com.project.unifiedMarketingGateway.models.WhatsappMessage;
import com.project.unifiedMarketingGateway.processor.RequestProcessorInterface;
import com.project.unifiedMarketingGateway.store.responseStore.WhatsappResponseStore;
import com.project.unifiedMarketingGateway.retryHandler.WhatsappReactiveRetryHandler;
import com.project.unifiedMarketingGateway.validators.WhatsappSendNotificationRequestValidator;
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
import static com.project.unifiedMarketingGateway.constants.Constants.VIDEO_MEDIA_DISABLED_ERROR;
import static com.project.unifiedMarketingGateway.enums.ClientType.WHATSAPP;

@Service
@Slf4j
public class WhatsappRequestProcessor implements RequestProcessorInterface {

    @Autowired
    SendNotificationResponseBuilder responseBuilder;
    @Autowired
    WhatsappSendNotificationRequestValidator requestValidator;
    @Autowired
    WhatsappHttpConnector whatsappHttpConnector;
    @Autowired
    WhatsappResponseStore whatsappResponseStore;
    @Autowired
    WhatsappPayloadBuilder payloadBuilder;
    @Autowired
    WhatsappReactiveRetryHandler reactiveRetryHandler;
    @Autowired
    WhatsappMessageStore messageStore;
    @Autowired
    MetricsService metricsService;

    @Value("${whatsapp.maxConcurrency:5}")
    private int maxConcurrency;

    @Value("${whatsapp.contentBasedResource.text.isEnabled:false}")
    private boolean isTextEnabled;

    @Value("${whatsapp.contentBasedResource.image.isEnabled:false}")
    private boolean isImageEnabled;

    @Value("${whatsapp.contentBasedResource.video.isEnabled:false}")
    private boolean isVideoEnabled;

    @Autowired ObjectMapper objectMapper;

    @Override
    public SendNotificationResponse processNotificationRequest(@NonNull SendNotificationRequest request) {
        // 1. Validation
        List<String> errors = requestValidator.validateSendNotificationRequest(request);
        if (!errors.isEmpty()) {
            return responseBuilder.buildFailureResponse("Request Validation Failed: " + errors);
        }

        List<String> recipientList = request.getRecipientList();
        List<MediaType> mediaTypeList = request.getMediaTypeList();

        if (recipientList == null || recipientList.isEmpty()) {
            return responseBuilder.buildFailureResponse("Recipient list is empty for WhatsApp.");
        }
        if (mediaTypeList == null || mediaTypeList.isEmpty()) {
            return responseBuilder.buildFailureResponse("No media types specified for WhatsApp.");
        }

        String textMessage  = request.getTextMessage();
        String imageUrl     = request.getImageUrl();
        String imageCaption = request.getImageCaption();
        String videoUrl     = request.getVideoUrl();
        String videoCaption = request.getVideoCaption();

        boolean anyQueued  = false;
        boolean allQueued  = true;
        List<String> mediaDisabledErrorList = new ArrayList<>();

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

        if (!anyQueued) {
            return responseBuilder.buildFailureResponse("Notification request couldn't be processed for WhatsApp." + mediaDisabledErrorList.toString());
        }
        if (!allQueued) {
            return responseBuilder.buildFailureResponse("Notification request was only partially queued for WhatsApp." + mediaDisabledErrorList.toString());
        }

        return responseBuilder.buildSuccessResponse("Notification request added to queue successfully for WhatsApp.");
    }

    public Mono<SendResultDTO> executeRequestReactive(String chatID,
            Map<String, Object> payload,
            MediaType mediaType) {
        SendContext ctx = SendContext.builder()
                .channel(WHATSAPP.getValue())
                .method(mediaType.getValue())
                .recipient(chatID)
                .requestId(UUID.randomUUID().toString())
                .build();
        metricsService.incrementSendAttempt(ctx.getChannel(), ctx.getMethod());
        metricsService.incrementInFlight(ctx.getChannel());
        ctx.markStart();

        Mono<String> httpCall = reactiveRetryHandler.withRetry(
                () -> whatsappHttpConnector.sendMarketingRequest(MESSAGES, payload)
        );

        return httpCall
                .publishOn(Schedulers.boundedElastic())
                .flatMap(body -> Mono.fromCallable(() -> {
                            saveResponseToDB(body, chatID, mediaType);
                            recordSuccess(ctx, body);
                            return new SendResultDTO(chatID, true, body, null);
                        })
                )
                .onErrorResume(err -> Mono.fromCallable(() -> {
                            String msg = err.getMessage() == null ? err.toString() : err.getMessage();
                            recordFailure(ctx, msg);
                            return new SendResultDTO(chatID, false, null, msg);
                        })
                );
    }

    private boolean prepareAndSendMedia(List<String> recipientList,
            Function<String, Map<String, Object>> payloadForRecipient, MediaType mediaType) {
        if (recipientList == null || recipientList.isEmpty()) {
            log.warn("No recipients provided for WhatsApp; nothing queued");
            return false;
        }

        int concurrency = Math.min(Math.max(1, maxConcurrency), recipientList.size());

        Flux.fromIterable(recipientList)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .flatMap(chatID -> {
                    Map<String, Object> payload = payloadForRecipient.apply(chatID);
                    return executeRequestReactive(chatID, payload, mediaType);
                }, concurrency)
                .doOnSubscribe(s -> log.info("Dispatching {} WhatsApp sends (concurrency={})", recipientList.size(), concurrency))
                .subscribe(
                        result -> {
                            if (result.isSuccess()) {
                                log.info("[{}] WhatsApp sent successfully", result.getChatId());
                            } else {
                                log.warn("[{}] WhatsApp send failed: {}", result.getChatId(), result.getErrorMessage());
                            }
                        },
                        err -> log.error("WhatsApp reactive pipeline error: {}", err.toString()),
                        () -> log.info("All WhatsApp send requests dispatched")
                );

        return true;
    }

    private boolean prepareAndSendTextMedia(List<String> recipientList, String textMessage) {
        return prepareAndSendMedia(
                recipientList,
                chatID -> payloadBuilder.buildTextPayload(chatID, textMessage), MediaType.TEXT
        );
    }

    private boolean prepareAndSendImageMedia(List<String> recipientList, String imageUrl, String imageCaption) {
        return prepareAndSendMedia(
                recipientList,
                chatID -> payloadBuilder.buildImagePayload(chatID, imageUrl, imageCaption), MediaType.IMAGE
        );
    }

    private boolean prepareAndSendVideoMedia(List<String> recipientList, String videoUrl, String videoCaption) {
        return prepareAndSendMedia(
                recipientList,
                chatID -> payloadBuilder.buildVideoPayload(chatID, videoUrl, videoCaption), MediaType.VIDEO
        );
    }

    private void saveResponseToDB(String body, String chatID, MediaType mediaType)
    {
        String waMessageId = extractWhatsAppMessageId(body);
        if (waMessageId != null) {
            WhatsappMessage msg = WhatsappMessage.builder()
                    .waMessageId(waMessageId)
                    .recipient(chatID)
                    .mediaType(mediaType.name())
                    .createdAtEpochMillis(System.currentTimeMillis())
                    .lastUpdatedEpochMillis(System.currentTimeMillis())
                    .status(WhatsappMessageStatus.SENT)
                    .build();
            messageStore.saveOutbound(msg);
        } else {
            log.warn("[{}] WA send success but no message id parsed", chatID);
        }

    }

    private String extractWhatsAppMessageId(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode arr = root.get("messages");
            if (arr != null && arr.isArray() && arr.size() > 0) {
                JsonNode first = arr.get(0);
                JsonNode idNode = first.get("id");
                if (idNode != null && !idNode.isNull()) {
                    return idNode.asText();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse WA message id from response: {}", e.toString());
        }
        return null;
    }

    private void recordSuccess(SendContext ctx, String body) {
        try {
            whatsappResponseStore.storeResponse(ctx.getRecipient(), body);
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
            whatsappResponseStore.storeResponse(ctx.getRecipient(), "{\"ok\":false,\"error\":\"" + errorMessage + "\"}");
        } catch (Exception e) {
            log.warn("[{}] failed to persist error response: {}", ctx.getRecipient(), e.toString());
        }

        // metrics
        metricsService.incrementSendFailure(ctx.getChannel(), ctx.getMethod(), errorMessage);
        metricsService.recordHttpLatency(ctx.getChannel(), ctx.getMethod(), ctx.elapsed());
        metricsService.decrementInFlight(ctx.getChannel());
    }
}

