package com.maxim.pojo.order;
import com.maxim.pojo.emun.OrderType;

import java.time.*;

public class Order{
    public OrderType order_type;  // 要么开仓要么平仓
    public String symbol; // 标的名称
    public Integer vol;
    public Double price;
    public LocalDate create_date;
    public LocalDateTime create_timestamp;
    public LocalDateTime min_order_timestamp;
    public LocalDateTime max_order_timestamp;
    public String reason;

    public Order() {
    }

    public Order(String symbol, Integer vol, Double price, LocalDate create_date, LocalDateTime create_timestamp, LocalDateTime min_order_timestamp, LocalDateTime max_order_timestamp, String reason) {
        this.symbol = symbol;
        this.vol = vol;
        this.price = price;
        this.create_date = create_date;
        this.create_timestamp = create_timestamp;
        this.min_order_timestamp = min_order_timestamp;
        this.max_order_timestamp = max_order_timestamp;
        this.reason = reason;
    }
}