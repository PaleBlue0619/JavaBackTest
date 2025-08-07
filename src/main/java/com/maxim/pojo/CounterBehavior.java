package com.maxim.pojo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxim.pojo.BackTestConfig;
import com.maxim.pojo.TradeBehavior;
import com.maxim.pojo.info.StockInfo;
import com.maxim.pojo.kbar.StockBar;
import com.maxim.pojo.position.Position;
import com.maxim.pojo.position.StockPosition;
import com.maxim.pojo.record.StockRecord;
import com.maxim.pojo.summary.StockSummary;
import com.xxdb.DBConnection;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.*;

public class CounterBehavior extends TradeBehavior {
    public CounterBehavior(){
        super();
    }

    public static void executeStock(String symbol, Double price, Double vol,
                                    Double static_profit, Double static_loss,
                                    Double dynamic_profit, Double dynamic_loss,
                                    LocalDateTime min_timestamp, LocalDateTime max_timestamp,
                                    String reason){
        /*
          """
        【核心函数】股票开仓/加仓(默认无手续费)
        min_price:平仓最小价格(止损)
        max_price:平仓最大价格(止盈)
        max_timestamp:持仓最大时间戳
        """
        */

        // 获取BackTestConfig实例
        BackTestConfig config = BackTestConfig.getInstance();

        // 初始化Summary视图对象
        Position pos = new StockPosition(price, vol, min_timestamp, max_timestamp, 0);
        if (!config.getStockPosition().containsKey(symbol)){
            // 说明当前没有该股票的持仓
            ArrayList<Position> pos_list = new ArrayList<>();
            pos_list.add(pos);
            config.stockPosition.put(symbol, pos_list); // 新增该股票的持仓
        }else{
            config.stockPosition.get(symbol).add(pos); // 新增该股票的持仓
        }

        if (!config.getStockSummary().containsKey(symbol)){
            StockSummary summary = new StockSummary(price, vol, static_profit, static_loss, dynamic_profit, dynamic_loss, price, price); // 这里history_min & history_max 都是当前价格
            config.stockSummary.put(symbol, summary);
        }else{
            // 需要先更新止盈止损属性
            StockSummary summary = config.stockSummary.get(symbol);
            StockSummary summary_updated = summary.update(summary, price, vol, static_profit, static_loss, dynamic_profit, dynamic_loss);
            config.stockSummary.put(symbol, summary_updated);
        }

        // 在StockRecord中记录
        StockRecord R = new StockRecord("close", reason, config.currentDate, config.currentMinute, config.currentTimeStamp, symbol, price, vol, 0.0);
        config.stockRecord.add(R);

        config.cash-=vol*price; // 减去股票购买成本
    }

    public static void closeStock(String symbol, Double price, Double vol, String reason){
        double profit = 0.0; // 该笔交易获得的盈利
        double margin = 0.0; // 该笔交易获得的保证金
        BackTestConfig config = BackTestConfig.getInstance(); // 获取BackTestConfig示例
        LinkedHashMap<String, StockSummary> summary = config.stockSummary; // 获取当前股票的持仓视图

        if (!config.stockPosition.isEmpty()){
            if (!config.stockPosition.containsKey(symbol)){
                System.out.printf("当前%s没有持仓%n", symbol);
            }else{
                // 当前股票有持仓
                // 获取该股票的持仓
                ArrayList<Position> pos_list = config.stockPosition.get(symbol);
                ArrayList<Double> current_vol_list = new ArrayList<>();
                ArrayList<Double> ori_price_list = new ArrayList<>();
                ArrayList<Integer> time_monitor_list = new ArrayList<>();
                for (Position pos : pos_list) {
                    current_vol_list.add(pos.getVol());
                    ori_price_list.add(pos.getPrice());
                    time_monitor_list.add(pos.getTime_monitor());
                }

                // 获取允许平仓的最大数量
                double current_vol;
                boolean state;
                if (time_monitor_list.contains(-2)){
                    // 说明当前持仓队列中存在禁止卖出的股票
                    int index = time_monitor_list.indexOf(-2);
                    current_vol = current_vol_list.stream()
                            .limit(index)
                            .mapToDouble(Double::doubleValue)
                            .sum();
                    state = false;
                }else{
                    // 说明当前持仓队列中所有股票均可以卖出
                    current_vol = current_vol_list.stream()
                            .mapToDouble(Double::doubleValue)
                            .sum();
                    state = true;
                }
                if (current_vol == 0.0){
                    return ; // 说明当前无法平仓
                }
                // 获取当前可以平仓的最大数量
                double max_vol = Math.min(current_vol, vol);
                double record_vol = max_vol;

                if (max_vol >= current_vol && state){  // 说明都可以平仓
                    // 先对视图进行批处理
                    summary.remove(symbol); // 删除该股票的持仓视图
                    // 逐笔计算盈亏
                    for (int i = 0; i < current_vol_list.size(); i++) {
                        Double position_vol = current_vol_list.get(i);
                        Double ori_price = ori_price_list.get(i);
                        margin += price * position_vol;
                        profit += (price - ori_price) * position_vol;  // 逐笔盈亏
                    }
                    // 再对持仓进行处理
                    config.stockPosition.remove(symbol); // 直接删除该股票的持有
                }else{  // 说明只有部分仓位可以被平

                    // 先对视图进行批处理
                    double vol0, amount0, vol1, amount1;
                    vol0 = summary.get(symbol).total_vol;
                    amount0 = summary.get(symbol).total_vol * summary.get(symbol).ori_price;
                    vol1 = vol0 + vol;
                    amount1 = amount0 + vol * price;
                    summary.get(symbol).ori_price = amount1 / vol1;
                    // 再对持仓进行处理
                    for (int i=0; i<current_vol_list.size(); i++){
                        Double posVol = current_vol_list.get(i);
                        Double posPrice = ori_price_list.get(i);
                        if (max_vol >= posVol){ // 当前订单全部平仓
                            margin += price*posVol;
                            profit += (price - posPrice) * posVol;
                            pos_list.remove(0); // FIFO Queue
                            max_vol -= posVol;
                        }else{ // 当前订单部分平仓
                            margin += price*max_vol;
                            profit += (price - posPrice) * max_vol;
                            pos_list.get(0).vol = posVol - max_vol; // FIFO Queue
                            break;
                        }
                    }
                    // 记录本次交易
                    StockRecord R = new StockRecord("close", reason, config.currentDate, config.currentMinute, config.currentTimeStamp,
                            symbol, price, record_vol, profit);
                    config.stockRecord.add(R);

                    // 结算
                    config.profit += profit;
                    config.cash += margin;  // 股票交易中一开始付出的现金可以理解为100%保证金
                    config.stockPosition.put(symbol, pos_list); // 更新股票持仓
                    config.stockSummary.put(symbol, summary.get(symbol));
                }

            }

        }else{
            // 当前一只票也没有持仓, 怎么平仓呢???
        }
    }

    public static void monitorStockPosition(boolean order_sequence){
        /*
        【柜台处理订单后运行,可重复运行】每日盘中运行,负责监控当前持仓是否满足限制平仓要求
        order_sequence=true 假设max_price先判断
        order_sequence=false 假设min_price先判断
        */

        // 获取当前配置实例
        BackTestConfig config = BackTestConfig.getInstance();
        LinkedHashMap<String, ArrayList<Position>> pos = config.getStockPosition();
        if (pos.isEmpty()){
            return ;  // 当前没有持仓
        }

        LocalDate date = config.currentDate;
        // String minute = Integer.toString(config.currentMinute);
        Integer minute = config.currentMinute;
        LocalDateTime timestamp = config.currentTimeStamp;
        // LinkedHashMap<Integer, HashMap<String, StockBar>> stock_k_dict = config.stockKDict;
        if (!config.stockKDict.containsKey(minute)){
            return ;
        }
        HashMap<String, StockBar> stock_k_dict = config.stockKDict.get(minute);
        HashMap<String, StockInfo> stock_info_dict = config.stockInfoDict;

        for (String symbol: pos.keySet()){
            StockInfo info_dict = stock_info_dict.get(symbol);
            StockBar kBar = stock_k_dict.get(symbol); // 当前股票分钟Bar
            StockSummary summary = config.stockSummary.get(symbol); // 当前股票持仓视图

            // Step0. 获取基本信息并更新股票视图
            LocalDate end_date = info_dict.end_date;
            Double daily_max_price = info_dict.high;
            Double daily_min_price = info_dict.low;
            Double high_price = kBar.high;
            Double low_price = kBar.low;
            Double close_price = kBar.close;
            Double ori_price = summary.ori_price;
            Double static_profit = summary.static_profit;
            Double static_loss = summary.static_loss;
            Double dynamic_profit = summary.dynamic_profit;
            Double dynamic_loss = summary.dynamic_loss;
            Double history_high = summary.history_max;
            Double history_low = summary.history_min;
            Double static_high = (static_profit != null) ? ori_price * (1 + static_profit) : null;
            Double static_low = (static_loss != null) ? ori_price * (1 - static_loss) : null;
            Double dynamic_high = (dynamic_profit != null) ? history_low * (1 + dynamic_profit) : null;
            Double dynamic_low = (dynamic_loss != null) ? history_high * (1 - dynamic_loss) : null;

            // 更新股票视图
            summary.history_max = Math.max(summary.history_max, high_price);
            summary.history_min = Math.min(summary.history_min, low_price);
            config.stockSummary.put(symbol,summary);

            ArrayList<Position> positionList = pos.get(symbol);
            int i = 0;
            while (i < positionList.size()){
                // 若当前已经清空该股票的持仓, 进行保护
                if (!config.stockPosition.containsKey(symbol)){
                    i++;
                    continue;
                }

                Position position = positionList.get(i);
                if (position.getTime_monitor() < 0){
                    i++;
                    continue;
                }

                // 获取持仓信息
                Double price = position.getPrice();
                Double vol = position.getVol();
                LocalDateTime min_timestamp = position.getMin_timestamp();
                LocalDate min_date = min_timestamp.toLocalDate();
                LocalDateTime max_timestamp = position.getMax_timestamp();
                LocalDate max_date = max_timestamp.toLocalDate();
                int time_monitor = position.getTime_monitor();

                // Step1. 给出时间维度基本判断(当天后续回测时间是否需要继续监视该订单)-持仓时间维度
                if (time_monitor == 0){  // 如果还没判断过 (time_monitor = 0)
                    if (min_date.isEqual(date) || min_date.isAfter(date)){
                        // T+1 制度, 禁止当日平仓
                        time_monitor = -2;
                    } else if (end_date.isAfter(date) && min_date.isBefore(date) && date.isBefore(max_date)) {
                        // 在最长持仓时间内 + 股票没有退市(end_date之前)
                        time_monitor = -1;
                    }else{
                        // 可以正常平仓
                        time_monitor = 1;
                    }
                    // 更新time_monitor信息
                    position.setTime_monitor(time_monitor);
                }
                i++;

                // Step2. 对限时单进行平仓[在JavaBackTest中, 时间维度优先级>价格维度]
                if (time_monitor == 1){ // 已经退市的股票
                    if (date.isEqual(end_date) || date.isAfter(end_date)){
                        CounterBehavior.closeStock(symbol, close_price, vol, "end_date");
                        continue;
                    }
                    if (timestamp.isAfter(max_timestamp)){ // 超过最长持仓时间
                        CounterBehavior.closeStock(symbol, close_price, vol, "max_timestamp");
                    }
                }

                // Step3. 给出静态价格维度基本判断(当天后续时间是否需要继续监视该订单)
                int static_monitor;
                if (static_high==null && static_low==null){
                    static_monitor = -1;
                }else if(static_high==null && daily_min_price<static_low){
                    static_monitor = -1;
                }else if(static_low==null && daily_max_price>static_high){
                    static_monitor = -1;
                }else{
                    static_monitor = 1;
                }
                if (config.stockSummary.containsKey(symbol)){
                    config.stockSummary.get(symbol).static_monitor = static_monitor;
                } // 设置static_monitor: -1表示不监视/1表示监视, 该属性会在收盘后置0

                // Step4. 对静态限价单进行平仓
                if (static_monitor==1){
                    if (order_sequence){ // 假设最高价先到来, 先判断最高价条件
                        if (static_high!=null && high_price >= static_high){
                            CounterBehavior.closeStock(symbol, static_high, summary.total_vol, "static_high");
                            continue;
                        }
                        if (static_low!=null && low_price <= static_low){
                            CounterBehavior.closeStock(symbol, static_low, summary.total_vol, "static_low");
                            continue;
                        }
                    }else{  // 假设最低价先到来, 先判断最低价条件
                        if (static_low!=null && low_price <= static_low){
                            CounterBehavior.closeStock(symbol, static_low, summary.total_vol, "static_low");
                            continue;
                        }
                        if (static_high!=null && high_price >= static_high){
                            CounterBehavior.closeStock(symbol, static_high, summary.total_vol, "static_high");
                            continue;
                        }
                    }
                }

                // Step5. 给出动态价格维度基本判断(当天后续时间是否需要继续监视该订单)
                int dynamic_monitor;
                if (dynamic_high==null && dynamic_low==null){
                    dynamic_monitor = -1; // 不设动态止盈止损
                }else if (dynamic_high==null && daily_min_price < dynamic_low){
                    dynamic_monitor = -1; // 不设最大价格+当日最低价格<目标最低价(动态)
                }else if (dynamic_low==null && daily_max_price > dynamic_high){
                    dynamic_monitor = -1; // 不设最小价格+当日最高价格>目标最高价(动态)
                }else{
                    dynamic_monitor = 1;
                }

                if (config.stockSummary.containsKey(symbol)){
                    config.stockSummary.get(symbol).dynamic_monitor = dynamic_monitor;
                } // 设置dynamic_monitor: -1表示不监视/1表示监视, 该属性会在收盘后置0

                // Step6. 对动态限价单进行平仓
                if (dynamic_monitor == 1){
                    if (order_sequence){ // 假设最高价先到来, 先判断最高价条件
                        if (dynamic_high!=null && high_price >= dynamic_high){
                            CounterBehavior.closeStock(symbol, dynamic_high, summary.total_vol, "dynamic_high");
                            continue;
                        }
                        if (dynamic_low!=null && low_price <= dynamic_low){
                            CounterBehavior.closeStock(symbol, dynamic_low, summary.total_vol, "dynamic_low");
                            continue;
                        }
                    }else{ // 假设最低价先到来, 先判断最低价条件
                        if (dynamic_low!=null && low_price <= dynamic_low){
                            CounterBehavior.closeStock(symbol, dynamic_low, summary.total_vol, "dynamic_low");
                            continue;
                        }
                        if (dynamic_high!=null && high_price >= dynamic_high){
                            CounterBehavior.closeStock(symbol, dynamic_high, summary.total_vol, "dynamic_high");
                            continue;
                        }
                    }
                }
            }
        }
    }
}
