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
        String configPath = "D:\\BackTest\\JavaBackTest\\src\\main\\java\\com\\maxim\\backtest_config.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(configPath)));
        // Java单例设计模式, 获取全局配置项, 回测逻辑会实时修改里面的属性
        BackTestConfig config = BackTestConfig.getInstance(jsonContent);
        System.out.println(config.getProfit());

        // for (date in date_list){
        LocalDate tradeDate = LocalDate.of(2023, 2, 1);
        config.setCurrentDate(tradeDate);
        config.setCurrentDotDate(tradeDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")));
        config.setCurrentStrDate(tradeDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        // 分钟频回测
        fromJson fj = new fromJson();
        Collection<LocalDate> dateList = List.of(LocalDate.of(2020, 1, 2),
                LocalDate.of(2020,1,3),
                LocalDate.of(2020,1,6));
        TreeMap<LocalDate, TreeMap<LocalTime, HashMap<String, StockBar>>> barMap =
                fj.JsonToJavaBeansByTime(dateList, barPath, StockBar.class);
        barMap.forEach(
                (date, timeMap) -> {
                    System.out.println(timeMap.keySet());
                }
        );
    }
}
