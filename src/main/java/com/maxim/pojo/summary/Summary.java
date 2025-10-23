package com.maxim.pojo.summary;
import java.time.LocalDateTime;

public class Summary{
    public Double ori_price;
    public Integer total_vol;
    public Double static_profit;
    public Double static_loss;
    public Double dynamic_profit;
    public Double dynamic_loss;

    public Summary(){

    }

    public Summary(Double ori_price, Integer total_vol, Double static_profit,
                   Double static_loss, Double dynamic_profit,
                   Double dynamic_loss) {
        this.static_profit = static_profit;
        this.ori_price = ori_price; // 初始价格 -> 后续会变成持仓买入均价
        this.total_vol = total_vol;
        this.static_loss = static_loss;
        this.dynamic_profit = dynamic_profit;
        this.dynamic_loss = dynamic_loss;
    }

}