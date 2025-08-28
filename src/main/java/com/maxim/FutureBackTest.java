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

//import static com.maxim.service.DataLoader.getStockInfoFromJson;
//import static com.maxim.service.DataLoader.getStockKDataFromJson;


public class FutureBackTest {
    public static void main(String[] args) throws Exception{
        // 读取JSON文件内容
        String configPath = "D:\\Maxim\\BackTest\\JavaBackTest\\src\\main\\java\\com\\maxim\\backtest_config.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(configPath)));
        // Java单例设计模式, 获取全局配置项, 回测逻辑会实时修改里面的属性
        BackTestConfig config = BackTestConfig.getInstance(jsonContent);
        System.out.println(config.getProfit());

        // for (date in date_list){
        LocalDate tradeDate = LocalDate.of(2023, 2, 1);
        config.setCurrentDate(tradeDate);
        config.setCurrentDotDate(tradeDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")));
        config.setCurrentStrDate(tradeDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
    }
}
