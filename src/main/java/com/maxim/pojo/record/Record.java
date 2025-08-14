package com.maxim.pojo.record;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Record {
    String state; // 仓位状态
    String reason; // 仓位原因
    LocalDate date;
    Integer minute;
    LocalDateTime timestamp;
    String symbol;
    Double price;
    Double vol;
    Double pnl;

    public Record(String state, String reason, LocalDate date, Integer minute, LocalDateTime timestamp, String symbol, Double price, Double vol, Double pnl) {
        this.timestamp = timestamp;
        this.state = state;
        this.reason = reason;
        this.date = date;
        this.minute = minute;
        this.symbol = symbol;
        this.price = price;
        this.vol = vol;
        this.pnl = pnl;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getMinute() {
        return minute;
    }

    public void setMinute(Integer minute) {
        this.minute = minute;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getVol() {
        return vol;
    }

    public void setVol(Double vol) {
        this.vol = vol;
    }

    public Double getPnl() {
        return pnl;
    }

    public void setPnl(Double pnl) {
        this.pnl = pnl;
    }
}
