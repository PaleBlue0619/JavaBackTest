package com.maxim.service.getdata;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.maxim.pojo.kbar.KBar;
import com.maxim.service.JedisDBPool;
import com.xxdb.DBConnection;
import com.xxdb.data.BasicTable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class fromDolphinDBTest {
    public static void main(String[] args) throws IOException {
        // 测试类
        String HOST = "172.16.0.184";
        int PORT = 8001;
        String USERNAME = "maxim";
        String PASSWORD = "dyJmoc-tiznem-1figgu";
        DBConnection conn = new DBConnection();
        conn.connect(HOST, PORT, USERNAME, PASSWORD);
        conn.login(USERNAME, PASSWORD, true);

        fromDolphinDB test = new fromDolphinDB(conn, "dfs://MinKDB", "Min1K", 4);
        String[] symbol_list = new String[]{"000001.SZ", "000002.SZ", "000004.SZ", "000005.SZ", "000006.SZ", "000007.SZ", "000008.SZ", "000009.SZ", "000010.SZ", "000011.SZ", "000012.SZ", "000013.SZ", "000014.SZ", "000015.SZ", "000016.SZ", "000017.SZ", "000018.SZ"};
        HashMap<String, String> transMap = new HashMap<>();
        List<String> featureName = Arrays.asList(new String[]{"code","tradeDate","tradeTime","open", "high", "low", "close","volume"});
        List<String> structName = Arrays.asList(new String[]{"symbol","tradeDate","tradeTime","open", "high", "low", "close","volume"});
        for (int i=0; i<featureName.size(); i++){
            transMap.put(featureName.get(i), structName.get(i));
        }

        // toBasicTable-1, 直接将一天的所有数据从DolphinDB取出，转成BasicTable的集合
        ConcurrentHashMap<LocalDate, BasicTable> dataMap1 = test.toBasicTable(
                LocalDate.parse("2023.02.01", DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                LocalDate.parse("2023.02.03", DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                List.of(symbol_list),
                "tradeDate", false,"code", "tradeTime","open", "high", "low", "close", "volume");

        // toBasicTable2, 将一天的所有数据按照标的进行分组, 转成BasicTable的集合
        ConcurrentHashMap<LocalDate, HashMap<String, BasicTable>> dataMap2 = test.toBasicTableBySymbol(
                LocalDate.parse("2023.02.01", DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                LocalDate.parse("2023.02.03", DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                List.of(symbol_list),
                "tradeDate", false,"code", "tradeTime","open", "high", "low", "close", "volume");

        // toBasicTable3, 将一天所有数据按照时间进行分组, 转成BasicTable的集合
        ConcurrentHashMap<LocalDate, TreeMap<LocalTime, BasicTable>> dataMap3 = test.toBasicTableByTime(
                LocalDate.parse("2023.02.01", DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                LocalDate.parse("2023.02.03", DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                List.of(symbol_list),
                "tradeDate", "tradeTime","code", "tradeTime","open", "high", "low", "close", "volume");
        for (LocalDate date: dataMap3.keySet()){
            TreeMap<LocalTime, BasicTable> timeMap = dataMap3.get(date);
            System.out.println(timeMap.keySet());
        }

        // toJavaBean-1
        ConcurrentHashMap<LocalDate, List<KBar>> beanMap = test.toJavaBeans(dataMap1, KBar.class, transMap);
        for (LocalDate date: beanMap.keySet()){
            List<KBar> kbarList = beanMap.get(date);
            for (KBar kbar: kbarList){
                System.out.println(kbar.toString());
            }
        }

        // toRedis-1
        JedisPool jedisPool = new JedisPool();
        toRedis toRedis = new toRedis();
        toRedis.JavaBeansToRedis(jedisPool, beanMap, "KBar", true);

        // fromRedis-1
        fromRedis fromRedis = new fromRedis();
        TreeMap<LocalDate, List<KBar>> beanMap1 = fromRedis.RedisToJavaBeans(jedisPool,
                List.of(LocalDate.parse("2023.02.01", DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                        LocalDate.parse("2023.02.02", DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                        LocalDate.parse("2023.02.03", DateTimeFormatter.ofPattern("yyyy.MM.dd"))),
                "KBar", KBar.class);
        for (LocalDate date: beanMap1.keySet()){
            // 直接从JSON字符串数组转换为对应的对象
            List<KBar> kBarList = JSON.parseArray(beanMap1.get(date).toString(), KBar.class);
            for (KBar kbar: kBarList){
                System.out.println("open:"+kbar.open+" close:"+kbar.close+" volume:"+kbar.volume);
            }
        }


    }


}
