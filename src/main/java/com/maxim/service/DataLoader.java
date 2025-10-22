package com.maxim.service;
import com.maxim.pojo.emun.AssetType;
import com.maxim.pojo.emun.DataFreq;
import com.maxim.pojo.info.FutureInfo;
import com.maxim.pojo.info.StockInfo;
import com.maxim.pojo.kbar.FutureBar;
import com.maxim.pojo.kbar.FutureBarDay;
import com.maxim.pojo.kbar.StockBar; // 反序列化为StockBar对象
//import com.maxim.service.struct.StockKBarStruct;
//import com.maxim.service.struct.StockInfoStruct;
import com.maxim.pojo.kbar.StockBarDay;
import com.xxdb.DBConnection;
import com.xxdb.data.*;

import com.maxim.service.getdata.fromDolphinDB;
import com.maxim.service.savedata.toJson;
import java.io.IOException;
import java.lang.Void;
import java.nio.file.Files;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.io.File;
import java.io.FileWriter;
import redis.clients.jedis.Jedis;
import com.alibaba.fastjson2.JSONObject;

public class DataLoader {
    // 连接DolphinDB并获取结构体数据对象, 返回JSON格式
    String HOST;
    Integer PORT;
    String USERNAME;
    String PASSWORD;
    Integer ThreadCount; // 线程数量

    public DataLoader(String HOST, Integer PORT, String USERNAME, String PASSWORD, Integer ThreadCount){
        this.HOST = HOST;
        this.PORT = PORT;
        this.USERNAME = USERNAME;
        this.PASSWORD = PASSWORD;
        this.ThreadCount = ThreadCount;
    }

    public void KBarToJson(AssetType assetType, DataFreq dataFreq, String dbName, String tbName, String dateCol, String timeCol, String symbolCol,
                                HashMap<String, String> transMap,
                                String savePath, boolean dropFilePath,
                                LocalDate start_date, LocalDate end_date,
                                Collection<String> symbol_list, String...featureCols) throws IOException {
        DBConnection conn = new DBConnection();
        conn.connect(HOST, PORT);
        conn.login(USERNAME, PASSWORD, false);
        fromDolphinDB fromDB = new fromDolphinDB(conn, dbName, tbName, ThreadCount);
        toJson js = new toJson();
        switch (dataFreq){
            case DAY:
                ConcurrentHashMap<LocalDate, HashMap<String, BasicTable>> dayMap = fromDB.toBasicTableBySymbol(
                        start_date, end_date, symbol_list, dateCol, timeCol, false, symbolCol, featureCols);
                switch (assetType){
                    case STOCK:
                        ConcurrentHashMap<LocalDate, HashMap<String, StockBarDay>> stockDayBars =
                                fromDB.toJavaBeanBySymbol(dayMap, symbolCol, StockBarDay.class, transMap);
                        js.JavaBeanBySymbolToJson(stockDayBars, savePath, dropFilePath);
                        System.out.println("Stock DayKBar Beans saved");
                        break;
                    case FUTURE:
                        ConcurrentHashMap<LocalDate, HashMap<String, FutureBarDay>> futureDayBars =
                                fromDB.toJavaBeanBySymbol(dayMap, symbolCol, FutureBarDay.class, transMap);
                        js.JavaBeanBySymbolToJson(futureDayBars, savePath, dropFilePath);
                        System.out.println("Future DayKBar Beans saved");
                        break;
                }
                break;

            case MINUTE:
                ConcurrentHashMap<LocalDate, TreeMap<LocalTime,BasicTable>> minMap = fromDB.toBasicTableByTime(
                        start_date, end_date, symbol_list, dateCol, timeCol, symbolCol, featureCols);
                switch (assetType){
                    case STOCK:
                        ConcurrentHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, StockBar>>> stockMinBars =
                                fromDB.toJavaBeanByTime(minMap, symbolCol, StockBar.class, transMap);
                        js.JavaBeanByTimeToJson(stockMinBars, savePath, dropFilePath);
                        System.out.println("Stock MinKBar Beans saved");
                        break;
                    case FUTURE:
                        ConcurrentHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, FutureBar>>> futureMinBars =
                                fromDB.toJavaBeanByTime(minMap, symbolCol, FutureBar.class, transMap);
                        js.JavaBeanByTimeToJson(futureMinBars, savePath, dropFilePath);
                        System.out.println("Future MinKBar Beans saved");
                        break;
                }
                break;
        }
    }

    public void InfoToJson(AssetType assetType, String dbName, String tbName, String dateCol, String symbolCol,
                           HashMap<String, String> transMap,
                           String savePath, boolean dropFilePath,
                           LocalDate start_date, LocalDate end_date,
                           Collection<String> symbol_list, String...featureCols) throws IOException {
        DBConnection conn = new DBConnection();
        conn.connect(HOST, PORT);
        conn.login(USERNAME, PASSWORD, false);
        fromDolphinDB fromDB = new fromDolphinDB(conn, dbName, tbName, ThreadCount);
        toJson js = new toJson();
        ConcurrentHashMap<LocalDate, HashMap<String, BasicTable>> dayMap = fromDB.toBasicTableBySymbol(
                start_date, end_date, symbol_list, dateCol, null, false, symbolCol, featureCols);
            switch (assetType){
                case STOCK:
                    ConcurrentHashMap<LocalDate, HashMap<String, StockInfo>> stockDayInfo =
                            fromDB.toJavaBeanBySymbol(dayMap, symbolCol, StockInfo.class, transMap);
                    js.JavaBeanBySymbolToJson(stockDayInfo, savePath, dropFilePath);
                    System.out.println("Stock DayInfo Beans saved");
                    break;
                case FUTURE:
                    ConcurrentHashMap<LocalDate, HashMap<String, FutureInfo>> futureDayInfo =
                                fromDB.toJavaBeanBySymbol(dayMap, symbolCol, FutureInfo.class, transMap);
                    js.JavaBeanBySymbolToJson(futureDayInfo, savePath, dropFilePath);
                    System.out.println("Future DayInfo Beans saved");
                    break;
                }
    }

    public void KBarToJsonAsync(AssetType assetType, DataFreq dataFreq, String dbName, String tbName, String dateCol, String timeCol, String symbolCol,
                                HashMap<String, String> transMap,
                                String savePath, boolean dropFilePath,
                                LocalDate start_date, LocalDate end_date,
                                Collection<String> symbol_list, String...featureCols) throws IOException {
        DBConnection conn = new DBConnection();
        conn.connect(HOST, PORT);
        conn.login(USERNAME, PASSWORD, false);
        fromDolphinDB fromDB = new fromDolphinDB(conn, dbName, tbName, ThreadCount);
        toJson js = new toJson();

        // 生成日期列表
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate current = start_date;
        while (!current.isAfter(end_date)) {
            dateList.add(current);
            current = current.plusDays(1);
        }

        // 创建线程池控制并发
        ExecutorService executor = Executors.newFixedThreadPool(ThreadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 为每个日期创建异步任务
        for (LocalDate tradeDate : dateList) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    switch (dataFreq) {
                        case DAY:
                            ConcurrentHashMap<LocalDate, HashMap<String, BasicTable>> dayMap = fromDB.toBasicTableBySymbol(
                                    tradeDate, tradeDate, symbol_list, dateCol, timeCol, false, symbolCol, featureCols);
                            switch (assetType) {
                                case STOCK:
                                    ConcurrentHashMap<LocalDate, HashMap<String, StockBarDay>> stockDayBars =
                                            fromDB.toJavaBeanBySymbol(dayMap, symbolCol, StockBarDay.class, transMap);
                                    js.JavaBeanBySymbolToJson(stockDayBars, savePath, dropFilePath);
                                    System.out.println("Stock DayKBar Beans saved");
                                    break;
                                case FUTURE:
                                    ConcurrentHashMap<LocalDate, HashMap<String, FutureBarDay>> futureDayBars =
                                            fromDB.toJavaBeanBySymbol(dayMap, symbolCol, FutureBarDay.class, transMap);
                                    js.JavaBeanBySymbolToJson(futureDayBars, savePath, dropFilePath);
                                    System.out.println("Future DayKBar Beans saved");
                                    break;
                            }
                            break;

                        case MINUTE:
                            ConcurrentHashMap<LocalDate, TreeMap<LocalTime, BasicTable>> minMap = fromDB.toBasicTableByTime(
                                    tradeDate, tradeDate, symbol_list, dateCol, timeCol, symbolCol, featureCols);
                            switch (assetType) {
                                case STOCK:
                                    ConcurrentHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, StockBar>>> stockMinBars =
                                            fromDB.toJavaBeanByTime(minMap, symbolCol, StockBar.class, transMap);
                                    js.JavaBeanByTimeToJson(stockMinBars, savePath, dropFilePath);
                                    System.out.println("Stock MinKBar Beans saved");
                                    break;
                                case FUTURE:
                                    ConcurrentHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, FutureBar>>> futureMinBars =
                                            fromDB.toJavaBeanByTime(minMap, symbolCol, FutureBar.class, transMap);
                                    js.JavaBeanByTimeToJson(futureMinBars, savePath, dropFilePath);
                                    System.out.println("Future MinKBar Beans saved");
                                    break;
                            }
                            break;
                        }
                }
                catch (Exception e) {
                    System.err.println("Error processing date: " + tradeDate);
                    e.printStackTrace();
                }
            }, executor);

            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 关闭线程池
        executor.shutdown();
    }

    public void InfoToJsonAsync(AssetType assetType, String dbName, String tbName, String dateCol, String symbolCol,
                                HashMap<String, String> transMap,
                                String savePath, boolean dropFilePath,
                                LocalDate start_date, LocalDate end_date,
                                Collection<String> symbol_list, String...featureCols) throws IOException {
        DBConnection conn = new DBConnection();
        conn.connect(HOST, PORT);
        conn.login(USERNAME, PASSWORD, false);
        fromDolphinDB fromDB = new fromDolphinDB(conn, dbName, tbName, ThreadCount);
        toJson js = new toJson();

        // 生成日期列表
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate current = start_date;
        while (!current.isAfter(end_date)) {
            dateList.add(current);
            current = current.plusDays(1);
        }

        // 创建线程池控制并发
        ExecutorService executor = Executors.newFixedThreadPool(ThreadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 为每个日期创建异步任务
        for (LocalDate tradeDate : dateList) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    ConcurrentHashMap<LocalDate, HashMap<String, BasicTable>> dayMap = fromDB.toBasicTableBySymbol(tradeDate, tradeDate, symbol_list, dateCol, null, false, symbolCol, featureCols);
                    switch (assetType) {
                        case STOCK:
                            ConcurrentHashMap<LocalDate, HashMap<String, StockInfo>> stockDayInfo =
                                    fromDB.toJavaBeanBySymbol(dayMap, symbolCol, StockInfo.class, transMap);
                            js.JavaBeanBySymbolToJson(stockDayInfo, savePath, dropFilePath);
                            System.out.println("Stock DayInfo Beans saved");
                            break;
                        case FUTURE:
                            ConcurrentHashMap<LocalDate, HashMap<String, FutureInfo>> futureDayInfo =
                                    fromDB.toJavaBeanBySymbol(dayMap, symbolCol, FutureBarDay.class, transMap);
                            js.JavaBeanBySymbolToJson(futureDayInfo, savePath, dropFilePath);
                            System.out.println("Future DayKBar Beans saved");
                            break;
                    }
                }
                catch (Exception e) {
                    System.err.println("Error processing date: " + tradeDate);
                    e.printStackTrace();
                }
            }, executor);
            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 关闭线程池
        executor.shutdown();
    }
}