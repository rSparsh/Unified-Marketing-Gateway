package com.project.unifiedMarketingGateway.processor.telegram;

import com.project.unifiedMarketingGateway.connector.TelegramHttpConnector;
import com.project.unifiedMarketingGateway.enums.MediaType;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import com.project.unifiedMarketingGateway.processor.RequestProcessorInterface;
import com.project.unifiedMarketingGateway.responseBuilders.SendNotificationResponseBuilder;
import com.project.unifiedMarketingGateway.responseStore.TelegramResponseStore;
import com.project.unifiedMarketingGateway.validators.TelegramSendNotificationRequestValidator;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.project.unifiedMarketingGateway.constants.Constants.*;
import static com.project.unifiedMarketingGateway.enums.MediaType.TEXT;

@Slf4j
@Service
public class TelegramRequestProcessor implements RequestProcessorInterface {

    @Autowired
    TelegramHttpConnector telegramHttpConnector;

    @Autowired
    TelegramResponseStore telegramResponseStore;

    @Autowired
    SendNotificationResponseBuilder sendNotificationResponseBuilder;

    @Autowired
    TelegramSendNotificationRequestValidator telegramSendNotificationRequestValidator;

    private final int concurrency = 8;

    @Override
    public SendNotificationResponse processNotificationRequest(@NonNull SendNotificationRequest sendNotificationRequest) {
        List<String> validationErrorList = telegramSendNotificationRequestValidator.validateSendNotificationRequest(sendNotificationRequest);
        if(!validationErrorList.isEmpty())
            return sendNotificationResponseBuilder.buildFailureResponse("Request Validation Failed: " + validationErrorList.toString());

        List<String> recipientList = sendNotificationRequest.getRecipientList();
        List<MediaType> mediaTypeList = sendNotificationRequest.getMediaTypeList();

        String textMessage = sendNotificationRequest.getTextMessage();
        String imageUrl = sendNotificationRequest.getImageUrl();
        String imageCaption = sendNotificationRequest.getImageCaption();
        String videoUrl = sendNotificationRequest.getVideoUrl();
        String videoCaption = sendNotificationRequest.getVideoCaption();

        AtomicBoolean operationStatus = new AtomicBoolean(true);

        mediaTypeList.stream().forEach(
                mediaType -> {
                    switch(mediaType){
                        case TEXT:
                            boolean ok = prepareAndSendTextMedia(recipientList, textMessage);
                            operationStatus.set(operationStatus.get() && ok); break;
                        case IMAGE:
                            ok = prepareAndSendImageMedia(recipientList, imageUrl, imageCaption);
                            operationStatus.set(operationStatus.get() && ok); break;
                        case VIDEO:
                            ok = prepareAndSendVideoMedia(recipientList, videoUrl, videoCaption);
                            operationStatus.set(operationStatus.get() && ok); break;
                    }
                }
        );

        if(operationStatus.get())
            return sendNotificationResponseBuilder.buildSuccessResponse("Notification request added to queue successfully");
        else
            return sendNotificationResponseBuilder.buildFailureResponse("Notification request couldn't be processed.");
    }

    private boolean prepareAndSendTextMedia(List<String> recipientList, String textMessage)
    {
        if (recipientList == null || recipientList.isEmpty()) {
            log.warn("No recipients to send to");
            return false;
        }

        Flux.fromIterable(recipientList)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(id -> Map.<String, Object>of(CHAT_ID, id, String.valueOf(TEXT).toLowerCase(), textMessage))
                .flatMap(payload -> {
                    String recipientId = String.valueOf(payload.get(CHAT_ID));
                    return telegramHttpConnector.sendMarketingRequest(TELEGRAM_SEND_MESSAGE_METHOD, payload)
                            .doOnNext(resp -> {
                                telegramResponseStore.storeResponse(recipientId, resp);
                                //TODO: add data persistency call here to store responses
//                                telegramResponseStore.persistResponseAsync(recipientId, resp);
                            })
                            .doOnError(err -> {
                                log.error("Error sending to {} : {}", recipientId, err.toString());
                                telegramResponseStore.storeResponse(recipientId, "{\"ok\":false, \"error\":\"" + err.getMessage() + "\"}");
                            })
                            .onErrorResume(e -> Mono.empty());
                }, concurrency)
                .doOnSubscribe(sub -> log.info("Starting async send for {} recipients", recipientList.size()))
                .subscribe(
                        item -> log.info("Text Message added to publishing queue successfully."),
                        err -> log.error("Reactive pipeline error (shouldn't cancel others): {}", err.toString()),
                        () -> log.info("All send requests dispatched (responses may still be processing).")
                );

        return true;
    }

    private boolean prepareAndSendImageMedia(List<String> recipientList, String imageUrl, String imageCaption)
    {
        if (recipientList == null || recipientList.isEmpty()) {
            log.warn("No recipients to send to");
            return false;
        }

        Flux.fromIterable(recipientList)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(id -> Map.<String, Object>of(
                        CHAT_ID, id,
                        PHOTO, imageUrl,
                        CAPTION, imageCaption))
                .flatMap(payload -> {
                    String recipientId = String.valueOf(payload.get(CHAT_ID));
                    return telegramHttpConnector.sendMarketingRequest(TELEGRAM_SEND_PHOTO_METHOD, payload)
                            .doOnNext(resp -> {
                                telegramResponseStore.storeResponse(recipientId, resp);
                                //TODO: add data persistency call here to store responses
//                                telegramResponseStore.persistResponseAsync(recipientId, resp);
                            })
                            .doOnError(err -> {
                                log.error("Error sending to {} : {}", recipientId, err.toString());
                                telegramResponseStore.storeResponse(recipientId, "{\"ok\":false, \"error\":\"" + err.getMessage() + "\"}");
                            })
                            .onErrorResume(e -> Mono.empty());
                }, concurrency)
                .doOnSubscribe(sub -> log.info("Starting async send for {} recipients", recipientList.size()))
                .subscribe(
                        item -> log.info("Image Message added to publishing queue successfully."),
                        err -> log.error("Reactive pipeline error (shouldn't cancel others): {}", err.toString()),
                        () -> log.info("All send requests dispatched (responses may still be processing).")
                );

        return true;
    }

    private boolean prepareAndSendVideoMedia(List<String> recipientList, String videoUrl, String videoCaption)
    {
        if (recipientList == null || recipientList.isEmpty()) {
            log.warn("No recipients to send to");
            return false;
        }

        Flux.fromIterable(recipientList)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(id -> Map.<String, Object>of(
                        CHAT_ID, id,
                        VIDEO, videoUrl,
                        CAPTION, videoCaption))
                .flatMap(payload -> {
                    String recipientId = String.valueOf(payload.get(CHAT_ID));
                    return telegramHttpConnector.sendMarketingRequest(TELEGRAM_SEND_VIDEO_METHOD, payload)
                            .doOnNext(resp -> {
                                telegramResponseStore.storeResponse(recipientId, resp);
                                //TODO: add data persistency call here to store responses
//                                telegramResponseStore.persistResponseAsync(recipientId, resp);
                            })
                            .doOnError(err -> {
                                log.error("Error sending to {} : {}", recipientId, err.toString());
                                telegramResponseStore.storeResponse(recipientId, "{\"ok\":false, \"error\":\"" + err.getMessage() + "\"}");
                            })
                            .onErrorResume(e -> Mono.empty());
                }, concurrency)
                .doOnSubscribe(sub -> log.info("Starting async send for {} recipients", recipientList.size()))
                .subscribe(
                        item -> log.info("Video Message added to publishing queue successfully."),
                        err -> log.error("Reactive pipeline error (shouldn't cancel others): {}", err.toString()),
                        () -> log.info("All send requests dispatched (responses may still be processing).")
                );

        return true;
    }

}
