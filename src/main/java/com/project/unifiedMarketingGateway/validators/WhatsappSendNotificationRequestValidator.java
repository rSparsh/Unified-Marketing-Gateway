package com.project.unifiedMarketingGateway.validators;

import com.project.unifiedMarketingGateway.enums.MediaType;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.project.unifiedMarketingGateway.constants.Constants.*;

@Service
public class WhatsappSendNotificationRequestValidator implements SendNotificationRequestValidator{

    @Override
    public List<String> validateSendNotificationRequest(SendNotificationRequest request)
    {
        List<String> validationErrorList = new ArrayList<>();

        if(request.getRecipientList().isEmpty())
            validationErrorList.add(ERROR_EMPTY_RECIPIENT_LIST);

        if(request.getRecipientList().size() > 100)
            validationErrorList.add(ERROR_EMPTY_RECIPIENT_LIST);

        List<MediaType> mediaTypeList = request.getMediaTypeList();

        if(mediaTypeList.isEmpty())
            validationErrorList.add(ERROR_EMPTY_MEDIA_TYPE_LIST);
        mediaTypeList.stream().forEach(
                mediaType -> {
                    switch (mediaType){
                        case TEXT:
                            if(StringUtils.isEmpty(request.getTextMessage()))
                                validationErrorList.add(ERROR_EMPTY_TEXT_MESSAGE);
                            break;
                        case IMAGE:
                            if(StringUtils.isEmpty(request.getImageUrl()))
                                validationErrorList.add(ERROR_EMPTY_IMAGE_URL);
                            if(StringUtils.isEmpty(request.getImageCaption()))
                                validationErrorList.add(ERROR_EMPTY_IMAGE_CAPTION);
                            break;
                        case VIDEO:
                            if(StringUtils.isEmpty(request.getVideoUrl()))
                                validationErrorList.add(ERROR_EMPTY_VIDEO_URL);
                            if(StringUtils.isEmpty(request.getVideoCaption()))
                                validationErrorList.add(ERROR_EMPTY_VIDEO_CAPTION);
                            break;
                    }
                }
        );
        return validationErrorList;
    }
}
