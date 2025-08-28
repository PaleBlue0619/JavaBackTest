package com.maxim.pojo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxim.pojo.BackTestConfig;
import com.maxim.pojo.TradeBehavior;
import com.maxim.pojo.info.FutureInfo;
import com.maxim.pojo.info.StockInfo;
import com.maxim.pojo.kbar.FutureBar;
import com.maxim.pojo.kbar.StockBar;
import com.maxim.pojo.position.FuturePosition;
import com.maxim.pojo.position.Position;
import com.maxim.pojo.position.StockPosition;
import com.maxim.pojo.record.FutureRecord;
import com.maxim.pojo.record.StockRecord;
import com.maxim.pojo.summary.FutureSummary;
import com.maxim.pojo.summary.StockSummary;
import com.xxdb.DBConnection;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.*;
import java.util.concurrent.Future;

public class CounterBehavior extends TradeBehavior {
    public CounterBehavior(){
        super();
    }

    public static void afterBarStock(){
        /*
        * 1. 更新全局属性(仓位的动态盈亏(由于所有Position在创建的时候pre_price == price, 所以第一个K线的动态盈亏始终为0.0))
        * 2. 更新股票视图(realTimePrice/realTimeProfit)
        * 3. 更新股票仓位相关属性(history_min/history_max/profit/preprice)
        * */

        // 获取BackTestConfig实例
        BackTestConfig config = BackTestConfig.getInstance();

        // 获取当前K线
        int minute = config.currentMinute;
        HashMap<String, StockBar> stock_k_dict = config.stockKDict.get(minute);

        // 获取持仓 & 视图
        LinkedHashMap<String, ArrayList<StockPosition>> stockPosition = config.getStockPosition();

        for (String symbol: stockPosition.keySet()){
            if (stock_k_dict.containsKey(symbol)){
                double close = stock_k_dict.get(symbol).close;  // 当前收盘价

                // 1.更新视图
                config.getStockSummary().get(symbol).onBarUpdate(close);

                // 2.更新仓位
                ArrayList<StockPosition> positionList = stockPosition.get(symbol);
                int k = positionList.size();

                for (int i=1; i<k; i++){
                    Double realTimeProfit = config.getStockPosition().get(symbol).get(i).onBarUpdate(close);
                    // 更新当前实时盈亏
                    config.realTimeProfit += realTimeProfit;
                    config.stockRealTimeProfit += realTimeProfit;
                }
            }
        }
    }

    public static void afterDayStock(){
        // 1. 重置仓位的monitor(time_monitor/static_monitor/dynamic_monitor)
        BackTestConfig config = BackTestConfig.getInstance(); // 获取BackTestConfig实例
        for (String symbol: config.getStockPosition().keySet()){
            ArrayList<StockPosition> positionList = config.getStockPosition().get(symbol);
            int k = positionList.size();
            for (int i=1; i<k; i++){
                config.stockPosition.get(symbol).get(i).afterDayUpdate();
            }
        }
    }

    public static void afterBarFuture(){
        /*
         * 1. 更新全局属性(仓位的动态盈亏(由于所有Position在创建的时候pre_price == price, 所以第一个K线的动态盈亏始终为0.0))
         * 2. 更新期货视图(realTimePrice/realTimeProfit)
         * 3. 更新期货多头/空头仓位的相关属性(history_min/history_max/profit/pre_price)
         * */

        // 获取BackTestConfig实例
        BackTestConfig config = BackTestConfig.getInstance();

        // 获取当前K线
        int minute = config.currentMinute;
        HashMap<String, FutureBar> future_k_dict = config.futureKDict.get(minute);

        // 获取多头持仓 & 空头持仓
        LinkedHashMap<String, ArrayList<FuturePosition>> futureLongPosition = config.getFutureLongPosition();
        LinkedHashMap<String, ArrayList<FuturePosition>> futureShortPosition = config.getFutureShortPosition();

        // 分别处理
        for (String symbol: futureLongPosition.keySet()){
            if (future_k_dict.containsKey(symbol)){
                double close = future_k_dict.get(symbol).close;

                // 1.更新视图
                config.getFutureLongSummary().get(symbol).onBarLongUpdate(close);

                // 2.更新仓位
                ArrayList<FuturePosition> positionList = futureLongPosition.get(symbol);
                int k = positionList.size();

                for (int i=1; i<k; i++){
                    Double realTimeProfit = config.getFutureLongPosition().get(symbol).get(i).onBarLongUpdate(close);
                    // 更新当前实时盈亏
                    config.realTimeProfit += realTimeProfit;
                    config.futureRealTimeProfit += realTimeProfit;
                }
            }
        }

        for (String symbol: futureShortPosition.keySet()){
            if (future_k_dict.containsKey(symbol)){
                double close = future_k_dict.get(symbol).close;

                // 1.更新视图
                config.getFutureShortSummary().get(symbol).onBarShortUpdate(close);

                // 2.更新仓位
                ArrayList<FuturePosition> positionList = futureShortPosition.get(symbol);
                int k = positionList.size();

                for (int i=1; i<k; i++){
                    Double realTimeProfit = config.getFutureShortPosition().get(symbol).get(i).onBarShortUpdate(close);
                    // 更新当前实时盈亏
                    config.realTimeProfit += realTimeProfit;
                    config.futureRealTimeProfit += realTimeProfit;
                }
            }
        }
    }

    public static void executeStock(String symbol, Double price, Double vol,
                                    Double static_profit, Double static_loss,
                                    Double dynamic_profit, Double dynamic_loss,
                                    LocalDateTime min_timestamp, LocalDateTime max_timestamp,
                                    String reason){
        /*
        【核心函数】股票开仓/加仓(默认无手续费)
        */

        // 获取BackTestConfig实例
        BackTestConfig config = BackTestConfig.getInstance();

        // 初始化Summary视图对象
        StockPosition pos = new StockPosition(price, vol, min_timestamp, max_timestamp, static_profit, static_loss, dynamic_profit, dynamic_loss);
        if (!config.getStockPosition().containsKey(symbol)){
            // 说明当前没有该股票的持仓
            ArrayList<StockPosition> pos_list = new ArrayList<>();
            pos_list.add(pos);
            config.stockPosition.put(symbol, pos_list); // 新增该股票的持仓
        }else{
            config.stockPosition.get(symbol).add(pos); // 新增该股票的持仓
        }

        if (!config.getStockSummary().containsKey(symbol)){
            StockSummary summary = new StockSummary(price, vol, static_profit, static_loss, dynamic_profit, dynamic_loss);
            config.stockSummary.put(symbol, summary);
        }else{
            // 需要先更新持仓视图
            config.stockSummary.get(symbol).openUpdate(price, vol, static_profit, static_loss, dynamic_profit, dynamic_loss);
        }

        // 在StockRecord中记录
        StockRecord R = new StockRecord("open", reason, config.currentDate, config.currentMinute, config.currentTimeStamp, symbol, price, vol, 0.0);
        config.stockRecord.add(R);

        config.cash-=vol*price; // 减去股票购买成本
        config.stockCash-=vol*price;
    }

    public static void executeFuture(String order_type, String symbol, Double price, Double vol,
                                     Double static_profit, Double static_loss,
                                     Double dynamic_profit, Double dynamic_loss,
                                     LocalDateTime min_timestamp, LocalDateTime max_timestamp,
                                     String reason){
        /*
        【核心函数】期货开仓/加仓(默认无手续费)
        */

        // 获取BackTestConfig实例
        BackTestConfig config = BackTestConfig.getInstance();

        // 获取当前期货标的的基本信息
        if (!config.futureInfoDict.containsKey(symbol)){
            throw new NullPointerException("期货合约信息字典中不存在"+symbol+", 请在"+config.currentStrDate+"之前的任意一个json中添加该合约的基本信息");
        }
        FutureInfo info = config.futureInfoDict.get(symbol);
        Double pre_settle = info.pre_settle;    // 昨日结算价
        Double margin_rate = info.margin_rate;  // 保证金比率

        // 创建持仓对象
        // TODO: 在FutureInfo中添加一个Integer类型的属性, 用于表明保证金计算方式, 这里获取该属性从而去使用对应方式计算初始保证金
        FuturePosition pos = new FuturePosition(price, vol, margin_rate, min_timestamp, max_timestamp); // 这里已经预先添加了hold_days=0, 相当于无论什么情况下, 只要下单, 那么一开始就没有走完一个day, hold_days=0

        if (order_type.equals("long")){
            // 将该仓位加入对应的持仓中
            if (!config.getFutureLongPosition().containsKey(symbol)){
                // 说明当前没有该期货的持仓
                ArrayList<FuturePosition> pos_list = new ArrayList<>();
                pos_list.add(pos);
                config.futureLongPosition.put(symbol, pos_list); // 新增该期货的持仓
            }else{
                config.futureLongPosition.get(symbol).add(pos); // 新增该期货的持仓
            }

            // 初始化Summary视图对象
            if (!config.getFutureLongSummary().containsKey(symbol)){
                FutureSummary summary = new FutureSummary(price, vol, static_profit, static_loss, dynamic_profit, dynamic_loss); // 这里history_min & history_max 都是当前价格
                config.futureLongSummary.put(symbol, summary); // 加入多头的持仓视图中
            }else{
                // 需要先更新持仓视图
                config.futureLongSummary.get(symbol).openUpdate(price, vol, static_profit, static_loss, dynamic_profit, dynamic_loss);
            }
        }else{
            // 将该仓位加入对应的持仓中
            if (!config.getFutureShortSummary().containsKey(symbol)){
                FutureSummary summary = new FutureSummary(price, vol, static_profit, static_loss, dynamic_profit, dynamic_loss); // 这里history_min & history_max 都是当前价格
                config.futureShortSummary.put(symbol, summary); // 加入空头的持仓视图中
            }else{
                // 需要先更新持仓视图
                config.futureShortSummary.get(symbol).openUpdate(price, vol, static_profit, static_loss, dynamic_profit, dynamic_loss);
            }
        }

        // 在FutureRecord中记录
        double margin = price * vol * margin_rate;
        FutureRecord R = new FutureRecord("open", order_type, reason, config.currentDate, config.currentMinute, config.currentTimeStamp, symbol, price, vol, 0.0);
        config.futureRecord.add(R);
        config.cash-=margin; // 减去期货购买成本, 即付出的保证金
        config.futureCash-=margin;
    }

    public static void closeStock(String symbol, Double price, Double vol, String reason) {
        double profit = 0.0; // 该笔交易获得的盈利
        double margin = 0.0; // 该笔交易获得的保证金
        BackTestConfig config = BackTestConfig.getInstance(); // 获取BackTestConfig示例

        ArrayList<StockPosition> pos_list;
        StockSummary summary;

        if (config.getStockPosition().isEmpty()) {
            return; // 说明当前没有股票持仓
        }
        if (!config.stockPosition.containsKey(symbol)) {
            System.out.printf("当前%s没有持仓%n", symbol);
            return;
        }
        summary = config.stockSummary.get(symbol); // 获取当前标的持仓视图

        // 当前股票有持仓
        // 获取该股票的持仓的具体细节
        pos_list = config.stockPosition.get(symbol);
        ArrayList<Double> current_vol_list = new ArrayList<>();
        ArrayList<Double> ori_price_list = new ArrayList<>();
        ArrayList<Integer> time_monitor_list = new ArrayList<>();
        for (StockPosition pos : pos_list) {
            current_vol_list.add(pos.getVol());
            ori_price_list.add(pos.getPrice());
            time_monitor_list.add(pos.time_monitor);
        }

        // 获取允许平仓的最大数量
        double current_vol;
        boolean state;
        if (time_monitor_list.contains(-2)) {
            // 说明当前持仓队列中存在禁止卖出的标的
            int index = time_monitor_list.indexOf(-2);
            current_vol = current_vol_list.stream()
                    .limit(index)
                    .mapToDouble(Double::doubleValue)
                    .sum();
            state = false;
        } else {
            // 说明当前持仓队列中所有股票均可以卖出
            current_vol = current_vol_list.stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
            state = true;
        }
        if (current_vol == 0.0) {
            return; // 说明当前无法平仓
        }
        // 获取当前可以平仓的最大数量
        double max_vol = Math.min(current_vol, vol);
        double record_vol = max_vol;

        if (max_vol >= current_vol && state) {  // 说明都可以平仓
            // 逐笔计算盈亏
            for (int i = 0; i < current_vol_list.size(); i++) {
                Double position_vol = current_vol_list.get(i);
                Double ori_price = ori_price_list.get(i);
                margin += price * position_vol;
                profit += (price - ori_price) * position_vol;  // 逐笔盈亏
            }
            // 再对持仓&视图进行处理
            config.stockSummary.remove(symbol); // 直接删除该标的的视图
            config.stockPosition.remove(symbol); // 直接删除该标的的持有
        } else {  // 说明只有部分仓位可以被平

            // 先对视图进行批处理
            summary.total_vol -= max_vol;
            // 再对持仓进行处理
            for (int i = 0; i < current_vol_list.size(); i++) {
                Double posVol = current_vol_list.get(i);
                Double posPrice = ori_price_list.get(i);
                if (max_vol >= posVol) { // 当前订单全部平仓
                    margin += price * posVol;
                    profit += (price - posPrice) * posVol;
                    pos_list.remove(0); // FIFO Queue
                    max_vol -= posVol;
                } else { // 当前订单部分平仓
                    margin += price * max_vol;
                    profit += (price - posPrice) * max_vol;
                    pos_list.get(0).vol = posVol - max_vol; // FIFO Queue
                    break;
                }
            }

            // 更新标的持仓 & 视图
            config.stockPosition.put(symbol, pos_list);
            config.stockSummary.put(symbol, summary);
        }
        // 记录本次交易
        StockRecord R = new StockRecord("close", reason, config.currentDate, config.currentMinute, config.currentTimeStamp,
                symbol, price, record_vol, profit);
        config.stockRecord.add(R);

        // 结算
        config.profit += profit;
        config.stockProfit += profit;
        config.cash += margin;  // 股票交易中一开始付出的现金可以理解为100%保证金
        config.stockCash += margin;
    }

    public static void closeFuture(String order_type, String symbol, Double vol, Double price, String reason){
        /*
        期货合约平仓函数
        */
        int LS; // long:1, short:-1 -> 简化业务收益计算逻辑
        BackTestConfig config = BackTestConfig.getInstance(); // 获取BackTestConfig示例

        // 获取当前期货的仓位+持仓视图
        ArrayList<FuturePosition> pos_list;
//        LinkedHashMap<String, ArrayList<FuturePosition>> futurePos;
        FutureSummary summary;
        if (order_type.equals("long")){
            if (config.getFutureLongPosition().isEmpty()){
                return ;  // 说明当前没有期货多头持仓
            }
            if (!config.getFutureLongPosition().containsKey(symbol)){
                System.out.printf("当前期货%s没有多头持仓",  symbol);
                return ;
            }
            LS = 1;
            pos_list = config.futureLongPosition.get(symbol);
            summary = config.futureLongSummary.get(symbol);
        }
        else{
            if (config.getFutureShortPosition().isEmpty()){
                return ;  // 说明当前没有期货空头持仓
            }
            if (!config.getFutureShortPosition().containsKey(symbol)){
                System.out.printf("当前期货%s没有空头持仓",  symbol);
                return ;
            }
            LS = -1;
            pos_list = config.futureShortPosition.get(symbol);
            summary = config.futureShortSummary.get(symbol);
        }

        // 获取当前期货持仓的具体细节
        ArrayList<Double> current_vol_list = new ArrayList<>();  // 每个仓位对应的持仓数量
        ArrayList<Double> ori_price_list = new ArrayList<>();    // 每个仓位对应的持仓价格
        ArrayList<Double> pre_margin_list = new ArrayList<>();   // 每个仓位对应的保证金
        ArrayList<Integer> time_monitor_list = new ArrayList<>(); // 每个仓位对应的持仓时间
        ArrayList<Integer> hold_days_list = new ArrayList<>();   // 每个仓位对应的期货持仓时间监控状态情况
        for (FuturePosition pos : pos_list) {
            current_vol_list.add(pos.getVol());
            ori_price_list.add(pos.getPrice());
            pre_margin_list.add(pos.margin);
            time_monitor_list.add(pos.time_monitor);
            hold_days_list.add(pos.hold_days);
        }

        // 获取允许平仓的最大数量
        double current_vol;
        boolean state;
        if (time_monitor_list.contains(-2)){
            // 说明当前持仓队列中存在禁止卖出的标的
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
        double max_vol = Math.min(current_vol, vol); // 每次循环会改变max_vol的值，直到max_vol为=0
        double record_vol = max_vol;  // for record
        double profit = 0.0, settle_profit = 0.0, margin = 0.0;  // 该笔交易获得盈利(平仓盈亏)  // 该笔交易获得的定时盈亏(平仓价-昨结价) // 该笔交易收回的保证金

        if (max_vol >= current_vol && state){  // 说明都可以平仓
            // 逐笔计算盈亏
            for (int i = 0; i < current_vol_list.size(); i++) {
                Double position_vol = current_vol_list.get(i);
                Double ori_price = ori_price_list.get(i);
                Double pre_margin = pre_margin_list.get(i);
                Double pre_settle = pre_margin_list.get(i);
                int hold_days = hold_days_list.get(i);
                profit += (price - ori_price) * position_vol * LS;  // 逐笔盈亏
                if (hold_days == 0){  // TODO: 说明是日内平仓,没有持隔夜仓,需要添加对应的commission计算逻辑
                    settle_profit += (price - ori_price) * position_vol * LS;
                }else{ // TODO: 说明是隔日平仓,需要添加对应的commission计算逻辑
                    settle_profit += (price - pre_settle) * position_vol * LS;
                }
                margin += (pre_margin + settle_profit); // 收回的保证金
            }
            // 对持仓&视图进行处理
            if (order_type.equals("long")){
                config.futureLongPosition.remove(symbol);
                config.futureLongSummary.remove(symbol);
            }else{
                config.futureShortPosition.remove(symbol);
                config.futureShortSummary.remove(symbol);
            }
        }else{ // 说明只有部分仓位可以被平

            // 先对视图进行批处理
            summary.total_vol -= max_vol;
            // 再对持仓进行处理
            for (int i = 0; i < current_vol_list.size(); i++) {
                Double position_vol = current_vol_list.get(i);
                Double ori_price = ori_price_list.get(i);
                Double pre_margin = pre_margin_list.get(i);
                Double pre_settle = pre_margin_list.get(i);
                int hold_days = hold_days_list.get(i);

                if (max_vol>=position_vol){  // 当前订单全部平仓
                    profit += (price - ori_price) * position_vol * LS;
                    if (hold_days == 0){  // TODO: 说明是日内平仓,没有持隔夜仓,需要添加对应的commission计算逻辑
                        settle_profit += (price - ori_price) * position_vol * LS;
                    }else{ // TODO: 说明是隔日平仓,需要添加对应的commission计算逻辑
                        settle_profit += (price - pre_settle) * position_vol * LS;
                    }
                    margin += (pre_margin + settle_profit); // 该仓位收回的保证金 = 该仓位保证金 + 该仓位盯市盈亏
                    pos_list.remove(0);  // FIFO Queue
                    max_vol -= position_vol;
                }
                else{  // 当前订单部分平仓
                    profit += (price - ori_price) * max_vol * LS;
                    if (hold_days == 0){  // TODO: 说明是日内平仓,没有持隔夜仓,需要添加对应的commission计算逻辑
                        settle_profit += (price-ori_price) * max_vol * LS;
                    }else{ // TODO: 说明是隔日平仓,需要添加对应的commission计算逻辑
                        settle_profit += (price - pre_settle) * max_vol * LS;
                    }
                    margin += (pre_margin * (max_vol/vol) + settle_profit); // 该仓位收回的保证金 = 该仓位保证金 * (平仓数量/总持仓数量) + 该仓位盯市盈亏
                    break;  // max_vol == 0 -> 该标的的全部仓位平仓完毕
                }
            }
            // 更新标的持仓 & 视图
            if (order_type.equals("long")){
                config.futureLongPosition.put(symbol, pos_list);
                config.futureLongSummary.put(symbol, summary);
            }else{
                config.futureShortPosition.put(symbol, pos_list);
                config.futureShortSummary.put(symbol, summary);
            }
        }
        // 记录本次交易
        FutureRecord R = new FutureRecord("close", order_type, reason, config.currentDate, config.currentMinute, config.currentTimeStamp,
                symbol, price, record_vol, profit);
        config.futureRecord.add(R);

        // 结算
        config.profit += profit;                // 逐笔盈亏(平仓价-开仓价)
        config.futureProfit += profit;
        config.profitSettle += settle_profit;  // 盯市盈亏(平仓价-昨结价)
        // TODO: futureXXX
        config.cash += margin;                 // 保证金(pre_margin+盯市盈亏)
        config.futureCash += margin;
    }

//    public static void calculateFutureLongProfit_afterDay(){
//        // 盘后运行
//        // 1. 计算盯市盈亏
//        // 2. 更新仓位属性(settle & hold_days)
//
//        int LS = 1;
//        BackTestConfig config = BackTestConfig.getInstance(); // 获取BackTestConfig示例
//
//        if (config.getFutureLongPosition().isEmpty()){
//            return ; // 说明多头没有持仓, 不需要检测
//        }
//        HashMap<String, FutureInfo> totalInfo = config.getFutureInfoDict(); // 获取标的信息
//        FutureInfo info;
//        double settle_profit;  // 盯市盈亏
//        double settle,pre_settle; // 昨结价,今结价
//        for (String symbol: config.getFutureLongPosition().keySet()){
//            if (!totalInfo.containsKey(symbol)){
//                continue;
//            }
//            info = totalInfo.get(symbol);
//            settle = info.settle;
//            pre_settle = info.pre_settle;
//            int k = config.getFutureLongPosition().get(symbol).size();
//            for (int i=0; i<k; i++){
//                FuturePosition pos = config.getFutureLongPosition().get(symbol).get(i);
//                double vol = pos.getVol();
//                int hold_days = pos.hold_days;
//                if (hold_days >=1){  // 说明这个仓位不是第一天持仓
//                    settle_profit = (settle - pre_settle) * vol * LS;  // 第K天的盯市盈亏
//                }else{
//                    settle_profit = (settle - pos.price) * vol * LS; // 第1天的盯市盈亏
//                }
//                config.profitSettle += settle_profit;
//                // TODO: futureXXX
//                pos.margin += settle_profit;
//                pos.hold_days += 1;       // 更新持仓属性:持仓天数
//                pos.pre_settle = settle;   // 更新持仓属性:昨结算价
//                config.getFutureLongPosition().get(symbol).set(i, pos);
//            }
//        }
//    }

//    public static void calculateFutureShortProfit_afterDay(){
//        /*
//         * [收盘后运行]:
//         * 1.利用仓位中的hold_days, 对于第1天持仓/第K天的标的分别计算收益->profitSettle
//         * 2.更新未平仓合约的pre_settle为收盘后的settle, hold_days+=1
//         * */
//        int LS = -1;
//        BackTestConfig config = BackTestConfig.getInstance(); // 获取BackTestConfig示例
//
//        if (config.getFutureShortPosition().isEmpty()){
//            return ; // 说明空头没有持仓, 不需要检测
//        }
//        HashMap<String, FutureInfo> totalInfo = config.getFutureInfoDict(); // 获取标的信息
//        FutureInfo info;
//        double settle_profit;  // 盯市盈亏
//        double settle,pre_settle; // 昨结价,今结价
//        for (String symbol: config.getFutureShortPosition().keySet()){
//            if (!totalInfo.containsKey(symbol)){
//                continue;
//            }
//            info = totalInfo.get(symbol);
//            settle = info.settle;
//            pre_settle = info.pre_settle;
//            int k = config.getFutureShortPosition().get(symbol).size();
//            for (int i=0; i<k; i++){
//                FuturePosition pos = config.getFutureShortPosition().get(symbol).get(i);
//                double vol = pos.getVol();
//                int hold_days = pos.hold_days;
//                if (hold_days >=1){  // 说明这个仓位不是第一天持仓
//                    settle_profit = (settle - pre_settle) * vol * LS;  // 第K天的盯市盈亏
//                }else{
//                    settle_profit = (settle - pos.price) * vol * LS; // 第1天的盯市盈亏
//                }
//                config.profitSettle += settle_profit;
//                // TODO: futureXXX
//                pos.margin += settle_profit;
//                pos.hold_days += 1;       // 更新持仓属性:持仓天数
//                pos.pre_settle = settle;   // 更新持仓属性:昨结算价
//                config.getFutureShortPosition().get(symbol).set(i, pos);
//            }
//        }
//    }

//    public static void calculateFutureProfit_afterDay(){
//        /*
//        * [收盘后运行]:
//        * 1.更新未平仓合约的pre_settle为收盘后的settle
//        * 2.利用仓位中的hold_days, 对于第一天持仓的标的单独计算收益
//        * */
//        calculateFutureLongProfit_afterDay();  // 计算多头持仓
//        calculateFutureShortProfit_afterDay(); // 计算空头持仓
//    }

//    public static void futureAfterDay(){
//        /*
//         * [收盘后运行]:
//         * 1.利用仓位中的hold_days, 对于第1天持仓/第K天的标的分别计算收益->profitSettle
//         * 2.更新未平仓合约的pre_settle为收盘后的settle, hold_days+=1
//         * */
//        calculateFutureProfit_afterDay();
//    }

    public static void monitorStockPosition(boolean order_sequence){
        /*
        【柜台处理订单后运行,可重复运行】每日盘中运行,负责监控当前持仓是否满足限制平仓要求
        order_sequence=true 假设max_price先判断
        order_sequence=false 假设min_price先判断
        */

        // 获取当前配置实例
        BackTestConfig config = BackTestConfig.getInstance();
        LinkedHashMap<String, ArrayList<StockPosition>> stockPos = config.getStockPosition();
        if (stockPos.isEmpty()){
            return ;  // 当前没有持仓
        }

        LocalDate date = config.currentDate;
        Integer minute = config.currentMinute;
        LocalDateTime timestamp = config.currentTimeStamp;
        if (!config.stockKDict.containsKey(minute)){
            return ;
        }
        HashMap<String, StockBar> stock_k_dict = config.stockKDict.get(minute);
        HashMap<String, StockInfo> stock_info_dict = config.stockInfoDict;

        for (String symbol: stockPos.keySet()){
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

            ArrayList<StockPosition> positionList = stockPos.get(symbol);
            int i = 0;
            boolean static_permission = true;   // 这个标的是否允许静态平仓
            boolean dynamic_permission = true;  // 这个标的是否允许动态平仓
            /*
            * 这一部分的逻辑是：
            * 在一个FIFO的队列中，不允许出现后面的仓位触发而平仓的行为[因为平仓是FIFO,所以相当于平掉的是前面的仓位]
            * //TODO: 这里严格限制止盈止损会对当前仓位的所有订单进行平仓,但仍然需要考虑部分成交撮合带来的潜在影响
            * 所以,在设置单个仓位动态止盈止损的情况下,只能若当前仓位没有被平掉,那么后续仓位
            * */

            while (i < positionList.size()){
                // 若当前已经清空该股票的持仓, 进行保护
                if (!config.stockPosition.containsKey(symbol)){
                    i++;
                    continue;
                }

                // 这个标的的第i个仓位
                Position position = positionList.get(i);
                int time_monitor = position.getTime_monitor();
                if (time_monitor < 0){
                    i++;
                    continue;
                }

                // 获取持仓信息
                Double vol = position.getVol();  // 这个仓位的持仓数量
                LocalDateTime min_timestamp = position.getMin_timestamp();
                LocalDate min_date = min_timestamp.toLocalDate();
                LocalDateTime max_timestamp = position.getMax_timestamp();
                LocalDate max_date = max_timestamp.toLocalDate();

                // Step1. 给出时间维度基本判断(当天后续回测时间是否需要继续监视该订单)-持仓时间维度
                if (time_monitor == 0){  // 如果还没判断过 (time_monitor = 0)
                    // if (min_date.isEqual(date) || min_date.isAfter(date)){
                    if (min_date.isAfter(date)){
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
                // TODO: 这是一个冗余的逻辑,需要删除
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
