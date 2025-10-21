package com.maxim.pojo.order;
import com.maxim.pojo.emun.OrderDirection;
import com.maxim.pojo.emun.OrderType;

import java.time.*;

// 订单对象
public class FutureCloseOrder extends FutureOrder {
    private static final OrderType ORDER_TYPE = OrderType.CLOSE;
    public OrderDirection order_direction;

    public FutureCloseOrder(OrderDirection direction, String symbol, Integer vol, Double price, LocalDate create_date, LocalDateTime create_timestamp,
                            LocalDateTime min_order_timestamp, LocalDateTime max_order_timestamp, String reason, Boolean partialOrder) {
        super(direction, symbol, vol, price, create_date, create_timestamp,
                min_order_timestamp, max_order_timestamp, reason);
        this.order_type = ORDER_TYPE;
        this.order_direction = direction;
        this.partialOrder = partialOrder;
    }
}
