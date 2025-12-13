package com.project.unifiedMarketingGateway.enums;

public enum ClientType {
    TELEGRAM("Telegram"),
    WHATSAPP("Whatsapp"),
    SMS("SMS");

    private final String value;

    ClientType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
