package com.project.unifiedMarketingGateway.processor.telegram;

import com.project.unifiedMarketingGateway.connector.TelegramHttpConnector;
import com.project.unifiedMarketingGateway.enums.MediaType;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import com.project.unifiedMarketingGateway.processor.RequestProcessorInterface;
import com.project.unifiedMarketingGateway.responseStore.TelegramResponseStore;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.project.unifiedMarketingGateway.constants.Constants.CHAT_ID;
import static com.project.unifiedMarketingGateway.enums.MediaType.TEXT;

@Slf4j
@Service
public class TelegramRequestProcessor implements RequestProcessorInterface {

    @Autowired
    TelegramHttpConnector telegramHttpConnector;

    @Autowired
    TelegramResponseStore telegramResponseStore;

    private final int concurrency = 8;

    @Override
    public SendNotificationResponse processNotificationRequest(@NonNull SendNotificationRequest sendNotificationRequest) {
        List<String> recipientList = sendNotificationRequest.getRecipientList();
        List<MediaType> mediaTypeList = sendNotificationRequest.getMediaTypeList();

        String textMessage = sendNotificationRequest.getTextMessage();
        AtomicBoolean operationStatus = new AtomicBoolean(true);

        mediaTypeList.stream().forEach(
                mediaType -> {
                    switch(mediaType){
                        case TEXT:
                            boolean ok = prepareAndSendTextMedia(recipientList, textMessage);
                            operationStatus.set(operationStatus.get() && ok); break;
                    }
                }
        );


        SendNotificationResponse response = SendNotificationResponse.builder()
                .responseStatus("200")
                .customMessage("Notification sent")
                .build();

        return response;
    }

    private boolean prepareAndSendTextMedia(List<String> recipientList, String textMessage)
    {
        if (recipientList == null || recipientList.isEmpty()) {
            log.warn("No recipients to send to");
            return false;
        }

        // Build the Flux (pipeline not yet subscribed)
        Flux.fromIterable(recipientList)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(id -> Map.<String, Object>of(CHAT_ID, id, String.valueOf(TEXT).toLowerCase(), textMessage == null ? "" : textMessage))
                .flatMap(payload -> {
                    String recipientId = String.valueOf(payload.get(CHAT_ID));
                    // postToTelegram returns Mono<String>
                    return telegramHttpConnector.sendMarketingRequest("sendMessage", payload)
                            .doOnNext(resp -> {
                                // store response in-memory
                                telegramResponseStore.storeResponse(recipientId, resp);
                                // optionally persist async
                                telegramResponseStore.persistResponseAsync(recipientId, resp);
                            })
                            .doOnError(err -> {
                                log.error("Error sending to {} : {}", recipientId, err.toString());
                                // you may store failure marker
                                telegramResponseStore.storeResponse(recipientId, "{\"ok\":false, \"error\":\"" + err.getMessage() + "\"}");
                            })
                            // swallow error to allow other recipients to proceed
                            .onErrorResume(e -> Mono.empty());
                }, concurrency)
                // final doOnSubscribe for logging when pipeline starts
                .doOnSubscribe(sub -> log.info("Starting async send for {} recipients", recipientList.size()))
                // subscribe starts the pipeline (fire-and-forget)
                .subscribe(
                        item -> { /* items handled in doOnNext above */ },
                        err -> log.error("Reactive pipeline error (shouldn't cancel others): {}", err.toString()),
                        () -> log.info("All send requests dispatched (responses may still be processing).")
                );

        return true;
    }

}
