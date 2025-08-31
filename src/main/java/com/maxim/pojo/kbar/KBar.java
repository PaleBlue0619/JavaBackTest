package com.maxim.pojo.kbar;
import java.time.*;

public class KBar {
    public String symbol;
    public LocalDate tradeDate;
    public LocalTime tradeTime;
    public Double open;
    public Double high;
    public Double low;
    public Double close;
    public Double volume;

    public KBar() {
    }

    public KBar(String symbol, LocalDate tradeDate, LocalTime tradeTime,
                Double open, Double high, Double low, Double close, Double volume) {
        this.low = low;
        this.symbol = symbol;
        this.tradeDate = tradeDate;
        this.tradeTime = tradeTime;
        this.open = open;
        this.high = high;
        this.close = close;
        this.volume = volume;
    }
}
