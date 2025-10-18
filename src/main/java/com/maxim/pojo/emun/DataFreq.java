package com.maxim.pojo.emun;

public enum DataFreq {
    MINUTE("Min"),
    DAY("Day");

    private final String value;

    DataFreq(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }

}