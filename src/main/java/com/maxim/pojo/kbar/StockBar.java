package com.maxim.pojo.kbar;
import com.alibaba.fastjson2.annotation.JSONField;
import java.time.*;


public class StockBar extends KBar{
    // 添加无参构造函数
    public StockBar() {
    }

    public StockBar(String symbol, LocalDate tradeDate, LocalTime tradeTime,
                    Double open, Double high, Double low, Double close, Integer volume){
        super(symbol, tradeDate, tradeTime, open, high, low, close, volume);
    }
}
