package com.project.unifiedMarketingGateway.enums;

public enum MediaType {
    TEXT("TEXT"),
    IMAGE("IMAGE"),
    VIDEO("VIDEO");

    private final String value;

    MediaType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
