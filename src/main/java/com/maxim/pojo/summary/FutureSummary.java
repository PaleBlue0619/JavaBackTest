package com.maxim.pojo.summary;
import java.time.LocalDateTime;

public class FutureSummary extends Summary{
    public Integer dynamic_monitor;
    public Integer static_monitor;
    public Double profit;
    public Double realTimePrice; // 最新价格
    public Double realTimeProfit; // 最新浮盈浮亏
    public FutureSummary(Double ori_price, Integer total_vol, Double static_profit, Double static_loss, Double dynamic_profit,
                         Double dynamic_loss){
        super(ori_price, total_vol, static_profit, static_loss, dynamic_profit, dynamic_loss);
        this.dynamic_monitor = 0;
        this.static_monitor = 0;
        this.profit = 0.0;
        this.realTimeProfit = 0.0;
    }

    // 开仓时触发回调
    public void openUpdate(Double price, Integer vol, Double static_profit, Double static_loss, Double dynamic_profit, Double dynamic_loss){
        // 更新基本属性
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
        this.ori_price = amount1 / vol1;  // 更新持仓均价
    }

    public void closeLongUpdate(Double price, Integer vol){
        /*
        更新基本属性
        对于多头仓位视图, 以price卖出vol后, summary的变动
         */
        // 获取当前持仓的均价
        this.total_vol -= vol;

        // 减去对应vol的盈亏, 因为已经被平仓平掉了
        // TODO: 需要思考和减掉vol/total_vol * profit的盈亏对比, 决定采用哪一个写法?
        double profit = (price - this.ori_price) * vol * 1;
        this.profit -= profit;
        this.realTimeProfit -= profit;
    }

    public void closeShortUpdate(Double price, Integer vol){
        /*
        更新基本属性
        对于空头仓位视图, 以price卖出vol后, summary的变动
         */
        // 获取当前持仓的均价
        this.total_vol -= vol;

        // 减去对应vol的盈亏, 因为已经被平仓平掉了
        // TODO: 需要思考和减掉vol/total_vol * profit的盈亏对比, 决定采用哪一个写法?
        this.profit -= (this.ori_price - price) * vol * -1;
    }

    public void onBarLongUpdate(Double price){
        // 每个K线到来的时候执行该方法, 更新持仓的price & 最新利润
        this.realTimePrice = price;
        this.realTimeProfit = (price - this.ori_price) * this.total_vol * 1;  // 更新最新利润
    }

    public void onBarShortUpdate(Double price){
        // 每个K线到来的时候执行该方法, 更新持仓的price & 最新利润
        this.realTimePrice = price;
        this.realTimeProfit = (this.ori_price - price) * this.total_vol * -1;  // 获取最新利润
    }

    public void afterDayLongUpdate(Double settle){
        // 当日结算时执行该方法, 获取最新利润
        this.realTimePrice = settle;
        this.realTimeProfit = (this.ori_price - settle) * this.total_vol * 1;
    }

    public void afterDayShortUpdate(Double settle){
        // 当日结算时执行该方法, 获取最新利润
        this.realTimePrice = settle;
        this.realTimeProfit = (settle - this.ori_price) * this.total_vol * -1;
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