package com.maxim.pojo.record;
import com.maxim.pojo.emun.OrderDirection;
import com.maxim.pojo.emun.OrderType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class FutureRecord extends Record{
    public OrderDirection direction;  // 多单(long)/空单(short)

    public FutureRecord(){

    }

    public FutureRecord(OrderType order_type, OrderDirection direction, String reason, LocalDate date, LocalTime minute, LocalDateTime timestamp,
                        String symbol, Double price, Integer vol, Double pnl) {
        super(order_type, reason, date, minute, timestamp, symbol, price, vol, pnl);
        this.direction = direction;
    }
}
