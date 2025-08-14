package com.maxim.pojo.record;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class StockRecord extends Record{
    public StockRecord(String state, String reason, LocalDate date, Integer minute, LocalDateTime timestamp, String symbol, Double price, Double vol, Double pnl) {
        super(state, reason, date, minute, timestamp, symbol, price, vol, pnl);
    }
}
