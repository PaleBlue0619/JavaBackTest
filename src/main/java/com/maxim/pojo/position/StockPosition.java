package com.maxim.pojo.position;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class StockPosition extends Position {
    public Double profit; // 持仓利润
    public Double ori_price;  // 原始价格
    public Double pre_price;  // 上一个K线的价格, 不会在创建K线的时候赋值, 而会在monitor的时候赋值
    public Integer time_monitor;
    public Integer static_monitor;  // 静态止盈止损, 当且仅当当前仓位在队列第一个元素会据此进行平仓
    public Double static_profit; // 静态止盈比例
    public Double static_loss; // 静态止损比例
    public Double static_high = null;
    public Double static_low = null;
    public Integer dynamic_monitor;  // 动态止盈止损, 当且仅当当前仓位在队列第一个元素会据此进行平仓
    public Double dynamic_profit; // 动态止盈比例
    public Double dynamic_loss; // 动态止损比例
    public Double dynamic_high = null;
    public Double dynamic_low = null;
    public Double history_max;
    public Double history_min;

    public StockPosition(){

    }

    public StockPosition(Double price, Integer vol,
                         LocalDateTime min_timestamp, LocalDateTime max_timestamp,
                         Double static_profit, Double static_loss,
                         Double dynamic_profit, Double dynamic_loss) {
        super(price, vol, min_timestamp, max_timestamp);
        this.ori_price = price;
        this.pre_price = price;
        this.profit = 0.0;  // 一开始买入的利润一定为0
        this.time_monitor = 0; // 初始化是否需要监控时间为0,表示还不知道,需要等到该仓位该天第一次被监控时进行判断
        this.static_monitor = 0;
        this.static_profit = static_profit;
        this.static_loss = static_loss;
        // 由于平仓是不会改变均价的, 而我这套系统加仓会另开一个仓位, 所以当仓位建立的时候, static_high & static_low就已经确定了
        this.static_high = price * (1 + static_profit);
        this.static_low = price * (1 - static_loss);
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

    // 最短&最长持仓时间判断
    public void onBarMonitorTime(LocalDate currentDate, LocalDateTime currentTime, LocalDate end_date){
        /*
        * end_date: info 中获取的股票退市时间
        * */
        // 这个仓位今天第一个K线到来的时候进行判断
        if (this.time_monitor == 0){
            LocalDate min_date = this.min_timestamp.toLocalDate();
            LocalDate max_date = this.max_timestamp.toLocalDate();

            // if (min_date.isEqual(date) || min_date.isAfter(date)){  // TODO: 判断应该是哪一种写法更好
            if (min_date.isAfter(currentTime.toLocalDate())){
                this.time_monitor = -2;                         // T+K 制度, 禁止K日平仓
                return;
            }else if (end_date.isAfter(currentDate) && min_date.isBefore(currentDate) && currentDate.isBefore(max_date)) {
                // 在最长持仓时间内 + 股票没有退市(end_date之前)
                this.time_monitor = -1;  // 需要精确到分钟进行判断,-1也说明不能平仓,每个K线维护
                return;
            }else{
                // 可以正常平仓
                this.time_monitor = 1;
                return;
            }
        }

        if (this.time_monitor == -1){  // 今天某个时刻会超过最短持仓时间
            if (currentTime.isAfter(this.min_timestamp)){
                this.time_monitor = 1;
                return;
            }
        }
    }

    // 静态止盈&止损判断
    public void onBarMonitorStatic(Double daily_max_price, Double daily_min_price){
        // TODO: 处理daily_max_price & daily_min_price 为 null的情况, 相当于infoDict没提供
        /*
        * daily_min_price: [上帝视角] 加速止盈止损判断
        * daily_max_price: [上帝视角] 加速止盈止损判断
        *
        * */

//        Double static_high = (this.static_profit != null) ? (1 + this.static_profit) * this.price : null;
//        Double static_low = (this.static_loss != null) ? (1 - this.static_loss) * this.price : null;
//        this.static_high = static_high;
//        this.static_low = static_low;

        // 这个仓位今天第一个K线到来的时候进行判断
        if (this.static_monitor == 0){
            if (this.static_high==null && this.static_low==null){
                this.static_monitor = -1;
            }else if(this.static_high==null && daily_min_price<this.static_low){
                this.static_monitor = -1;
            }else if(this.static_low==null && daily_max_price>this.static_high){
                this.static_monitor = -1;
            }else{
                this.static_monitor = 1;
            }
        }
    }

    public void onBarMonitorDynamic(Double daily_max_price, Double daily_min_price) {
        // TODO: 处理daily_max_price & daily_min_price 为 null的情况, 相当于infoDict没提供
        /*
         * daily_min_price: [上帝视角] 加速止盈止损判断
         * daily_max_price: [上帝视角] 加速止盈止损判断
         * 动态止盈止损只能通过振幅判断, 即通过比较理论最大动态振幅与设置的动态止盈止损比例进行判断,
         * 如果0<理论最大上涨幅度<动态止盈比例,则必然不可能满足止盈条件 [极端条件就是先min(daily_low,history_min)一下子爬到daily_high]
         * 如果0<理论最大下跌幅度<动态止损比例,则必然不可能满足止损条件 [极端条件就是先max(daily_max,history_high)一下子掉到daily_low]
         * 总结起来, 如果两者同时满足,即动态止盈止损比例均高于日内理论最大动态振幅,而且只需要比较一次history_min & history_max和 daily_low & daily_max

         * */
        // 计算当前动态止盈止损价格并进行更新
        Double dynamic_high = (this.dynamic_profit != null) ? (1 + this.dynamic_profit) * this.history_min : null;
        Double dynamic_low = (this.dynamic_loss != null) ? (1 - this.dynamic_loss) * this.history_max : null;
        this.dynamic_high = dynamic_high;
        this.dynamic_low = dynamic_low;

        // 这个仓位今天第一个K线到来的时候进行判断
        if (this.dynamic_monitor == 0) {
            double max_price = Math.max(daily_max_price, this.history_max);  // 理论最大动态下跌幅度所用到的价格
            double min_price = Math.min(daily_min_price, this.history_min);  // 理论最大动态上涨幅度所用到的价格
            if (this.dynamic_profit == null && this.dynamic_loss == null) {
                this.dynamic_monitor = -1; // 不设动态止盈止损
            } else if (this.dynamic_profit == null && (max_price - daily_min_price) / max_price < this.dynamic_loss) {
                this.dynamic_monitor = -1; // 只设动态止损+max(daily_max,history_high)->daily_low的振幅<动态止损比例：今天必然触发不了
            } else if (this.dynamic_loss == null && (daily_max_price - min_price) / min_price < this.dynamic_profit) {
                this.dynamic_monitor = -1; // 只设动态止盈+min(daily_min,history_low)->daily_high的振幅<动态止盈比例, 今天必然触发不了
            } else {
                this.dynamic_monitor = 1;
            }
        }
    }
}
