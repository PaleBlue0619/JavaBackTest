package com.maxim.pojo.order;
import com.maxim.pojo.emun.OrderDirection;
import com.maxim.pojo.emun.OrderType;
import java.time.*;

public class FutureOrder extends Order{
    public OrderDirection order_direction;  // long/short
    public Double margin_rate; // 期货保证金
    public Double static_profit;
    public Double static_loss;
    public Double dynamic_profit;
    public Double dynamic_loss;
    public boolean partialOrder; // 当前订单是否为子单

    public FutureOrder() {
    }

    public FutureOrder(OrderDirection order_direction, String symbol, Integer vol, Double price, LocalDate create_date, LocalDateTime create_timestamp,
                      LocalDateTime min_order_timestamp, LocalDateTime max_order_timestamp, String reason){
        super(symbol, vol, price, create_date, create_timestamp, min_order_timestamp, max_order_timestamp, reason);
        this.partialOrder = false; // 默认当前订单为完整订单
    }
}
