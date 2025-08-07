package com.maxim.pojo.info;
import com.maxim.pojo.info.Info;
import java.time.*;

public class StockInfo extends Info {
    public StockInfo(LocalDate tradeDate, String symbol, Double open, Double high, Double low, Double close,
                     LocalDate start_date, LocalDate end_date){
        super(tradeDate, symbol, open, high, low, close, start_date, end_date);
    }
}