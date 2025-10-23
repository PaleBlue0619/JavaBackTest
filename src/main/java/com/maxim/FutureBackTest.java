package com.maxim;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.maxim.pojo.BackTestConfig;
import com.maxim.pojo.CounterBehavior;
import com.maxim.pojo.emun.OrderDirection;
import com.maxim.pojo.info.StockInfo;
import com.maxim.pojo.kbar.StockBar;
import com.maxim.pojo.position.Position;
import com.maxim.pojo.TradeBehavior;
import com.maxim.service.Utils;
import com.maxim.service.Utils.*;
import com.maxim.pojo.*;
import com.maxim.service.getdata.fromJson;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

//import static com.maxim.service.DataLoader.getStockInfoFromJson;
//import static com.maxim.service.DataLoader.getStockKDataFromJson;


public class FutureBackTest {
    public static void main(String[] args) throws Exception{
        // 读取JSON文件内容
        String barPath = "D:\\BackTest\\JavaBackTest\\data\\stock_cn\\kbar";
        String infoPath = "D:\\BackTest\\JavaBackTest\\data\\stock_cn\\info";
        String configPath = "D:\\BackTest\\JavaBackTest\\src\\main\\java\\com\\maxim\\backtest_config.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(configPath)));
        // Java单例设计模式, 获取全局配置项, 回测逻辑会实时修改里面的属性
        BackTestConfig config = BackTestConfig.getInstance(jsonContent);
        System.out.println(config.getProfit());

        // 分钟频回测
        fromJson fj = new fromJson(); // JSON->JavaBeans解析类
        Collection<LocalDate> dateList = List.of(
                LocalDate.of(2020, 1, 2),
                LocalDate.of(2020,1,3),
                LocalDate.of(2020,1,6)
                );
        // 一次性读取所有数据至内存
        TreeMap<LocalDate, TreeMap<LocalTime, HashMap<String, StockBar>>> barMap =
                fj.JsonToJavaBeansByTime(dateList, barPath, StockBar.class);
        TreeMap<LocalDate, HashMap<String, StockInfo>> infoMap =
                fj.JsonToJavaBeansBySymbol(dateList, infoPath, StockInfo.class);
        System.out.println(infoMap.containsKey(LocalDate.of(2020,1,2)));

        for (LocalDate tradeDate : dateList){ // for - loop
            config.setCurrentDate(tradeDate); // 固定配置项
            config.setStockKDict(barMap.get(tradeDate));
            config.setStockInfoDict(infoMap.get(tradeDate));

            for (LocalTime tradeTime: Utils.getMinuteList("SSE")){
                config.setCurrentMinute(tradeTime); // 固定配置项
                Counter.executeStock("000001.SZ", 166500.0, 100,
                        0.03, 0.03, 0.03, 0.03,
                        null, LocalDateTime.of(tradeDate.getYear(), tradeDate.getMonth(), tradeDate.getDayOfMonth(),
                                tradeTime.getHour(), tradeTime.getMinute(), tradeTime.getSecond()), "");
                // 固定回调函数
                Counter.monitorStockPosition(true);
                Counter.monitorFuturePosition(OrderDirection.LONG, true);
                Counter.monitorFuturePosition(OrderDirection.SHORT, true);
                Counter.processStockOrderStrict(0.05, 0.05);
                Counter.afterBarStock();
                Counter.afterBarFuture();
            } // 固定回调函数
            Counter.afterDayStock();
            Counter.afterDayFuture();
            System.out.println("Day:"+tradeDate+"Profit"+config.getProfit());
        }
    }
}
