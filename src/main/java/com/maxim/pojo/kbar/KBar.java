package com.maxim.pojo.kbar;
import com.alibaba.fastjson2.annotation.JSONField;

import java.time.*;

public class KBar {
    public String symbol;
    public LocalDate tradeDate;
    public LocalTime tradeTime;
    public Double open;
    public Double high;
    public Double low;
    public Double close;
    public Integer volume;

    public KBar() {
    }

    public KBar(String symbol, LocalDate tradeDate, LocalTime tradeTime,
                Double open, Double high, Double low, Double close, Integer volume) {
        this.low = low;
        this.symbol = symbol;
        this.tradeDate = tradeDate;
        this.tradeTime = tradeTime;
        this.open = open;
        this.high = high;
        this.close = close;
        this.volume = volume;
    }

    public LocalTime getTradeTime() {
        return tradeTime;
    }
}
