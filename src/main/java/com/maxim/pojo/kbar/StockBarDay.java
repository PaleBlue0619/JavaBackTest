package com.maxim.pojo.kbar;
import java.time.*;


public class StockBarDay extends KBar{
    // 添加无参构造函数
    public StockBarDay() {
    }

    public StockBarDay(String symbol, LocalDate tradeDate,
                    Double open, Double high, Double low, Double close, Integer volume){
        super(symbol, tradeDate, LocalTime.of(15,0,0), open, high, low, close, volume);
    }
}
