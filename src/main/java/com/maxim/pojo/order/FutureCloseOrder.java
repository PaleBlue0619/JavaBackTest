package com.maxim.pojo.order;
import java.time.*;

// 订单对象
public class FutureCloseOrder extends FutureOrder {
    private static final String ORDER_STATE = "close";
    public String order_type;

    public FutureCloseOrder(String order_type, String symbol, Double vol, Double price, LocalDate create_date, LocalDateTime create_timestamp,
                           LocalDateTime min_order_timestamp, LocalDateTime max_order_timestamp, String reason, Boolean partialOrder) {
        super(order_type, symbol, vol, price, create_date, create_timestamp,
                min_order_timestamp, max_order_timestamp, reason);
        this.order_type = order_type;
        this.order_state = ORDER_STATE;
        this.partialOrder = partialOrder;
    }
}
