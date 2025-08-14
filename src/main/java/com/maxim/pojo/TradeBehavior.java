package com.maxim.pojo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxim.pojo.BackTestConfig;
import com.maxim.pojo.order.StockOpenOrder;
import com.maxim.pojo.order.StockCloseOrder;
import com.xxdb.DBConnection;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.time.*;
import java.util.LinkedHashMap;

public class TradeBehavior {
    public static void orderOpenStock(String symbol, Double vol, Double price,
                                      Double static_profit, Double static_loss,
                                      Double dynamic_profit, Double dynamic_loss,
                                      LocalDateTime min_timestamp, LocalDateTime max_timestamp,
                                      LocalDateTime min_order_timestamp, LocalDateTime max_order_timestamp,
                                      Double commission, String reason, Boolean partialOrder){
        /*
        【盘中运行】股票订单发送至stock_counter,
        min_timestamp: 最短持仓时间(在该時刻之前不能平仓)
        max_timestamp: 最长持仓时间(在该時刻之前必须平仓)
        min_order_timestamp:　最早开始执行该order发送的时间(交易计划)
        max_order_timestamp: 最晚开始执行该order发送的时间

        如果不设置max_order_timestamp,
        直到回测结束每天都会尝试在min_order_date后发送该订单
        */
        // 获取配置实例
        BackTestConfig config = BackTestConfig.getInstance();

        // 设置默认时间戳值
        if (min_timestamp == null){
            min_timestamp = config.startDateTime;
        }
        if (max_timestamp == null){
            max_timestamp = config.endDateTime;
        }
        if (min_order_timestamp == null){
            min_order_timestamp = config.startDateTime;
        }
        if (max_order_timestamp == null){
            max_order_timestamp = config.endDateTime;
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

    public static void orderCloseStock(String symbol, Double vol, Double price,
                                       LocalDateTime min_order_timestamp,
                                       LocalDateTime max_order_timestamp,
                                       String reason, Boolean partialOrder){
        // 获取配置实例
        BackTestConfig config = BackTestConfig.getInstance();

        if (min_order_timestamp == null){
            min_order_timestamp = BackTestConfig.getInstance().startDateTime;
        }
        if (max_order_timestamp == null){
            max_order_timestamp = BackTestConfig.getInstance().endDateTime;
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
}

