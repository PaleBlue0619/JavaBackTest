package com.maxim.pojo.position;
import java.time.LocalDateTime;

public class StockPosition extends Position{
    public StockPosition(Double price, Double vol, LocalDateTime min_timestamp, LocalDateTime max_timestamp, Integer time_monitor) {
        super(price, vol, min_timestamp, max_timestamp, time_monitor);
    }
}
