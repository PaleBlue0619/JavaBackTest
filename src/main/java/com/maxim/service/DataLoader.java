package com.maxim.service;
import com.maxim.pojo.info.StockInfo;
import com.maxim.pojo.kbar.StockBar; // 反序列化为StockBar对象
import com.xxdb.DBConnection;
import com.xxdb.data.*;

import java.io.IOException;
import java.lang.Void;
import java.nio.file.Files;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.io.File;
import java.io.FileWriter;
import com.alibaba.fastjson2.JSONObject;

public class DataLoader {
    // 连接DolphinDB并获取结构体数据对象, 返回JSON格式
    protected String HOST;
    protected Integer PORT;
    protected String USERNAME;
    protected String PASSWORD;
    protected Integer ThreadCount; // 线程数量

    public static ConcurrentHashMap<LocalDate, BasicTable> getStockInfo(DBConnection conn, String DBName, String TBName, String savePath,
                                                                        String start_date, String end_date, StockInfoStruct struct, ArrayList<String> symbol_list) throws IOException {
        /*
        TBName: 日频KBar表
        start_date：like 2020.01.01
        end_date：like 2020.01.02
        */
        // 多线程从DolphinDB数据库中取数，返回BasicTable
        // 转换字符串为DolphinDB List str
        String symbol_list_str = Utils.arrayToDolphinDBString(symbol_list);
        System.out.println("symbolList: " + symbol_list_str);

        // 获取所有时间
        BasicDateVector date_list = (BasicDateVector) conn.run("""
                t = select count(*) as count from loadTable("%s", "%s") where %s between date(%s) and date(%s) group by %s order by %s; 
                exec %s from t
                """.formatted(DBName, TBName, struct.dateCol, start_date, end_date, struct.dateCol, struct.dateCol, struct.dateCol));
        System.out.println("dateList: " + date_list.getString());

        // 提前获取所有股票的startDate与endDate, 存储在两个HashMap中
        HashMap<String, LocalDate> startDateMap = new HashMap<>();
        HashMap<String, LocalDate> endDateMap = new HashMap<>();

        // 获取每个股票的startDate与endDate
        BasicTable date_info = (BasicTable) conn.run("""
                select first(%s) as startDate, last(%s) as endDate from loadTable("%s", "%s") where %s between date(%s) and date(%s) group by %s as symbol;
                """.formatted(struct.dateCol, struct.dateCol, DBName, TBName, struct.dateCol, start_date, end_date, struct.symbolCol));
        BasicDateVector startDate_list = (BasicDateVector) date_info.getColumn("startDate");
        BasicDateVector endDate_list = (BasicDateVector) date_info.getColumn("endDate");
        for (int i=0; i<date_info.rows(); i++){
            String symbol = date_info.getColumn("symbol").getString(i);
            LocalDate symbolStartDate = startDate_list.getDate(i);
            LocalDate symbolEndDate = endDate_list.getDate(i);
            startDateMap.put(symbol, symbolStartDate);
            endDateMap.put(symbol, symbolEndDate);
        }

        // 创建多线程结果接收集合
        ConcurrentHashMap<LocalDate, BasicTable> resultMap = new ConcurrentHashMap<>();
        Collection<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i=0; i<date_list.rows(); i++) {
            LocalDate tradeDate = date_list.getDate(i);
            String tradeDateStr = tradeDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")); // 2020.01.01
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String script = """
                    select %s,%s,
                    %s,%s,%s,%s from loadTable("%s","%s") where %s == date(%s)
                    and %s in %s
                    """.formatted(struct.symbolCol, struct.dateCol,
                            struct.openCol, struct.highCol, struct.lowCol, struct.closeCol,
                            DBName, TBName, struct.dateCol, tradeDateStr, struct.symbolCol, symbol_list_str);
                    System.out.println(script);
                    BasicTable data = (BasicTable) conn.run(script,4,4);

                    // 获取当前symbolCol, 转化为List
                    // 将symbol列转换为List<String>
                    List<String> symbolList = new ArrayList<>(data.rows());
                    for (int j = 0; j < data.rows(); j++) {
                        String sym = data.getColumn(struct.symbolCol).get(j).toString();
                        symbolList.add(sym);
                    }

                    // 使用Stream映射获取对应的startDate & endDate
                    List<LocalDate> startDatelist = symbolList.stream()
                            .map(startDateMap::get)
                            .toList();
                    List<LocalDate> endDatelist = symbolList.stream()
                            .map(endDateMap::get)
                            .toList();
                    BasicDateVector startDateVector = new BasicDateVector(0);
                    BasicDateVector endDateVector = new BasicDateVector(0);

                    for (int j = 0; j < startDatelist.size(); j++) {
                        startDateVector.add(com.xxdb.data.Utils.countDays(startDatelist.get(j)));
                        endDateVector.add(com.xxdb.data.Utils.countDays(endDatelist.get(j)));
                    }
                    // 添加StartDate与EndDate两列
                    data.addColumn("startDate", startDateVector);
                    data.addColumn("endDate", endDateVector);

                    // 写入到本地Json文件(日频Kbar / 分钟频Kbar)
                    saveStockInfoToJson(data, savePath, struct, tradeDate);

                    // 保存到结果集
                    resultMap.put(tradeDate, data);
                    System.out.println("Date: " + tradeDate);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return resultMap;
    }

    public static ConcurrentHashMap<LocalDate, BasicTable> getStockKBar(DBConnection conn, String DBName, String TBName, String savePath, String freqType,
                                                      String start_date, String end_date, StockKBarStruct struct, ArrayList<String> symbol_list) throws IOException {
        /*
        TBName: 日频/分钟频KBar表
        start_date：like 2020.01.01
        end_date：like 2020.01.02
        */
        // 多线程从DolphinDB数据库中取数，返回BasicTable
        // 转换字符串为DolphinDB List str
        String symbol_list_str = Utils.arrayToDolphinDBString(symbol_list);
        System.out.println("symbolList: " + symbol_list_str);

        // 获取所有时间
        BasicDateVector date_list = (BasicDateVector) conn.run("""
                t = select count(*) as count from loadTable("%s", "%s") where %s between date(%s) and date(%s) group by %s order by %s; 
                exec %s from t
                """.formatted(DBName, TBName, struct.dateCol, start_date, end_date, struct.dateCol, struct.dateCol, struct.dateCol));
        System.out.println("dateList: " + date_list.getString());

        // 创建多线程结果接收集合
        ConcurrentHashMap<LocalDate, BasicTable> resultMap = new ConcurrentHashMap<>();
        Collection<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i=0; i<date_list.rows(); i++) {
            LocalDate tradeDate = date_list.getDate(i);
            String tradeDateStr = tradeDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")); // 2020.01.01
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String script = """
                    select %s,%s,%s,
                    %s,%s,%s,%s,%s from loadTable("%s","%s") where %s == date(%s)
                    and %s in %s
                    """.formatted(struct.symbolCol, struct.dateCol, struct.timeCol,
                            struct.openCol, struct.highCol, struct.lowCol, struct.closeCol, struct.volumeCol,
                            DBName, TBName, struct.dateCol, tradeDateStr, struct.symbolCol, symbol_list_str);
                    System.out.println(script);
                    BasicTable data = (BasicTable) conn.run(script,4,4);

                    // 写入到本地Json文件(日频Kbar / 分钟频Kbar)
                    saveStockKBarToJson(data, savePath, struct, freqType, tradeDate);

                    // 保存到结果集
                    resultMap.put(tradeDate, data);
                    System.out.println("Date: " + tradeDate);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return resultMap;
    }

    public static void saveStockInfoToJson(BasicTable data, String savePath, StockInfoStruct struct, LocalDate tradeDate){
        /*
        "标的名称":{"open": ,"high": ,"low": ,"close": ,"start_date": ,"end_date": }
        */
        String dateStr = tradeDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String datePath = savePath + File.separator + dateStr; // Linux & Windows 兼容方法
        File dateDir = new File(datePath);  // 目录
        if (!dateDir.exists()){
            Boolean saveState = dateDir.mkdirs(); // 创建目录
        }

        // 按照日期分组并保存数据
        int rowCount = data.rows(); // 行数
        ConcurrentHashMap<String, JSONObject> infoMap = new ConcurrentHashMap<>();

        for (int i = 0; i < rowCount; i++){
            String symbol = data.getColumn(struct.symbolCol).get(i).toString();
            BasicDate date = (BasicDate) data.getColumn(struct.dateCol).get(i);
            double open = Double.parseDouble(data.getColumn(struct.openCol).get(i).getString());
            double high = Double.parseDouble(data.getColumn(struct.highCol).get(i).getString());
            double low = Double.parseDouble(data.getColumn(struct.lowCol).get(i).getString());
            double close = Double.parseDouble(data.getColumn(struct.closeCol).get(i).getString());
            BasicDate startDateObj = (BasicDate) data.getColumn("startDate").get(i);
            BasicDate endDateObj = (BasicDate) data.getColumn("endDate").get(i);
            String start_date = startDateObj.getDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String end_date = endDateObj.getDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 创建构造标的信息JSON对象
            JSONObject infoData = new JSONObject();
            infoData.put("open", open);
            infoData.put("high", high);
            infoData.put("low", low);
            infoData.put("close", close);
            infoData.put("start_date", start_date);
            infoData.put("end_date", end_date);

            // 将数据添加到对应股票代码的JSON对象中
            infoMap.put(symbol, infoData);
            // infoMap.computeIfAbsent(symbol, k -> new JSONObject()).put(symbol, infoData);
        }

        // 保存到本地Json文件
        try {
            String fileName = datePath + File.separator + dateStr + ".json";
            try (FileWriter fileWriter = new FileWriter(fileName)) {
                fileWriter.write(JSONObject.toJSONString(infoMap));
            } // Object序列化为JSON对象
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void saveStockKBarToJson(BasicTable data, String savePath, StockKBarStruct struct, String freqType, LocalDate tradeDate){
        String dateStr = tradeDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String datePath = savePath + File.separator + dateStr; // Linux & Windows 兼容方法
        File dateDir = new File(datePath);  // 目录
        if (!dateDir.exists()){
            Boolean saveState = dateDir.mkdirs(); // 创建目录
        }

        // 按照股票代码分组并保存数据
        int rowCount = data.rows(); // 行数
        ConcurrentHashMap<String, JSONObject> dataMap = new ConcurrentHashMap<>();

        for (int i = 0; i < rowCount; i++){
            String symbol = data.getColumn(struct.symbolCol).get(i).toString();
            BasicDate date = (BasicDate) data.getColumn(struct.dateCol).get(i);
            BasicTime time = null;
            double open = Double.parseDouble(data.getColumn(struct.openCol).get(i).getString());
            double high = Double.parseDouble(data.getColumn(struct.highCol).get(i).getString());
            double low = Double.parseDouble(data.getColumn(struct.lowCol).get(i).getString());
            double close = Double.parseDouble(data.getColumn(struct.closeCol).get(i).getString());
            double volume = Double.parseDouble(data.getColumn(struct.volumeCol).get(i).getString());

            // 创建构造分钟数据JSON对象
            JSONObject minuteData = new JSONObject();

            // 构造时间戳
            String timestamp = null;
            int minute = 1500;
            if(freqType.toLowerCase().equals("minute") && struct.timeCol != null){
                // 分钟频K线正常按照分钟编排
                time = (BasicTime) data.getColumn(struct.timeCol).get(i);
                LocalDateTime dateTime = LocalDateTime.of(date.getDate(), time.getTime());
                timestamp = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                int hour = time.getTime().getHour();
                int minuteOfHour = time.getTime().getMinute();
                minute = hour * 100 + minuteOfHour;  // 9+30 -> 930
            }else{
                // 日频K线默认添加1500
                LocalDateTime dateTime = LocalDateTime.of(date.getDate(), LocalTime.of(15, 0));
                timestamp = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            minuteData.put("minute", minute);
            minuteData.put("timestamp", timestamp);
            minuteData.put("open", open);
            minuteData.put("high", high);
            minuteData.put("low", low);
            minuteData.put("close", close);
            minuteData.put("volume", volume);

            // 将数据添加到对应股票代码的JSON对象中
            dataMap.computeIfAbsent(symbol, k -> new JSONObject()).put(Integer.toString(minute), minuteData);

        }
        // 保存每个股票的数据到JSON文件
        dataMap.forEach((symbol, jsonData) -> {
            try {
                String fileName = datePath + File.separator + symbol + ".json";
                try (FileWriter fileWriter = new FileWriter(fileName)) {
                    fileWriter.write(jsonData.toJSONString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void getStockKDataFromDolphinDB(DBConnection conn, String DBName, String TBName){}

    public static HashMap<String, StockInfo> getStockInfoFromJson(String JsonPath, LocalDate tradeDate, Integer threadCount) throws IOException {
        /*
        Step1. 读取目标文件夹的一天Json文件, 并返回一个ConcurrentHashMap -> 转换为HashMap<String, StockInfo>
        */
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String dateStr = tradeDate.format(formatter);
        String datePath = JsonPath + File.separator + dateStr; // Linux & Windows 兼容方法

        File dateDir = new File(datePath);
        if (!dateDir.exists()){ // 如果目录不存在
            return new HashMap<>();
        }

        File[] jsonFiles = dateDir.listFiles(((dir, name) -> name.endsWith(".json")));
        if (jsonFiles == null || jsonFiles.length == 0){ // 如果没有Json文件, 则返回一个空的HashMap
            return new HashMap<>();
        }

        // 读取HashMap并转换为HashMap<String, StockInfo>
        // 这里考虑到都是日线数据, 数据量不大, 直接串行读写即可
        HashMap<String, StockInfo> infoMap = new HashMap<>();

        for (File jsonFile: jsonFiles) {
            String content = new String(Files.readAllBytes(jsonFile.toPath()));
            JSONObject jsonData = JSONObject.parseObject(content);
            for (String symbol: jsonData.keySet()){
                JSONObject json = jsonData.getJSONObject(symbol);
                Double open = json.getDoubleValue("open");
                Double high = json.getDoubleValue("high");
                Double low = json.getDoubleValue("low");
                Double close = json.getDoubleValue("close");
                LocalDate start_date = LocalDate.parse(json.getString("start_date"),formatter); // 第一条股票行情对应的日期
                LocalDate end_date = LocalDate.parse(json.getString("end_date"),formatter);  // 最后一条股票行情对应的日期
                StockInfo info = new StockInfo(tradeDate, symbol, open, high, low, close, start_date, end_date);
                infoMap.put(symbol, info);
            }
        }

        return infoMap;
    }
    public static LinkedHashMap<Integer, HashMap<String, StockBar>> getStockKDataFromJson(String JsonPath, LocalDate tradeDate, Integer threadCount){
        /*
        分两步进行执行:
        Step1. 读取目标文件夹的一天Json文件, 并返回一个ConcurrentHashMap -> 转换为HashMap<String, LinkedHashMap<Integer, StockBar>>
        Step2. 再将步骤1的结果进行转换为HashMap<String, LinkedHashMap<Integer, StockBar>>
        */

        // 多线程读取目标文件夹的一天Json文件, 并返回一个ConcurrentHashMap -> 转换为HashMap
        String dateStr = tradeDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String datePath = JsonPath + File.separator + dateStr; // Linux & Windows 兼容方法

        File dateDir = new File(datePath);
        if (!dateDir.exists()){ // 如果目录不存在, 则返回一个空的LinkedHashMap
            return new LinkedHashMap<>();
        }

        File[] jsonFiles = dateDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) { // 如果目录下没有任何符合条件的Json文件
            return new LinkedHashMap<>();
        }

        // 创建ConcurrentHashMap保证线程安全
        ConcurrentHashMap<String, LinkedHashMap<Integer, StockBar>> resultMap = new ConcurrentHashMap<>();
        Collection<CompletableFuture<Void>> futures = new ArrayList<>();

        for (File jsonFile: jsonFiles){
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String symbol = jsonFile.getName().replace(".json",""); // 获取标的名称
                    String content = new String(Files.readAllBytes(jsonFile.toPath()));
                    JSONObject jsonData = JSONObject.parseObject(content);

                    // 创建LinkedHashMap接收分钟K线数据, 保证顺序(防止KBar里的数据是乱序的)
                    LinkedHashMap<Integer, StockBar> minuteBarMap = new LinkedHashMap<>();

                    for (String minuteStr : jsonData.keySet()){
                        JSONObject barData = jsonData.getJSONObject(minuteStr);  // 反序列化为JSONObject
                        LocalDateTime tradeTime = LocalDateTime.parse(barData.getString("timestamp"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        Double open = barData.getDoubleValue("open");
                        Double high = barData.getDoubleValue("high");
                        Double low = barData.getDoubleValue("low");
                        Double close = barData.getDoubleValue("close");
                        Double volume = barData.getDoubleValue("volume");
                        StockBar stockBar = new StockBar(symbol, tradeDate, tradeTime, open, high, low, close, volume);
                        // 添加至LinkedHashMap之中
                        minuteBarMap.put(Integer.parseInt(minuteStr), stockBar);
                    }

                    // 按照分钟进行排序
                    LinkedHashMap<Integer, StockBar> sortedMap = minuteBarMap.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    (e1, e2) -> e1,
                                    LinkedHashMap::new
                            ));
                    resultMap.put(symbol, sortedMap);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 此时的resultMap维度为HashMap<String, LinkedHashMap<Integer, StockBar>>
        // 需要转换为LinkedHashMap<Integer, HashMap<String, StockBar>>
        LinkedHashMap<Integer, HashMap<String, StockBar>> transFormedMap = new LinkedHashMap<>();

        for (Map.Entry<String, LinkedHashMap<Integer, StockBar>> entry: resultMap.entrySet()){
            String symbol = entry.getKey();
            LinkedHashMap<Integer, StockBar> minuteBarMap = entry.getValue();

            for (Map.Entry<Integer, StockBar> minuteEntry: minuteBarMap.entrySet()){
                Integer minute = minuteEntry.getKey();
                StockBar stockBar = minuteEntry.getValue();

                // 简洁写法
                transFormedMap.computeIfAbsent(minute, k -> new HashMap<>()).put(symbol, stockBar);

                // 完整写法
                if (!transFormedMap.containsKey(minute)){
                    transFormedMap.put(minute, new HashMap<>()); // 如果该时间点还不存在, 创建新的HashMap
                }
            }
        }

        // 保持时间戳顺序
        LinkedHashMap<Integer, HashMap<String, StockBar>> sortedTransformedDict = new LinkedHashMap<>();
        transFormedMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(entry -> sortedTransformedDict.put(entry.getKey(), entry.getValue()));

        // 返回LinkedHashMap
        return sortedTransformedDict;
    }
}
