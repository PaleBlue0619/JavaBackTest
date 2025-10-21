package com.maxim.pojo.record;
import com.maxim.pojo.emun.OrderType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Record {
    OrderType order_type; // 仓位状态
    String reason; // 仓位原因
    LocalDate date;
    LocalTime minute;
    LocalDateTime timestamp;
    String symbol;
    Double price;
    Integer vol;
    Double pnl;

    public Record(OrderType order_type, String reason, LocalDate date, LocalTime minute, LocalDateTime timestamp, String symbol, Double price, Integer vol, Double pnl) {
        this.timestamp = timestamp;
        this.order_type = order_type;
        this.reason = reason;
        this.date = date;
        this.minute = minute;
        this.symbol = symbol;
        this.price = price;
        this.vol = vol;
        this.pnl = pnl;
    }
}
