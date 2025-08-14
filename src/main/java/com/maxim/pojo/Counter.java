package com.maxim.pojo;
import com.maxim.pojo.kbar.StockBar;
import com.maxim.pojo.order.StockOpenOrder;
import com.maxim.pojo.order.StockOrder;

import java.time.*;
import java.util.*;

public class Counter extends CounterBehavior{

    // 回测中唯一用到的柜台类, 是所有交易对象类的子类, 能够调用所有方法
    public Counter(){
        super();
    }

    public static void processStockOrder(){
        /*
         柜台成交判断函数：
         【开仓/平仓order处理后运行,可重复运行】柜台仅通过价格判断是否当前订单能够执行,若能则执行,并在柜台删除该订单
        */
        // TODO: 获取InfoDict以减少判断次数

        // 获取当前配置实例
        BackTestConfig config = BackTestConfig.getInstance();
        LocalDate date = config.currentDate;
        Integer minute = config.currentMinute;
        LocalDateTime current_timestamp = config.currentTimeStamp;
        LinkedHashMap<Integer, StockOrder> stockCounter = config.getStockCounter();

        HashMap<String, StockBar> stock_k_dict;
        if (!config.stockKDict.containsKey(minute)){
            return ;
        }
        stock_k_dict = config.stockKDict.get(minute);

        // 收集需要删除的order_id
        Collection<Integer> delete_ids = new ArrayList<>();

        for (Integer order_id: stockCounter.keySet()){
            StockOrder order_obj = stockCounter.get(order_id);
            // 获取当前订单对象的属性值
            String order_state = order_obj.order_state;
            String symbol = order_obj.symbol;
            Double price = order_obj.price;
            Double vol = order_obj.vol;
            LocalDateTime min_order_timestamp = order_obj.min_order_timestamp;
            LocalDateTime max_order_timestamp = order_obj.max_order_timestamp;

            if (max_order_timestamp.isEqual(current_timestamp) || max_order_timestamp.isBefore(current_timestamp)){
                delete_ids.add(order_id); // 记录需要删除的订单id
                // config.stockCounter.remove(order_id); // 直接在全局配置实例项中删除对应的股票订单
                System.out.println("OrderNum"+order_id+"Bahavior"+order_state+"-symbol:"+symbol+"price"+price+"vol"+vol+"failed[Out of Timestamp]");
                
            } else if (current_timestamp.isEqual(min_order_timestamp) || current_timestamp.isAfter(min_order_timestamp)) {
                Double low = null;
                Double high = null;
                // 获取当前K线对象
                if (stock_k_dict.containsKey(symbol)){
                    StockBar kBar = stock_k_dict.get(symbol);
                    // 说明由当前分钟当前标的的K线数据
                    low = kBar.low;
                    high = kBar.high;
                }
                if (low!=null && high!=null){
                    // 说明这根K线上有该股票的数据
                    if (low<=price && price<=high){ // 说明可以成交
                        if (Objects.equals(order_state, "open")){
                            CounterBehavior.executeStock(symbol, price, vol,
                                    order_obj.static_profit, order_obj.static_loss,
                                    order_obj.dynamic_profit, order_obj.dynamic_loss,
                                    min_order_timestamp, max_order_timestamp,
                                    order_obj.reason);
                        } else if (Objects.equals(order_state, "close")) {
                            CounterBehavior.closeStock(symbol, price, vol, order_obj.reason);
                        }
                        delete_ids.add(order_id); // 记录需要删除的订单id
                        // config.stockCounter.remove(order_id); // 删除柜台的订单
                    }
                }
            }
        }
        // 统一删除订单id
        for (Integer order_id: delete_ids){
            config.stockCounter.remove(order_id);
        }

    }

    public static void processStockOrderStrict(Double open_share_threshold, Double close_share_threshold){
        /*
        更严格的柜台成交判断函数：
        当前开仓最多只能成交这根K线成交量的open_share_threshold倍
        当前平仓最多只能成交这根K线成交量的close_share_threshold倍
        */
        // TODO: 获取InfoDict以减少判断次数

        // 获取当前配置实例
        BackTestConfig config = BackTestConfig.getInstance();
        LocalDate date = config.currentDate;
        Integer minute = config.currentMinute;
        LocalDateTime current_timestamp = config.currentTimeStamp;
        LinkedHashMap<Integer, StockOrder> stockCounter = config.getStockCounter();

        HashMap<String, StockBar> stock_k_dict;
        if (!config.stockKDict.containsKey(minute)){
            return ;
        }
        stock_k_dict = config.stockKDict.get(minute);

        // 收集需要删除的order_id
        Collection<Integer> delete_ids = new ArrayList<>();
        for (Integer order_id: stockCounter.keySet()){
            StockOrder order_obj = stockCounter.get(order_id);
            // 获取当前订单对象的属性值
            String order_state = order_obj.order_state;
            String symbol = order_obj.symbol;
            Double price = order_obj.price;
            Double vol = order_obj.vol;
            LocalDateTime min_order_timestamp = order_obj.min_order_timestamp;
            LocalDateTime max_order_timestamp = order_obj.max_order_timestamp;
            if (max_order_timestamp.isEqual(current_timestamp) || max_order_timestamp.isBefore(current_timestamp)){
                delete_ids.add(order_id); // 记录需要删除的订单id
                System.out.println("OrderNum"+order_id+"Bahavior"+order_state+"-symbol:"+symbol+"price"+price+"vol"+vol+"failed[Out of Timestamp]");
            } else if (current_timestamp.isEqual(min_order_timestamp) || current_timestamp.isAfter(min_order_timestamp)){
                Double low = null;
                Double high = null;
                Double close = null;
                Double volume = null;
                // 获取当前K线对象
                if (stock_k_dict.containsKey(symbol)){
                    StockBar kBar = stock_k_dict.get(symbol);
                    // 说明由当前分钟当前标的的K线数据
                    low = kBar.low;
                    high = kBar.high;
                    close = kBar.close;
                    volume = kBar.volume;
                }

                if (low!=null && high!=null && close!=null && volume!=null){
                    if (order_obj.partialOrder){
                        price = close; // 说明是部分成交的订单, 第一次成交按照挂单价进行成交
                    }
                    if (low<=price && price<=high){
                        if (Objects.equals(order_state, "open")){ // 开仓订单
                            StockOpenOrder open_order = (StockOpenOrder) order_obj; // 强制类型转换为子类以获取更多属性
                            Double openVolThreshold = (open_share_threshold * volume);
                            if (vol <= openVolThreshold){
                                delete_ids.add(order_id);
                                // config.stockCounter.remove(order_id);  // 删除柜台的订单
                            }else{
                                config.stockCounter.get(order_id).vol -= openVolThreshold;
                                config.stockCounter.get(order_id).partialOrder = true; // 当前订单是拆单后的部分订单
                                vol = openVolThreshold;
                            }
                            if (vol > 0.0){ // TODO: 这里由于会出现那种空Bar, 导致这里乘上去是不能成交的, 所以需要加入判断
                                CounterBehavior.executeStock(symbol, price, vol,
                                        open_order.static_profit, open_order.static_loss,
                                        open_order.dynamic_profit, open_order.dynamic_loss,
                                        open_order.min_timestamp, open_order.max_timestamp,
                                        open_order.reason);
                            }
                        }else if(Objects.equals(order_state, "close")){ // 平仓订单
                            // StockCloseOrder close_order = (StockCloseOrder) order_obj; // 强制类型转换为子类以获取更多属性
                            Double closeVolThreshold = (close_share_threshold * volume);
                            if (vol <= closeVolThreshold){
                                delete_ids.add(order_id);
                                // config.stockCounter.remove(order_id); // 删除柜台的订单
                            }else{
                                config.stockCounter.get(order_id).vol -= closeVolThreshold;
                                config.stockCounter.get(order_id).partialOrder = true; // 当前订单是拆单后的部分订单
                                vol = closeVolThreshold;
                            }
                            // 这里因为目前StockCloseOrder的属性相比StockOrder没有更多, 所以不用强制类型转换，可以直接拿属性
                            if (vol > 0.0){
                                CounterBehavior.closeStock(symbol, price, vol, order_obj.reason);
                            }
                        }
                    }
                }

            }

        }

        // 统一删除订单id
        for (Integer order_id: delete_ids){
            config.stockCounter.remove(order_id);
        }

    }

    public static void processStockOrderMySelf(){
        // 自定义订单成交判断函数
    }

}
