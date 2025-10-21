package com.maxim.pojo.kbar;
import java.time.*;

public class FutureBarDay extends KBar{
    public Double pre_settle; // 昨日结算价
    public Integer open_interest; // 持仓量
    // 添加无参构造函数
    public FutureBarDay() {
    }

    public FutureBarDay(String symbol, LocalDate tradeDate,
                    Double open, Double high, Double low, Double close, Integer volume, Double pre_settle, Integer open_interest){
        super(symbol, tradeDate, LocalTime.of(15,0,0), open, high, low, close, volume);
        this.pre_settle = pre_settle;
        this.open_interest = open_interest;
    }
}
