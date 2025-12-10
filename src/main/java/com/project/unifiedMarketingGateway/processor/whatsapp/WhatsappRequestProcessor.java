package com.project.unifiedMarketingGateway.processor.whatsapp;

import com.project.unifiedMarketingGateway.builders.SendNotificationResponseBuilder;
import com.project.unifiedMarketingGateway.builders.WhatsappPayloadBuilder;
import com.project.unifiedMarketingGateway.connectors.WhatsappHttpConnector;
import com.project.unifiedMarketingGateway.dto.SendResultDTO;
import com.project.unifiedMarketingGateway.enums.MediaType;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import com.project.unifiedMarketingGateway.processor.RequestProcessorInterface;
import com.project.unifiedMarketingGateway.responseStore.WhatsappResponseStore;
import com.project.unifiedMarketingGateway.retryHandler.WhatsappReactiveRetryHandler;
import com.project.unifiedMarketingGateway.validators.WhatsappSendNotificationRequestValidator;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.project.unifiedMarketingGateway.constants.Constants.MESSAGES;

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

    @Value("${whatsapp.maxConcurrency:5}")
    private int maxConcurrency;

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

        for (MediaType mediaType : mediaTypeList) {
            boolean ok;
            switch (mediaType) {
                case TEXT -> ok = prepareAndSendTextMedia(recipientList, textMessage);
                case IMAGE -> ok = prepareAndSendImageMedia(recipientList, imageUrl, imageCaption);
                case VIDEO -> ok = prepareAndSendVideoMedia(recipientList, videoUrl, videoCaption);
                default      -> ok = false;
            }
            anyQueued  = anyQueued || ok;
            allQueued  = allQueued && ok;
        }

        if (!anyQueued) {
            return responseBuilder.buildFailureResponse("Notification request couldn't be processed for WhatsApp.");
        }
        if (!allQueued) {
            return responseBuilder.buildFailureResponse("Notification request was only partially queued for WhatsApp.");
        }

        return responseBuilder.buildSuccessResponse("Notification request added to queue successfully for WhatsApp.");
    }

    private Mono<SendResultDTO> executeRequestReactive(String chatID, Map<String, Object> payload) {
        Mono<String> httpCall = reactiveRetryHandler.withRetry(
                () -> whatsappHttpConnector.sendMarketingRequest(MESSAGES, payload)
        );

        return httpCall
                .map(body -> {
                    whatsappResponseStore.storeResponse(chatID, body);
                    return new SendResultDTO(chatID, true, body, null);
                })
                .onErrorResume(err -> {
                    whatsappResponseStore.storeResponse(
                            chatID,
                            "{\"ok\":false,\"error\":\"" + err.getMessage() + "\"}"
                    );
                    return Mono.just(new SendResultDTO(chatID, false, null, err.getMessage()));
                });
    }

    private boolean prepareAndSendMedia(List<String> recipientList,
            Function<String, Map<String, Object>> payloadForRecipient) {
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
                    return executeRequestReactive(chatID, payload);
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
                chatID -> payloadBuilder.buildTextPayload(chatID, textMessage)
        );
    }

    private boolean prepareAndSendImageMedia(List<String> recipientList, String imageUrl, String imageCaption) {
        return prepareAndSendMedia(
                recipientList,
                chatID -> payloadBuilder.buildImagePayload(chatID, imageUrl, imageCaption)
        );
    }

    private boolean prepareAndSendVideoMedia(List<String> recipientList, String videoUrl, String videoCaption) {
        return prepareAndSendMedia(
                recipientList,
                chatID -> payloadBuilder.buildVideoPayload(chatID, videoUrl, videoCaption)
        );
    }
}

