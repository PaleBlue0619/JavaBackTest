package com.maxim.pojo.position;
import java.time.LocalDateTime;

public class Position {
    public Integer vol;
    public Double price;
    public LocalDateTime min_timestamp;
    public LocalDateTime max_timestamp;

    public Position(Double price, Integer vol, LocalDateTime min_timestamp, LocalDateTime max_timestamp) {
        this.price = price;
        this.vol = vol;
        this.min_timestamp = min_timestamp;
        this.max_timestamp = max_timestamp;
    }
}