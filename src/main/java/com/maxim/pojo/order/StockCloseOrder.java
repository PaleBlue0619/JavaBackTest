package com.maxim.pojo.order;
import java.time.*;

// 订单对象
public class StockCloseOrder extends StockOrder {
    private static final String ORDER_STATE = "close";

    public StockCloseOrder(String symbol, Double vol, Double price, LocalDate create_date, LocalDateTime create_timestamp,
                           LocalDateTime min_order_timestamp, LocalDateTime max_order_timestamp, String reason, Boolean partialOrder) {
        super(symbol, vol, price, create_date, create_timestamp, min_order_timestamp, max_order_timestamp, reason);
        this.order_state=ORDER_STATE;
        this.partialOrder = partialOrder;
    }
}
