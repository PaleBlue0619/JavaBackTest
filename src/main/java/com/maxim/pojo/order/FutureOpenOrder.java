package com.maxim.pojo.order;
import com.maxim.pojo.emun.OrderDirection;
import com.maxim.pojo.emun.OrderType;

import java.time.*;

// 订单对象
public class FutureOpenOrder extends FutureOrder {
    private final static OrderType ORDER_TYPE = OrderType.OPEN;  // 订单状态(open/close)
    public OrderDirection order_direction;   // 订单类型(long/short)
//    public Double margin_rate;  // 保证金率 // 这里已经移动到Position类中, 在executeFutureOrder的时候才会被添加, 否则如果用户下单过多而成交甚少的话性能会亏太多!
    public LocalDateTime min_timestamp;
    public LocalDateTime max_timestamp;
    public Double static_profit;
    public Double static_loss;
    public Double dynamic_profit;
    public Double dynamic_loss;
    public Double commission;

    public FutureOpenOrder(){

    }

    public FutureOpenOrder(OrderDirection direction, String symbol, Integer vol, Double price, // Double margin_rate,
                           LocalDate create_date, LocalDateTime create_timestamp,
                           LocalDateTime min_timestamp, LocalDateTime max_timestamp,
                           LocalDateTime min_order_timestamp, LocalDateTime max_order_timestamp,
                           Double static_profit, Double static_loss, Double dynamic_profit,
                           Double dynamic_loss, Double commission, String reason, Boolean partialOrder) {
        super(direction, symbol, vol, price, create_date, create_timestamp,
                min_order_timestamp, max_order_timestamp, reason);
        this.order_type = ORDER_TYPE;
        this.order_direction = direction;
//        this.margin_rate = margin_rate;
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