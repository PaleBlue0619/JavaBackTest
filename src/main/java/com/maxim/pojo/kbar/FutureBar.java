package com.maxim.pojo.kbar;
import java.time.*;

public class FutureBar extends KBar{
    public Double open_interest; // 持仓量
    public FutureBar(String symbol, LocalDate tradeDate, LocalDateTime tradeTime,
                    Double open, Double high, Double low, Double close, Double volume, Double open_interest){
        super(symbol, tradeDate, tradeTime, open, high, low, close, volume);
        this.open_interest = open_interest;
    }
}
