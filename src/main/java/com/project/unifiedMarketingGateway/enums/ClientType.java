package com.project.unifiedMarketingGateway.enums;

public enum ClientType {
    TELEGRAM("Telegram");

    private final String value;

    ClientType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
