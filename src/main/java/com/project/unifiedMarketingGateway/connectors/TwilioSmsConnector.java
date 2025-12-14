package com.project.unifiedMarketingGateway.connectors;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TwilioSmsConnector {

    private final String fromNumber;

    public TwilioSmsConnector(
            @Value("${sms.twilio.accountSid:PLEASE_SET_SECRETS}") String accountSid,
            @Value("${sms.twilio.authToken:PLEASE_SET_SECRETS}") String authToken,
            @Value("${sms.twilio.fromNumber:PLEASE_SET_SECRETS}") String fromNumber
    ) {
        Twilio.init(accountSid, authToken);
        this.fromNumber = fromNumber;
    }

    public Message sendSms(String to, String text) {
        return Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(fromNumber),
                text
        ).create();
    }
}