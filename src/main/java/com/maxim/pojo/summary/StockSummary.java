package com.maxim.pojo.summary;
import java.time.LocalDateTime;

public class StockSummary extends Summary{
    public Integer dynamic_monitor;
    public Integer static_monitor;
    public Double profit;   // 平仓损益
    public Double realTimeProfit;  // 当前实时损益
    public Double realTimePrice; // 当前实时价格

    public StockSummary(Double ori_price, Integer total_vol, Double static_profit, Double static_loss, Double dynamic_profit,
                        Double dynamic_loss){
        super(ori_price, total_vol, static_profit, static_loss, dynamic_profit, dynamic_loss);
        this.dynamic_monitor = 0;
        this.static_monitor = 0;
        this.profit = 0.0;  // 第一个Bar持仓的时候一定还没有利润产生
        this.realTimeProfit = 0.0;
    }

    // 开仓时回调函数
    public void openUpdate(Double price, Integer vol, Double static_profit, Double static_loss, Double dynamic_profit, Double dynamic_loss){
        /*
        更新基本属性
        以price买入vol后, summary的变动
         */
        this.static_profit = static_profit;
        this.static_loss = static_loss;
        this.dynamic_profit = dynamic_profit;
        this.dynamic_loss = dynamic_loss;

        // 更新vol & ori_price(持仓买入均价)
        Double ori_price = this.ori_price;
        Integer vol0 = this.total_vol;
        double amount0 = ori_price * total_vol;
        Integer vol1 = vol0 + vol;
        Double amount1 = amount0 + price * vol;

        // 赋值回sum
        this.total_vol = vol1;
        this.ori_price = amount1 / vol1;
    }


    // 部分平仓时回调函数 [因为全部平仓会直接删除这个仓位]
    public void closeUpdate(Double price, Integer vol){
        /*
        更新基本属性
        以price卖出vol后, summary的变动
         */
        // 因为卖出后, 均价是不会发生变化的, 所以只需要更新vol的属性即可
        this.total_vol -= vol;

        // 计算盈亏
        this.profit += (price - this.ori_price) * vol;
    }

    public void onBarUpdate(Double price){
        // 每个K线到来的时候执行该方法, 更新持仓的price & 最新利润
        this.realTimePrice = price;
        this.realTimeProfit = (price - this.ori_price) * this.total_vol;  // 更新最新利润
    }
}