package com.maxim.pojo.position;

import java.time.LocalDateTime;

public class FuturePosition extends Position{
    public Double profit; // 持仓利润
    public Double margin; // 保证金金额
    public Double margin_rate; // 保证金比率
    public Integer hold_days; // 用于平仓时计算收益
    public Double pre_price;  // 上一个K线的价格
    public Integer time_monitor;
    public Integer static_monitor;
    public Double static_profit;
    public Double static_loss;
    public Integer dynamic_monitor;
    public Double dynamic_profit;
    public Double dynamic_loss;
    public Double history_max;
    public Double history_min;

    public FuturePosition(Double price, Double vol, Double margin_rate, LocalDateTime min_timestamp, LocalDateTime max_timestamp) {
        super(price, vol, min_timestamp, max_timestamp);
        this.profit = 0.0;  // 一开始买入的时候利润一定为0.0
        this.margin_rate = margin_rate;
        this.margin = margin_rate * vol * price;  // TODO: 增加一个参数,使得能够根据这个参数选择对应的保证金计算方式
        this.hold_days = 0;
        this.pre_price = price; // 创建的时候就是开仓价， 后续在afterBar的中会更新该属性
        this.time_monitor = 0;
        this.static_monitor = 0;
        this.dynamic_monitor = 0;
        this.history_max = price;
        this.history_min = price;
    }

    // K线到来时触发
    public Double onBarLongUpdate(Double price){
        // 更新历史最高价&最低价
        this.history_max = Math.max(this.history_max, price);
        this.history_min = Math.min(this.history_min, price);

        // 更新当前仓位的利润
        double realTimeProfit = (price - this.pre_price) * this.vol * 1; // 这个K线上的pnl
        this.profit += realTimeProfit;

        // 更新当前仓位的pre_price为price
        this.pre_price = price;
        return realTimeProfit;
    }

    public Double onBarShortUpdate(Double price){
        // 更新历史最高价&最低价
        this.history_max = Math.max(this.history_max, price);
        this.history_min = Math.min(this.history_min, price);

        // 更新当前仓位的利润
        double realTimeProfit = (price - this.pre_price) * this.vol * -1; // 这个K线上的pnl
        this.profit += realTimeProfit;

        // 更新当前仓位的pre_price为price
        this.pre_price = price;
        return realTimeProfit;
    }
}
