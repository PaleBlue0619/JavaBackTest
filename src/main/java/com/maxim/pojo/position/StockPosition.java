package com.maxim.pojo.position;
import java.time.LocalDateTime;

public class StockPosition extends Position {
    public Double profit; // 持仓利润
    public Double pre_price;  // 上一个K线的价格, 不会在创建K线的时候赋值, 而会在monitor的时候赋值
    public Integer time_monitor;
    public Integer static_monitor;  // 静态止盈止损, 当且仅当当前仓位在队列第一个元素会据此进行平仓
    public Double static_profit; // 静态止盈比例
    public Double static_loss; // 静态止损比例
    public Integer dynamic_monitor;  // 动态止盈止损, 当且仅当当前仓位在队列第一个元素会据此进行平仓
    public Double dynamic_profit; // 动态止盈比例
    public Double dynamic_loss; // 动态止损比例
    public Double history_max;
    public Double history_min;

    public StockPosition(Double price, Double vol,
                         LocalDateTime min_timestamp, LocalDateTime max_timestamp,
                         Double static_profit, Double static_loss,
                         Double dynamic_profit, Double dynamic_loss) {
        super(price, vol, min_timestamp, max_timestamp);
        this.pre_price = price;
        this.profit = 0.0;  // 一开始买入的利润一定为0
        this.time_monitor = 0; // 初始化是否需要监控时间为0,表示还不知道,需要等到该仓位该天第一次被监控时进行判断
        this.static_monitor = 0;
        this.static_profit = static_profit;
        this.static_loss = static_loss;
        this.dynamic_monitor = 0;
        this.dynamic_profit = dynamic_profit;
        this.dynamic_loss = dynamic_loss;
        this.history_max = price;
        this.history_min = price;
    }

    // K线到来时触发
    public Double onBarUpdate(Double price){
        // 更新历史最高价&最低价
        this.history_max = Math.max(this.history_max, price);
        this.history_min = Math.min(this.history_min, price);

        // 更新当前仓位的利润
        double realTimeProfit = (price - this.pre_price) * this.vol;
        this.profit += realTimeProfit;

        // 更新当前仓位的pre_price为price
        this.pre_price = price;
        return realTimeProfit;
    }

    // 收盘后触发
    public void afterDayUpdate(){
        this.time_monitor = 0;
        this.static_monitor = 0;
        this.dynamic_monitor = 0;
    }


}
