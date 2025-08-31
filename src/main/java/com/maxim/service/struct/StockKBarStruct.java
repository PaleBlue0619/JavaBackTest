package com.maxim.service.struct;


public class StockKBarStruct {
    public String symbolCol;
    public String dateCol;
    public String timeCol; // LocalDateTime Format(DolphinDB Timestamp)
    public String openCol;
    public String highCol;
    public String lowCol;
    public String closeCol;
    public String volumeCol;

    public StockKBarStruct() {
        // 可以为空或设置默认值
    }

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

