package com.maxim;
import com.maxim.pojo.BackTestConfig;
import com.maxim.pojo.CounterBehavior;
import com.maxim.pojo.info.StockInfo;
import com.maxim.pojo.kbar.StockBar;
import com.maxim.pojo.position.Position;
import com.maxim.pojo.TradeBehavior;
import com.maxim.service.Utils;
import com.maxim.service.Utils.*;
import com.maxim.pojo.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.maxim.service.DataLoader.getStockInfoFromJson;
import static com.maxim.service.DataLoader.getStockKDataFromJson;

public class StockBackTest {
    public static void main(String[] args) throws Exception{
        // 读取JSON文件内容
        String configPath = "D:\\Maxim\\BackTest\\JavaBackTest\\src\\main\\java\\com\\maxim\\backtest_config.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(configPath)));
        // Java单例设计模式, 获取全局配置项, 回测逻辑会实时修改里面的属性
        BackTestConfig config = BackTestConfig.getInstance(jsonContent);

        // for (date in date_list){
        LocalDate tradeDate = LocalDate.of(2023, 2, 1);
        config.setCurrentDate(tradeDate);
        config.setCurrentDotDate(tradeDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")));
        config.setCurrentStrDate(tradeDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        LinkedHashMap<Integer, HashMap<String, StockBar>> MinStockBars = getStockKDataFromJson(config.getStockCounterJson(), tradeDate, 3);
        config.setStockKDict(MinStockBars);
        HashMap<String, StockInfo> DailyStockInfo = getStockInfoFromJson(config.getStockInfoJson(), tradeDate, 3);
        config.setStockInfoDict(DailyStockInfo);

        // 获取当前日期对应的回测分钟
        // 这里先固定为240分钟
        Collection<Integer> minute_list = Utils.getMinuteList("SSE"); // 240分钟

        for (Integer minute: minute_list) {
            config.setCurrentMinute(minute);  // 931
            LocalDateTime timestamp = tradeDate.atTime( minute / 100, minute % 100, 0);
            config.setCurrentTimeStamp(timestamp);

            // 尝试进行下单
            if (minute == 930){
                Counter.orderOpenStock("000001", 200000.0, 14.9, null, null, null, null, null, null, null, null, null, "", false);
            }

            if (minute == 1300){
                Counter.orderCloseStock("000001", 200000.0, 14.65, null,LocalDateTime.of(2023, 2, 1, 15, 0, 0), "", false);
            }

            // 柜台逻辑
            Counter.processStockOrderStrict(0.005,0.005);
            Counter.monitorStockPosition(true);
            System.out.println("Minute:"+minute+"Cash"+config.getCash()+"Profit"+config.getProfit());

        }
        // 当天回测结束
        // 查看股票持仓
        for (String symbol : config.getStockPosition().keySet()) {
            Collection<Position> pos_list = config.getStockPosition().get(symbol);
            if (pos_list.size() > 1) {
                for (Position pos : pos_list) {
                    System.out.println(symbol + ":" + pos.getVol());
                }
            } else {
                System.out.println(symbol + ":" + config.getStockPosition().get(symbol).get(0).getVol());
            }
        }

    }
}