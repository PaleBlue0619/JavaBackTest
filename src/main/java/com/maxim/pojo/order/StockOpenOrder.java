package com.maxim.pojo.order;
import java.time.*;

// 订单对象
public class StockOpenOrder extends StockOrder {
    private final static String ORDER_STATE = "open";
    public LocalDateTime min_timestamp;
    public LocalDateTime max_timestamp;
    public Double static_profit;
    public Double static_loss;
    public Double dynamic_profit;
    public Double dynamic_loss;
    public Double commission;

    public StockOpenOrder(String symbol, Double vol, Double price,
                          LocalDate create_date, LocalDateTime create_timestamp, LocalDateTime min_timestamp, LocalDateTime max_timestamp,
                          LocalDateTime min_order_timestamp, LocalDateTime max_order_timestamp,
                          Double static_profit, Double static_loss, Double dynamic_profit,
                          Double dynamic_loss, Double commission, String reason, Boolean partialOrder) {
        super(symbol, vol, price, create_date, create_timestamp, min_order_timestamp, max_order_timestamp, reason);
        this.order_state = ORDER_STATE;
        this.partialOrder = partialOrder;
        this.min_timestamp = min_timestamp;
        this.max_timestamp = max_timestamp;
        this.static_profit = static_profit;
        this.static_loss = static_loss;
        this.dynamic_profit = dynamic_profit;
        this.dynamic_loss = dynamic_loss;
        this.commission = commission;
    }


}