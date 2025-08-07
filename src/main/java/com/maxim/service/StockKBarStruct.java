package com.maxim.service;


public class StockKBarStruct {
    protected String symbolCol;
    protected String dateCol;
    protected String timeCol; // LocalDateTime Format(DolphinDB Timestamp)
    protected String openCol;
    protected String highCol;
    protected String lowCol;
    protected String closeCol;
    protected String volumeCol;

    // 构造方式一: date + time
    public StockKBarStruct(String symbolCol, String dateCol, String timeCol, String openCol, String highCol, String lowCol, String closeCol, String volumeCol) {
        this.symbolCol = symbolCol;
        this.dateCol = dateCol;
        this.timeCol = timeCol;
        this.openCol = openCol;
        this.highCol = highCol;
        this.lowCol = lowCol;
        this.closeCol = closeCol;
        this.volumeCol = volumeCol;
    }

//    // 构造方式二：仅 time
//    public StockKBarStruct(String symbolCol, String timeCol, String openCol, String highCol, String lowCol, String closeCol, String volumeCol) {
//        this.symbolCol = symbolCol;
//        this.timeCol = timeCol;
//        this.openCol = openCol;
//        this.highCol = highCol;
//        this.lowCol = lowCol;
//        this.closeCol = closeCol;
//        this.volumeCol = volumeCol;
//    }
}

