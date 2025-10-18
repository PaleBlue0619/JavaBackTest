package com.maxim.pojo.emun;

public enum AssetType {
    STOCK("stock"),  // 股票类资产
    FUTURE("future"), // 期货类资产
    OPTION("option"), // 期权类资产
    INDEX("index"); // 指数类资产

    private final String type;

    AssetType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
