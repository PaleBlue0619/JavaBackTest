package com.maxim.pojo.info;
import com.maxim.pojo.info.Info;
import java.time.*;

public class FutureInfo extends Info{
    public Double pre_settle;  // 前结算价
    public Double settle; // 结算价
    public Double margin_rate; // 保证金率

    public FutureInfo(){}

    public FutureInfo(LocalDate tradeDate, String symbol, Double open, Double high, Double low, Double close,
                      Double pre_settle, Double settle, Double margin_rate,  // 新增字段
                     LocalDate start_date, LocalDate end_date){
        super(tradeDate, symbol, open, high, low, close, start_date, end_date);
        this.pre_settle = pre_settle;
        this.settle = settle;
        this.margin_rate = margin_rate;
    }
}
