package com.maxim.pojo.kbar;
import java.time.*;


public class StockBar extends KBar{
    // 添加无参构造函数
    public StockBar() {
    }

    public StockBar(String symbol, LocalDate tradeDate, LocalTime tradeTime,
                    Double open, Double high, Double low, Double close, Double volume){
        super(symbol, tradeDate, tradeTime, open, high, low, close, volume);
    }
}
