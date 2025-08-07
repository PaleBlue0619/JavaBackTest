package com.maxim.pojo.info;
import java.time.*;

public class Info{
    public String symbol;
    public LocalDate tradeDate;
    public Double open;
    public Double high;
    public Double low;
    public Double close;
    public LocalDate start_date;
    public LocalDate end_date;

    public Info(LocalDate tradeDate, String symbol, Double open, Double high, Double low, Double close,
                LocalDate start_date, LocalDate end_date) {
        this.tradeDate = tradeDate;
        this.symbol = symbol;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.start_date = start_date;
        this.end_date = end_date;
    }
}