package com.maxim.pojo.position;
import java.time.LocalDateTime;

public class Position {
    public Double vol;
    public Double price;
    public LocalDateTime min_timestamp;
    public LocalDateTime max_timestamp;
    public Integer time_monitor;

    public Position(Double price, Double vol, LocalDateTime min_timestamp, LocalDateTime max_timestamp, Integer time_monitor) {
        this.price = price;
        this.vol = vol;
        this.min_timestamp = min_timestamp;
        this.max_timestamp = max_timestamp;
        this.time_monitor = time_monitor;
    }

    public Double getVol() {
        return vol;
    }

    public void setVol(Double vol) {
        this.vol = vol;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public LocalDateTime getMin_timestamp() {
        return min_timestamp;
    }

    public void setMin_timestamp(LocalDateTime min_timestamp) {
        this.min_timestamp = min_timestamp;
    }

    public LocalDateTime getMax_timestamp() {
        return max_timestamp;
    }

    public void setMax_timestamp(LocalDateTime max_timestamp) {
        this.max_timestamp = max_timestamp;
    }

    public Integer getTime_monitor() {
        return time_monitor;
    }

    public void setTime_monitor(Integer time_monitor) {
        this.time_monitor = time_monitor;
    }
}