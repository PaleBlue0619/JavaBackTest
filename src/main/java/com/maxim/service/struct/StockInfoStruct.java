package com.maxim.service.struct;

public class StockInfoStruct{
    public String symbolCol;
    public String dateCol;
    public String openCol;
    public String highCol;
    public String lowCol;
    public String closeCol;

    // 构造方式一: date + time
    public StockInfoStruct(String symbolCol, String dateCol, String openCol, String highCol, String lowCol, String closeCol) {
        this.symbolCol = symbolCol;
        this.dateCol = dateCol;
        this.openCol = openCol;
        this.highCol = highCol;
        this.lowCol = lowCol;
        this.closeCol = closeCol;
    }

    // TODO: 后续需要添加是否能够成交的字段(Integer state), 同时在processStockOrder中添加相关的预处理逻辑, 减少订单判断次数
}
