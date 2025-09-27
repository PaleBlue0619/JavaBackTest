package com.maxim.service.getdata;
// Json模块 -alibaba fastJson2
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
// 工具模块
import javax.print.DocFlavor;
import java.io.IOException;
import java.lang.Void;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class fromJson {
    /*
    * 从持久化Json文件中获取对应格式的JavaObject至内存
    * 输入的Json数据类型有以下几种, 均是按照JsonString格式进行存储, 其中LocalDate为Json文件名, 对应的集合为JsonString
    * */

    public <T> TreeMap<LocalDate, List<T>> JsonToJavaBeans(Collection<LocalDate> dateList,
                                                           String filePath,
                                                           Class<T> clazz){
        TreeMap<LocalDate, List<T>> resultMap = (TreeMap<LocalDate, List<T>>) fromJsonCommon(dateList, filePath,
                (Class<List<T>>) (Class<?>) List.class);
        return resultMap;
    }

    public <T> TreeMap<LocalDate, HashMap<String, T>> JsonToJavaBeansBySymbol(Collection<LocalDate> dateList,
                                                                              String filePath,
                                                                              Class<T> clazz){
        TreeMap<LocalDate, HashMap<String, T>> resultMap = fromJsonCommon(dateList, filePath,
                (Class<HashMap<String, T>>) (Class<?>) HashMap.class);
        return resultMap;
    }

    public <T> TreeMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> JsonToJavaBeansByTime(Collection<LocalDate> dateList,
                                                                                                String filePath,
                                                                                                Class<T> clazz){
        TreeMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> resultMap = fromJsonCommon(dateList, filePath,
                (Class<TreeMap<LocalTime, HashMap<String, T>>>) (Class<?>) TreeMap.class);
        return resultMap;
    }

    public <T> TreeMap<LocalDate, HashMap<String, T>> JsonToJavaBeanBySymbol(Collection<LocalDate> dateList,
                                                                             String filePath,
                                                                             Class<T> clazz){
        TreeMap<LocalDate, HashMap<String, T>> resultMap = fromJsonCommon(dateList, filePath,
                (Class<HashMap<String, T>>) (Class<?>) HashMap.class);
        return resultMap;
    }

    public <T> TreeMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> JsonToJavaBeanByTime(Collection<LocalDate> dateList,
                                                                                               String filePath,
                                                                                               Class<T> clazz){
        TreeMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> resultMap = fromJsonCommon(dateList, filePath,
                (Class<TreeMap<LocalTime, HashMap<String, T>>>) (Class<?>) TreeMap.class);
        return resultMap;
    }

    public <T> TreeMap<LocalDate, T> fromJsonCommon(Collection<LocalDate> dateList,
                                                    String filePath,
                                                    Class<T> clazz){
        // 创建异步多线程结果接收集合
        TreeMap<LocalDate, T> resultMap = new TreeMap<>();
        Collection<CompletableFuture<Void>> futures = new ArrayList<>();

        for (LocalDate date: dateList){
            LocalDate finalDate = date; // 创建一个局部变量，用于保存当前循环变量的值
            String fileName = date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".json";
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String strJson = new String(
                            Files.readAllBytes(Paths.get(fileName)));
                    T object = JSON.parseObject(strJson, clazz);
                    resultMap.put(finalDate, object);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        // 等待所有异步任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 返回TreeMap
        return resultMap;
    }
}



