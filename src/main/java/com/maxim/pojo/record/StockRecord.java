package com.maxim.pojo.record;
import com.maxim.pojo.emun.OrderDirection;
import com.maxim.pojo.emun.OrderType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class StockRecord extends Record{
    private final static OrderDirection direction = OrderDirection.LONG;
    public StockRecord(OrderType order_type, String reason, LocalDate date, LocalTime minute, LocalDateTime timestamp,
                       String symbol, Double price, Integer vol, Double pnl) {
        super(order_type, reason, date, minute, timestamp, symbol, price, vol, pnl);
    }
}
