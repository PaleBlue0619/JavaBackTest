package com.maxim.pojo.summary;
import java.time.LocalDateTime;

public class Summary{
    public Double ori_price;
    public Double total_vol;
    public Double static_profit;
    public Double static_loss;
    public Double dynamic_profit;
    public Double dynamic_loss;

    // Constructor
    public Summary(Double ori_price, Double total_vol, Double static_profit, Double static_loss, Double dynamic_profit,
                   Double dynamic_loss) {
        this.static_profit = static_profit;
        this.ori_price = ori_price; // 初始价格 -> 后续会变成持仓买入均价
        this.total_vol = total_vol;
        this.static_loss = static_loss;
        this.dynamic_profit = dynamic_profit;
        this.dynamic_loss = dynamic_loss;
    }

    public Double getOri_price() {
        return ori_price;
    }

    public void setOri_price(Double ori_price) {
        this.ori_price = ori_price;
    }

    public Double getTotal_vol() {
        return total_vol;
    }

    public void setTotal_vol(Double total_vol) {
        this.total_vol = total_vol;
    }

    public Double getStatic_profit() {
        return static_profit;
    }

    public void setStatic_profit(Double static_profit) {
        this.static_profit = static_profit;
    }

    public Double getStatic_loss() {
        return static_loss;
    }

    public void setStatic_loss(Double static_loss) {
        this.static_loss = static_loss;
    }

    public Double getDynamic_profit() {
        return dynamic_profit;
    }

    public void setDynamic_profit(Double dynamic_profit) {
        this.dynamic_profit = dynamic_profit;
    }

    public Double getDynamic_loss() {
        return dynamic_loss;
    }

    public void setDynamic_loss(Double dynamic_loss) {
        this.dynamic_loss = dynamic_loss;
    }

}