package com.maxim.pojo.position;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FuturePosition extends Position{
    public Double profit; // 实时利润
    public Double margin; // 保证金金额
    public Double margin_rate; // 保证金比率
    public Integer hold_days; // 用于平仓时计算收益
    public Double pre_price;  // 上一个K线的价格
    public Integer time_monitor;
    public Integer static_monitor;
    public Double static_profit;
    public Double static_loss;
    public Double static_high = null;
    public Double static_low = null;
    public Integer dynamic_monitor;
    public Double dynamic_profit;
    public Double dynamic_loss;
    public Double dynamic_high = null;
    public Double dynamic_low = null;
    public Double history_max;
    public Double history_min;

    public FuturePosition(){

    }

    public FuturePosition(Double price, Integer vol, Double margin_rate, LocalDateTime min_timestamp, LocalDateTime max_timestamp,
                          Double static_profit, Double static_loss,
                          Double dynamic_profit, Double dynamic_loss) {
        super(price, vol, min_timestamp, max_timestamp);
        this.pre_price = price; // 创建的时候就是开仓价， 后续在afterBar的中会更新该属性
        this.profit = 0.0;  // 一开始买入的时候利润一定为0.0
        this.margin_rate = margin_rate;
        this.margin = margin_rate * vol * price;  // TODO: 增加一个参数,使得能够根据这个参数选择对应的保证金计算方式
        this.hold_days = 0;
        this.time_monitor = 0;
        this.static_monitor = 0;
        this.static_profit = static_profit;
        this.static_loss = static_loss;
        // 这里无法感知到是FuturePosition多单还是空单, 所以不能确定static_high 和 static_low, 留给外部executeFuture函数, 即执行仓位的时候去进行该属性的更新
        this.dynamic_monitor = 0;
        this.dynamic_profit = dynamic_profit;
        this.dynamic_loss = dynamic_loss;
        this.history_max = price;
        this.history_min = price;
    }

    // 盘前触发
    public Double marginRateUpdate(Double margin_rate){
        // 输入今日的对应保证金率, 更新当前保证金水平, 返回需要在现金账户中增加/减少的保证金
        if (Math.abs(this.margin_rate - margin_rate) < 1e-6){
            return 0.0; // 保证金率没有发生变化, 无需增加/减少保证金
        }
        // 需要增加或减少的保证金
        double marginDiff = (margin_rate - this.margin_rate) * this.vol * this.pre_price;
        this.margin -= marginDiff;
        this.margin_rate = margin_rate; // 更新保证金率水平
        return marginDiff;
    }

    // K线到来时触发
    public Double onBarLongUpdate(Double price){
        // 更新历史最高价&最低价
        this.history_max = Math.max(this.history_max, price);
        this.history_min = Math.min(this.history_min, price);

        // 更新当前仓位的利润
        double realTimeProfit = (price - this.pre_price) * this.vol * 1; // 这个K线上的pnl
        this.profit += realTimeProfit;
        this.margin += realTimeProfit;

        // 更新当前仓位的pre_price为price
        this.pre_price = price;
        return realTimeProfit;
    }

    public Double onBarShortUpdate(Double price){
        // 更新历史最高价&最低价
        this.history_max = Math.max(this.history_max, price);
        this.history_min = Math.min(this.history_min, price);

        // 更新当前仓位的利润 & 保证金
        double realTimeProfit = (price - this.pre_price) * this.vol * -1; // 这个K线上的pnl
        this.profit += realTimeProfit;
        this.margin += realTimeProfit;

        // 更新当前仓位的pre_price为price
        this.pre_price = price;
        return realTimeProfit;
    }

    public Double afterDayLongUpdate(Double settle) {
        // 多头仓位给定当日结算价, 进行更新
        // 更新当前仓位monitor
        this.time_monitor = 0;
        this.static_monitor = 0;
        this.dynamic_monitor = 0;

        // 更新当前仓位的利润
        double realTimeProfit = (settle - this.pre_price) * this.vol * 1;
        this.profit += realTimeProfit;
        this.margin += realTimeProfit;

        // 更新当前仓位的pre_price为price
        this.pre_price = settle;
        return realTimeProfit;
    }

    public Double afterDayShortUpdate(Double settle){
        // 空头仓位给定当日结算价, 进行更新
        // 更新当前仓位monitor
        this.time_monitor = 0;
        this.static_monitor = 0;
        this.dynamic_monitor = 0;

        // 更新当前仓位的利润
        double realTimeProfit = (settle - this.pre_price) * this.vol * -1;
        this.profit += realTimeProfit;
        this.margin += realTimeProfit;

        // 更新当前仓位的pre_price为price
        this.pre_price = settle;
        return realTimeProfit;
    }

    // 最短&最长持仓时间判断
    public void onBarMonitorTime(LocalDate currentDate, LocalDateTime currentTime, LocalDate end_date){
        /*
        * end_date: info 中获取的期货合约到期日
        * */
        if (this.time_monitor == 0){
            LocalDate min_date = this.min_timestamp.toLocalDate();
            LocalDate max_date = this.max_timestamp.toLocalDate();
            // if (min_date.isEqual(date) || min_date.isAfter(date)){  // TODO: 判断应该是哪一种写法更好
            if (min_date.isAfter(currentDate)){
                // T+1 制度, 禁止当日平仓
                time_monitor = -2;
            } else if (end_date.isAfter(currentDate) && min_date.isBefore(currentDate) && currentDate.isBefore(max_date)) {
                // 在最长持仓时间内 + 期货合约没有结束(end_date之前)
                time_monitor = -1;
            }else{
                // 可以正常平仓
                time_monitor = 1;
            }
        }

        if (this.time_monitor == -1){  // 今天某个时刻会超过最短持仓时间
            if (currentTime.isAfter(this.min_timestamp)){
                this.time_monitor = 1;
            }
        }
    }

    // 静态止盈&止损判断, 这里多头和空头共享, 因为两者在executeFuture中会赋予不同的static_high & static_low;
    public void onBarMonitorStatic(Double daily_max_price, Double daily_min_price){
        // TODO: 处理daily_max_price & daily_min_price 为 null的情况, 相当于infoDict没提供
        /*
         * daily_min_price: [上帝视角] 加速止盈止损判断
         * daily_max_price: [上帝视角] 加速止盈止损判断
         *
         * */

        // 这个仓位今天第一个K线到来的时候进行判断
        if (this.static_monitor == 0){
            if (this.static_high==null && this.static_low==null){
                this.static_monitor = -1;
            }else if(static_high==null && daily_min_price<this.static_low){
                this.static_monitor = -1;
            }else if(static_low==null && daily_max_price>this.static_high){
                this.static_monitor = -1;
            }else{
                this.static_monitor = 1;
            }
        }
    }

    // 动态止盈&止损判断
    // 这里不能多仓 & 空仓共用一套逻辑了, 静态止盈止损那边能共用是因为在下单的时候已经知道是多头还是空头了, 推理出了对应的静态止盈&止损价格
    // 而动态的止盈止损价格是会随着K线的变动而变动的, 所以必须每个K线都判断(?或者还有什么可以加速判断的), 必须onBarMonitor
    // 所以仓位本身感知不到自己是多头仓位还是空头仓位, 需要在monitor模块进行判断->调用不同的monitor
    public void onBarMonitorLongDynamic(Double daily_max_price, Double daily_min_price) {
        // TODO: 处理daily_max_price & daily_min_price 为 null的情况, 相当于infoDict没提供
        /*
         * daily_min_price: [上帝视角] 加速止盈止损判断
         * daily_max_price: [上帝视角] 加速止盈止损判断
         * 动态止盈止损只能通过振幅判断, 即通过比较理论最大动态振幅与设置的动态止盈止损比例进行判断,
         * 如果0<上涨最大振幅<动态止盈比例,则必然不可能满足止盈条件 [极端条件就是先min(daily_low,history_min)一下子爬到daily_high]
         * 如果0<下跌最大振幅<动态止损比例,则必然不可能满足止损条件 [极端条件就是先max(daily_max,history_high)一下子掉到daily_low]
         * 总结起来, 如果两者同时满足,即动态止盈止损比例均高于日内理论最大动态振幅,而且只需要比较一次history_min & history_max和 daily_low & daily_max
         * */
        // 计算当前动态止盈止损价格并进行更新
        Double dynamic_high = (this.dynamic_profit != null) ? (1 + this.dynamic_profit) * this.history_min : null;
        Double dynamic_low = (this.dynamic_loss != null) ? (1 - this.dynamic_loss) * this.history_max : null;
        this.dynamic_high = dynamic_high;
        this.dynamic_low = dynamic_low;

        // 这个仓位今天第一个K线到来的时候进行判断
        if (this.dynamic_monitor == 0){
            double max_price = Math.max(daily_max_price, this.history_max);  // 理论最大动态下跌幅度所用到的价格
            double min_price = Math.min(daily_min_price, this.history_min);  // 理论最大动态上涨幅度所用到的价格
            if (this.dynamic_profit == null && this.dynamic_loss == null) {
                this.dynamic_monitor = -1; // 不设动态止盈止损
            } else if (this.dynamic_profit == null && (max_price - daily_min_price) / max_price < this.dynamic_loss) {
                this.dynamic_monitor = -1; // 只设动态止损+max(daily_high,history_high)->daily_low的振幅<动态止损比例：今天必然触发不了
            } else if (this.dynamic_loss == null && (daily_max_price - min_price) / min_price < this.dynamic_profit) {
                this.dynamic_monitor = -1; // 只设动态止盈+min(daily_low,history_low)->daily_high的振幅<动态止盈比例, 今天必然触发不了
            } else {
                this.dynamic_monitor = 1;
            }
        }
    }

    public void onBarMonitorShortDynamic(Double daily_max_price, Double daily_min_price) {
        // TODO: 处理daily_max_price & daily_min_price 为 null的情况, 相当于infoDict没提供
        /*
         * daily_min_price: [上帝视角] 加速止盈止损判断
         * daily_max_price: [上帝视角] 加速止盈止损判断
         * 动态止盈止损只能通过振幅判断, 即通过比较理论最大动态振幅与设置的动态止盈止损比例进行判断,
         * 如果0<下跌最大振幅<动态止盈比例,则必然不可能满足止盈条件 [极端条件就是先max(daily_max,history_high)一下子掉到daily_low]
         * 如果0<上涨最大振幅<动态止损比例,则必然不可能满足止损条件 [极端条件就是先min(daily_low,history_min)一下子爬到daily_high]
         * 总结起来, 如果两者同时满足,即动态止盈止损比例均高于日内理论最大动态振幅,而且只需要比较一次history_min & history_max和 daily_low & daily_max
         * */
        // 计算当前动态止盈止损价格并进行更新
        Double dynamic_high = (this.dynamic_loss != null) ? (1 + this.dynamic_loss) * this.history_min : null;
        Double dynamic_low = (this.dynamic_profit != null) ? (1 - this.dynamic_profit) * this.history_max : null;
        this.dynamic_high = dynamic_high;
        this.dynamic_low = dynamic_low;

        // 这个仓位今天第一个K线到来的时候进行判断
        if (this.dynamic_monitor == 0){
            double max_price = Math.max(daily_max_price, this.history_max);  // 理论最大动态下跌幅度所用到的价格
            double min_price = Math.min(daily_min_price, this.history_min);  // 理论最大动态上涨幅度所用到的价格
            if (this.dynamic_profit == null && this.dynamic_loss == null) {
                this.dynamic_monitor = -1; // 不设动态止盈止损
            } else if (this.dynamic_profit == null && (daily_max_price - min_price) / min_price < this.dynamic_loss) {
                this.dynamic_monitor = -1; // 只设动态止损+min(daily_low,history_low)->daily_high的振幅<动态止损比例：今天必然触发不了
            } else if (this.dynamic_loss == null && (max_price - daily_min_price) / max_price < this.dynamic_profit) {
                this.dynamic_monitor = -1; // 只设动态止盈+max(daily_high,history_high)->daily_low的振幅<动态止盈比例, 今天必然触发不了
            } else {
                this.dynamic_monitor = 1;
            }
        }
    }
}