package com.maxim.pojo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxim.pojo.BackTestConfig;
import com.maxim.pojo.emun.OrderDirection;
import com.maxim.pojo.info.FutureInfo;
import com.maxim.pojo.order.*;
import com.xxdb.DBConnection;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.time.*;
import java.util.LinkedHashMap;

public class TradeBehavior {
    public static void orderOpenStock(String symbol, Integer vol, Double price,
                                      Double static_profit, Double static_loss,
                                      Double dynamic_profit, Double dynamic_loss,
                                      LocalDateTime min_timestamp, LocalDateTime max_timestamp,
                                      LocalDateTime min_order_timestamp, LocalDateTime max_order_timestamp,
                                      Double commission, String reason, Boolean partialOrder){
        /*
        【盘中运行】股票订单发送至stock_counter
        symbol: 股票代码
        vol: 订单数量
        price: 订单价格
        static_profit: 静态止盈比例
        static_loss: 静态止损比例
        dynamic_profit: 动态止盈比例
        dynamic_loss: 动态止损比例
        min_timestamp: 最短持仓时间(在该時刻之前不能平仓)
        max_timestamp: 最长持仓时间(在该時刻之前必须平仓)
        min_order_timestamp: 最早开始执行该order发送的时间(交易计划)
        max_order_timestamp: 最晚开始执行该order发送的时间
        commission: 手续费
        reason: 订单原因
        partialOrder: 是否为部分订单, 默认为false(这个用户级别的都是false, 系统级别的是true) // TODO: 后续取消该参数的暴露

        如果不设置max_order_timestamp,
        直到回测结束每根Bar上都会尝试在min_order_date后发送该订单
        */
        // 获取配置实例
        BackTestConfig config = BackTestConfig.getInstance();

        // 设置默认时间戳值
        if (min_timestamp == null){
            min_timestamp = config.startTimeStamp;
        }
        if (max_timestamp == null){
            max_timestamp = config.endTimeStamp;
        }
        if (min_order_timestamp == null){
            min_order_timestamp = config.startTimeStamp;
        }
        if (max_order_timestamp == null){
            max_order_timestamp = config.endTimeStamp;
        }

        // 增加订单编号
        int orderNum;
        synchronized(config) {
            config.orderNum++;
            orderNum = config.orderNum;
        }

        // 创建订单对象
        StockOpenOrder order = new StockOpenOrder(
                symbol,
                vol,
                price,
                config.currentDate,
                config.currentTimeStamp,
                min_timestamp,
                max_timestamp,
                min_order_timestamp,
                max_order_timestamp,
                static_profit,
                static_loss,
                dynamic_profit,
                dynamic_loss,
                commission,
                reason,
                partialOrder
        );

        // 将订单添加到柜台和记录中
        synchronized(config) {
            config.stockCounter.put(orderNum, order);
            config.stockOrderRecord.add(order);
        }
    }

    public static void orderCloseStock(String symbol, Integer vol, Double price,
                                       LocalDateTime min_order_timestamp,
                                       LocalDateTime max_order_timestamp,
                                       String reason, Boolean partialOrder){
        // 获取配置实例
        BackTestConfig config = BackTestConfig.getInstance();

        if (min_order_timestamp == null){
            min_order_timestamp = BackTestConfig.getInstance().startTimeStamp;
        }
        if (max_order_timestamp == null){
            max_order_timestamp = BackTestConfig.getInstance().endTimeStamp;
        }
        // 增加订单编号
        int orderNum;
        synchronized(config) {
            config.orderNum++;
            orderNum = config.orderNum;
        }

        StockCloseOrder order = new StockCloseOrder(
                symbol,
                vol,
                price,
                config.currentDate,
                config.currentTimeStamp,
                min_order_timestamp,
                max_order_timestamp,
                reason,
                partialOrder
        );

        // 将订单添加到柜台和记录中
        synchronized(config) {
            config.stockCounter.put(orderNum, order);
            config.stockOrderRecord.add(order);
        }
    }

    public static void orderOpenFuture(OrderDirection direction, String symbol, Integer vol, Double price,
                                       Double static_profit, Double static_loss,
                                       Double dynamic_profit, Double dynamic_loss,
                                       LocalDateTime min_timestamp, LocalDateTime max_timestamp,
                                       LocalDateTime min_order_timestamp, LocalDateTime max_order_timestamp,
                                       Double commission, String reason, Boolean partialOrder){
        /*
        【盘中运行】期货订单发送至future_counter
        direction: 订单方向
        symbol: 期货代码
        vol: 订单数量
        price: 订单价格
        static_profit: 静态止盈比例
        static_loss: 静态止损比例
        dynamic_profit: 动态止盈比例
        dynamic_loss: 动态止损比例
        min_timestamp: 最短持仓时间(在该時刻之前不能平仓)
        max_timestamp: 最长持仓时间(在该時刻之前必须平仓)
        min_order_timestamp: 最早开始执行该order发送的时间(交易计划)
        max_order_timestamp: 最晚开始执行该order发送的时间
        commission: 手续费
        reason: 订单原因
        partialOrder: 是否为部分订单, 默认为false(这个用户级别的都是false, 系统级别的是true)  // TODO: 后续取消该参数的暴露
         */

        // 获取配置实例
        BackTestConfig config = BackTestConfig.getInstance();

        // 设置默认时间戳值
        if (min_timestamp == null){
            min_timestamp = config.startTimeStamp;
        }
        if (max_timestamp == null){
            max_timestamp = config.endTimeStamp;
        }
        if (min_order_timestamp == null){
            min_order_timestamp = config.startTimeStamp;
        }
        if (max_order_timestamp == null){
            max_order_timestamp = config.endTimeStamp;
        }

        // 增加订单编号
        int orderNum;
        synchronized(config) {
            config.orderNum++;
            orderNum = config.orderNum;
        }

        // 创建订单对象
        FutureOpenOrder order = new FutureOpenOrder(
                direction,
                symbol,
                vol,
                price,
                config.currentDate,
                config.currentTimeStamp,
                min_timestamp,
                max_timestamp,
                min_order_timestamp,
                max_order_timestamp,
                static_profit,
                static_loss,
                dynamic_profit,
                dynamic_loss,
                commission,
                reason,
                partialOrder
        );

        // 将订单添加到柜台和记录中
        synchronized(config) {
            config.futureCounter.put(orderNum, order);
            config.futureOrderRecord.add(order);
        }
    }

    public static void orderCloseFuture(OrderDirection direction, String symbol, Integer vol, Double price,
                                        LocalDateTime min_order_timestamp,
                                        LocalDateTime max_order_timestamp,
                                        String reason, Boolean partialOrder){
        // 获取配置实例
        BackTestConfig config = BackTestConfig.getInstance();

        if (min_order_timestamp == null){
            min_order_timestamp = BackTestConfig.getInstance().startTimeStamp;
        }
        if (max_order_timestamp == null){
            max_order_timestamp = BackTestConfig.getInstance().endTimeStamp;
        }
        // 增加订单编号
        int orderNum;
        synchronized(config) {
            config.orderNum++;
            orderNum = config.orderNum;
        }

        FutureCloseOrder order = new FutureCloseOrder(
                direction,
                symbol,
                vol,
                price,
                config.currentDate,
                config.currentTimeStamp,
                min_order_timestamp,
                max_order_timestamp,
                reason,
                partialOrder
        );

        // 将订单添加到柜台和记录中
        synchronized(config) {
            config.futureCounter.put(orderNum, order);
            config.futureOrderRecord.add(order);
        }
    }
}

