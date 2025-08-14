package com.maxim.pojo.summary;
import java.time.LocalDateTime;

public class StockSummary extends Summary{
    public Integer dynamic_monitor;
    public Integer static_monitor;
    public StockSummary(Double ori_price, Double total_vol, Double static_profit, Double static_loss, Double dynamic_profit,
                        Double dynamic_loss, Double history_min, Double history_max){
        super(ori_price, total_vol, static_profit, static_loss, dynamic_profit, dynamic_loss, history_min, history_max);
        this.dynamic_monitor = 0;
        this.static_monitor = 0;
    }
    public StockSummary update(StockSummary sum, Double price, Double vol, Double static_profit, Double static_loss, Double dynamic_profit, Double dynamic_loss){
        // 更新基本属性
        sum.static_profit = static_profit;
        sum.static_loss = static_loss;
        sum.dynamic_profit = dynamic_profit;
        sum.dynamic_loss = dynamic_loss;

        // 更新vol & ori_price(持仓买入均价)
        Double ori_price = sum.ori_price;
        Double vol0 = sum.total_vol;
        double amount0 = ori_price * total_vol;
        Double vol1 = vol0 + vol;
        Double amount1 = amount0 + price * vol;

        // 赋值回sum
        sum.total_vol = vol1;
        sum.ori_price = amount1 / vol1;
        return sum;
    }

    public Integer getDynamic_monitor() {
        return dynamic_monitor;
    }

    public void setDynamic_monitor(Integer dynamic_monitor) {
        this.dynamic_monitor = dynamic_monitor;
    }

    public Integer getStatic_monitor() {
        return static_monitor;
    }

    public void setStatic_monitor(Integer static_monitor) {
        this.static_monitor = static_monitor;
    }
}
