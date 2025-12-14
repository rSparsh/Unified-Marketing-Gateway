package com.project.unifiedMarketingGateway.processor.sms;

import com.project.unifiedMarketingGateway.builders.SendNotificationResponseBuilder;
import com.project.unifiedMarketingGateway.connectors.TwilioSmsConnector;
import com.project.unifiedMarketingGateway.contexts.SendContext;
import com.project.unifiedMarketingGateway.dto.SendResultDTO;
import com.project.unifiedMarketingGateway.processor.IdempotencyService;
import com.project.unifiedMarketingGateway.store.messageStore.SmsMessageStore;
import com.twilio.rest.api.v2010.account.Message;
import com.project.unifiedMarketingGateway.metrics.MetricsService;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.models.SendNotificationResponse;
import com.project.unifiedMarketingGateway.processor.RequestProcessorInterface;
import com.project.unifiedMarketingGateway.retryHandler.SmsReactiveRetryHandler;
import com.project.unifiedMarketingGateway.validators.SmsTwilioSendNotificationRequestValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.project.unifiedMarketingGateway.constants.Constants.TEXT_MEDIA_DISABLED_ERROR;
import static com.project.unifiedMarketingGateway.enums.ClientType.SMS;
import static com.project.unifiedMarketingGateway.enums.MediaType.TEXT;

@Slf4j
@Service
public class SmsRequestProcessor implements RequestProcessorInterface {

    @Autowired TwilioSmsConnector smsConnector;
    @Autowired MetricsService metricsService;
    @Autowired
    SmsTwilioSendNotificationRequestValidator requestValidator;
    @Autowired
    SendNotificationResponseBuilder responseBuilder;
    @Autowired
    SmsReactiveRetryHandler reactiveRetryHandler;
    @Autowired
    SmsMessageStore smsMessageStore;
    @Autowired
    IdempotencyService idempotencyService;

    @Value("${sms.maxConcurrency:5}")
    private int maxConcurrency;
    @Value("${sms.contentBasedResource.text.isEnabled:false}")
    private boolean isTextEnabled;

    @Override
    public SendNotificationResponse processNotificationRequest(SendNotificationRequest sendNotificationRequest) {

        List<String> validationErrorList = requestValidator.validateSendNotificationRequest(sendNotificationRequest);
        if (!validationErrorList.isEmpty()) {
            return responseBuilder.buildFailureResponse("Request Validation Failed: " + validationErrorList.toString());
        }

        List<String> recipientList = sendNotificationRequest.getRecipientList();
        String textMessage = sendNotificationRequest.getTextMessage();

        boolean allQueued = false;
        List<String> mediaDisabledErrorList = new ArrayList<>();

        if(isTextEnabled)
            allQueued = prepareAndSendMedia(recipientList, textMessage);
        else
            mediaDisabledErrorList.add(TEXT_MEDIA_DISABLED_ERROR);

        if (allQueued) {
            return responseBuilder.buildSuccessResponse("Notification request added to queue successfully");
        } else {
            return responseBuilder.buildFailureResponse("Notification request couldn't be processed." + mediaDisabledErrorList.toString());
        }
    }

    private boolean prepareAndSendMedia(List<String> recipientList, String textMessage) {
        List<String> validRecipients = recipientList.stream()
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList();

        if (validRecipients.isEmpty()) {
            return false;
        }

        int concurrency = Math.min(maxConcurrency, validRecipients.size());

        Flux.fromIterable(validRecipients)
                .flatMap(chatId -> executeRequestReactive(chatId, textMessage), concurrency)
                .doOnSubscribe(s -> log.info(
                        "Dispatching {} SMS sends (concurrency={})",
                        validRecipients.size(), concurrency))
                .subscribe(
                        result -> {
                            if (result.isSuccess()) {
                                log.info("[{}] SMS sent successfully", result.getChatId());
                            } else {
                                log.warn("[{}] SMS failed: {}", result.getChatId(), result.getErrorMessage());
                            }
                        },
                        err -> log.error("SMS pipeline error: {}", err.toString()),
                        () -> log.info("All SMS send requests dispatched")
                );

        return true;
    }

    private Mono<SendResultDTO> executeRequestReactive(String chatId, String textMessage) {
        SendContext ctx = SendContext.builder()
                .channel(SMS.getValue())
                .method(TEXT.getValue())
                .recipient(chatId)
                .requestId(UUID.randomUUID().toString())
                .build();

        if (!idempotencyService.tryStart(
                ctx.getRequestId(),
                ctx.getChannel(),
                ctx.getRecipient(),
                ctx.getMethod()
        )) {
            log.info("[{}] Duplicate request blocked", ctx.getRequestId());
            return Mono.just(
                    new SendResultDTO(
                            ctx.getRecipient(),
                            true,
                            "DUPLICATE",
                            null
                    )
            );
        }

        metricsService.incrementSendAttempt(ctx.getChannel(), "TEXT");
        metricsService.incrementInFlight(ctx.getChannel());
        smsMessageStore.storeQueued(chatId);
        ctx.markStart();

        Mono<String> smsMono =
                reactiveRetryHandler.withRetry(() ->
                        Mono.fromCallable(() ->
                                smsConnector.sendSms(ctx.getRecipient(), textMessage)
                        ).map(Message::getSid)
                );

        return smsMono
                .subscribeOn(Schedulers.boundedElastic())
                .map(sid -> {
                    recordSuccess(ctx, sid);
                    return new SendResultDTO(
                            ctx.getRecipient(),
                            true,
                            sid,
                            null
                    );
                })
                .onErrorResume(err -> {
                    recordFailure(ctx, err.getMessage());
                    return Mono.just(
                            new SendResultDTO(
                                    ctx.getRecipient(),
                                    false,
                                    null,
                                    err.getMessage()
                            )
                    );
                });
    }

    private void recordSuccess(SendContext ctx, String sid) {
        idempotencyService.markCompleted(
                ctx.getRequestId(),
                ctx.getChannel(),
                ctx.getRecipient(),
                ctx.getMethod()
        );

        try {
            smsMessageStore.storeSent(sid, ctx.getRecipient());
        } catch (Exception e) {
            log.warn("[{}] failed to persist success response: {}", ctx.getRecipient(), e.toString());
        }

        // metrics
        metricsService.incrementSendSuccess(ctx.getChannel(), ctx.getMethod());
        metricsService.recordHttpLatency(ctx.getChannel(), ctx.getMethod(), ctx.elapsed());
        metricsService.decrementInFlight(ctx.getChannel());
    }

    private void recordFailure(SendContext ctx, String errorMessage) {
        idempotencyService.markFailed(
                ctx.getRequestId(),
                ctx.getChannel(),
                ctx.getRecipient(),
                ctx.getMethod()
        );

        try {
            smsMessageStore.storeFailed(ctx.getRecipient(), errorMessage);
        } catch (Exception e) {
            log.warn("[{}] failed to persist error response: {}", ctx.getRecipient(), e.toString());
        }

        // metrics
        metricsService.incrementSendFailure(ctx.getChannel(), ctx.getMethod(), errorMessage);
        metricsService.recordHttpLatency(ctx.getChannel(), ctx.getMethod(), ctx.elapsed());
        metricsService.decrementInFlight(ctx.getChannel());
    }
}

