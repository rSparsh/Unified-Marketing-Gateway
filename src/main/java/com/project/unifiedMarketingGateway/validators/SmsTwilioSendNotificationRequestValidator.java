package com.project.unifiedMarketingGateway.validators;

import com.project.unifiedMarketingGateway.enums.MediaType;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.project.unifiedMarketingGateway.constants.Constants.*;

@Service
public class SmsTwilioSendNotificationRequestValidator implements SendNotificationRequestValidator{

    @Override
    public List<String> validateSendNotificationRequest(SendNotificationRequest request)
    {
        List<String> validationErrorList = new ArrayList<>();

        if(request.getRecipientList().isEmpty())
            validationErrorList.add(ERROR_EMPTY_RECIPIENT_LIST);

        if(request.getRecipientList().size() > 100)
            validationErrorList.add(MAX_RECIPIENT_LIST_SIZE);

        List<MediaType> mediaTypeList = request.getMediaTypeList();

        if(mediaTypeList.isEmpty())
            validationErrorList.add(ERROR_EMPTY_MEDIA_TYPE_LIST);
        mediaTypeList.stream().forEach(
                mediaType -> {
                    switch (mediaType){
                        case TEXT:
                            if(StringUtils.isEmpty(request.getTextMessage()))
                                validationErrorList.add(ERROR_EMPTY_TEXT_MESSAGE);
                            if(request.getTextMessage().length() > 1600)
                                validationErrorList.add(ERROR_TEXT_MESSAGE_SIZE_LIMIT);
                            break;
                        default: validationErrorList.add(ERROR_INVALID_MEDIA_TYPE_LIST_FOR_SMS);
                    }
                }
        );
        return validationErrorList;
    }
}
