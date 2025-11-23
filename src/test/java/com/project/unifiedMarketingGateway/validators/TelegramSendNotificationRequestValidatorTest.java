package com.project.unifiedMarketingGateway.validators;

import com.project.unifiedMarketingGateway.enums.MediaType;
import com.project.unifiedMarketingGateway.models.SendNotificationRequest;
import com.project.unifiedMarketingGateway.constants.Constants;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

public class TelegramSendNotificationRequestValidatorTest {

    private final TelegramSendNotificationRequestValidator validator = new TelegramSendNotificationRequestValidator();

    @Test
    public void testEmptyRecipientList() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientList(new ArrayList<>());
        request.setMediaTypeList(List.of(MediaType.TEXT));

        List<String> errors = validator.validateSendNotificationRequest(request);

        assertEquals(2, errors.size());
        assertTrue(errors.contains(Constants.ERROR_EMPTY_RECIPIENT_LIST));
        assertTrue(errors.contains(Constants.ERROR_EMPTY_TEXT_MESSAGE));
    }

    @Test
    public void testEmptyMediaTypeList() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientList(List.of("123456"));
        request.setMediaTypeList(new ArrayList<>());

        List<String> errors = validator.validateSendNotificationRequest(request);

        assertEquals(1, errors.size());
        assertTrue(errors.contains(Constants.ERROR_EMPTY_MEDIA_TYPE_LIST));
    }

    @Test
    public void testTextMediaTypeWithEmptyTextMessage() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientList(List.of("123456"));
        request.setMediaTypeList(List.of(MediaType.TEXT));
        request.setTextMessage("");

        List<String> errors = validator.validateSendNotificationRequest(request);

        assertEquals(1, errors.size());
        assertTrue(errors.contains(Constants.ERROR_EMPTY_TEXT_MESSAGE));
    }

    @Test
    public void testImageMediaTypeWithEmptyFields() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientList(List.of("123456"));
        request.setMediaTypeList(List.of(MediaType.IMAGE));
        request.setImageUrl(null);
        request.setImageCaption("");

        List<String> errors = validator.validateSendNotificationRequest(request);

        assertEquals(2, errors.size());
        assertTrue(errors.contains(Constants.ERROR_EMPTY_IMAGE_URL));
        assertTrue(errors.contains(Constants.ERROR_EMPTY_IMAGE_CAPTION));
    }

    @Test
    public void testVideoMediaTypeWithEmptyFields() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientList(List.of("123456"));
        request.setMediaTypeList(List.of(MediaType.VIDEO));
        request.setVideoUrl("");
        request.setVideoCaption(null);

        List<String> errors = validator.validateSendNotificationRequest(request);

        assertEquals(2, errors.size());
        assertTrue(errors.contains(Constants.ERROR_EMPTY_VIDEO_URL));
        assertTrue(errors.contains(Constants.ERROR_EMPTY_VIDEO_CAPTION));
    }

    @Test
    public void testMultipleMediaTypes() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientList(List.of("123456"));
        request.setMediaTypeList(List.of(MediaType.TEXT, MediaType.IMAGE));
        request.setTextMessage("");
        request.setImageUrl("");
        request.setImageCaption("");

        List<String> errors = validator.validateSendNotificationRequest(request);

        assertEquals(3, errors.size());
        assertTrue(errors.contains(Constants.ERROR_EMPTY_TEXT_MESSAGE));
        assertTrue(errors.contains(Constants.ERROR_EMPTY_IMAGE_URL));
        assertTrue(errors.contains(Constants.ERROR_EMPTY_IMAGE_CAPTION));
    }

    @Test
    public void testValidRequest() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientList(List.of("123456"));
        request.setMediaTypeList(List.of(MediaType.TEXT));
        request.setTextMessage("Hello, world!");

        List<String> errors = validator.validateSendNotificationRequest(request);

        assertTrue(errors.isEmpty());
    }
}
