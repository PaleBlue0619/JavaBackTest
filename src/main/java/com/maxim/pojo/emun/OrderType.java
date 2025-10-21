package com.maxim.pojo.emun;

public enum OrderType {
    OPEN("Execute Order"), // 开仓
    CLOSE("Close Order"); // 平仓

    private final String value;
    OrderType(String value){
        this.value = value;
    }
    public String getValue(){
        return value;
    }
}
