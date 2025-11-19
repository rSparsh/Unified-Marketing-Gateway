package com.project.unifiedMarketingGateway.connector;

public interface ConnectorInterface {

    public int sendText(String recipientId, String message);

    public int sendImage(String recipientId, String imageUrl, String imageCaption);

    public int sendVideo(String recipientId, String videoUrl, String videoCaption);
}
