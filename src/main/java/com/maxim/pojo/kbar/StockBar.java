package com.maxim.pojo.kbar;
import java.time.*;


public class StockBar extends KBar{
    public StockBar(String symbol, LocalDate tradeDate, LocalDateTime tradeTime,
                    Double open, Double high, Double low, Double close, Double volume){
        super(symbol, tradeDate, tradeTime, open, high, low, close, volume);
    }
}
