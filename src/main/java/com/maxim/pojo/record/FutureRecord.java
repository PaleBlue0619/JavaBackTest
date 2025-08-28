package com.maxim.pojo.record;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FutureRecord extends Record{
    public String order_type;  // 多单(long)/空单(short)
    public FutureRecord(String state, String order_type, String reason, LocalDate date, Integer minute, LocalDateTime timestamp,
                        String symbol, Double price, Double vol, Double pnl) {
        super(state, reason, date, minute, timestamp, symbol, price, vol, pnl);
        this.order_type = order_type;
    }
}
