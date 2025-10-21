package com.maxim.pojo.order;
import com.maxim.pojo.emun.OrderType;
import java.time.*;

// 订单对象
public class StockCloseOrder extends StockOrder {
    private static final OrderType ORDER_TYPE = OrderType.CLOSE;

    public StockCloseOrder(String symbol, Integer vol, Double price, LocalDate create_date, LocalDateTime create_timestamp,
                           LocalDateTime min_order_timestamp, LocalDateTime max_order_timestamp, String reason, Boolean partialOrder) {
        super(symbol, vol, price, create_date, create_timestamp, min_order_timestamp, max_order_timestamp, reason);
        this.order_type = ORDER_TYPE;
        this.partialOrder = partialOrder;
    }
}
