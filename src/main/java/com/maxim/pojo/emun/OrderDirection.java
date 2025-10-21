package com.maxim.pojo.emun;

public enum OrderDirection {
    LONG("Long Order"),
    SHORT("Short Order");

    private final String value;
    OrderDirection(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
