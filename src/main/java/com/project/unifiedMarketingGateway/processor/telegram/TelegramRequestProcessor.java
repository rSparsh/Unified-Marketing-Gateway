package com.project.unifiedMarketingGateway.processor.telegram;

import com.project.unifiedMarketingGateway.builders.TelegramPayloadBuilder;
import com.project.unifiedMarketingGateway.connectors.TelegramHttpConnector;
import com.project.unifiedMarketingGateway.dto.SendResultDTO;
import com.project.unifiedMarketingGateway.enums.MediaType;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import com.project.unifiedMarketingGateway.processor.RequestProcessorInterface;
import com.project.unifiedMarketingGateway.builders.SendNotificationResponseBuilder;
import com.project.unifiedMarketingGateway.responseStore.TelegramResponseStore;
import com.project.unifiedMarketingGateway.retryHandler.TelegramReactiveRetryHandler;
import com.project.unifiedMarketingGateway.validators.TelegramSendNotificationRequestValidator;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.project.unifiedMarketingGateway.constants.Constants.*;

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

    @Value("${telegram.maxConcurrency:5}")
    private int maxConcurrency;

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

        // iterate media types and queue work (synchronous control only indicates queuing success)
        for (MediaType mediaType : mediaTypeList) {
            boolean ok;
            switch (mediaType) {
                case TEXT -> ok = prepareAndSendTextMedia(recipientList, textMessage);
                case IMAGE -> ok = prepareAndSendImageMedia(recipientList, imageUrl, imageCaption);
                case VIDEO -> ok = prepareAndSendVideoMedia(recipientList, videoUrl, videoCaption);
                default -> ok = false;
            }
            allQueued = allQueued && ok;
        }

        if (allQueued) {
            return responseBuilder.buildSuccessResponse("Notification request added to queue successfully");
        } else {
            return responseBuilder.buildFailureResponse("Notification request couldn't be processed.");
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
        Mono<String> httpCall = reactiveRetryHandler.withRetry(() -> telegramHttpConnector.sendMarketingRequest(method, payload));

        return httpCall
                .publishOn(Schedulers.boundedElastic()) // offload blocking store to boundedElastic
                .map(body -> {
                    try {
                        telegramResponseStore.storeResponse(chatId, body);
                    } catch (Exception e) {
                        log.warn("[{}] failed to persist success response: {}", chatId, e.toString());
                    }
                    return new SendResultDTO(chatId, true, body, null);
                })
                .onErrorResume(err -> {
                    String msg = err.getMessage() == null ? err.toString() : err.getMessage();
                    try {
                        telegramResponseStore.storeResponse(chatId, "{\"ok\":false,\"error\":\"" + msg + "\"}");
                    } catch (Exception e) {
                        log.warn("[{}] failed to persist error response: {}", chatId, e.toString());
                    }
                    return Mono.just(new SendResultDTO(chatId, false, null, msg));
                });
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
}
