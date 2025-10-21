package com.maxim.pojo.kbar;
import java.time.*;

public class FutureBar extends KBar{
    public Double pre_settle;
    public Double open_interest; // 持仓量
    // 添加无参构造函数
    public FutureBar() {
    }

    public FutureBar(String symbol, LocalDate tradeDate, LocalTime tradeTime,
                    Double open, Double high, Double low, Double close, Integer volume, Double pre_settle, Double open_interest){
        super(symbol, tradeDate, tradeTime, open, high, low, close, volume);
        this.pre_settle = pre_settle;
        this.open_interest = open_interest;
    }


}
