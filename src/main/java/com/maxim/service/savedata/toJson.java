package com.maxim.service.savedata;
// Json - alibaba.fastJson2
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
// 工具模块
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Path;

public class toJson{
    /*
     * BasicTable Obj -> JavaBean -> 解析后的JavaBean对象存储至Json
     * 目标Json文件路径: JsonPath + File.separator + dateStr + ".json"
     */

    public void createFilePath(String filePath){
        try{
            Path path = Paths.get(filePath);
            Files.createDirectories(path);
        } catch (Exception e){
            throw new RuntimeException("Failed to Create FilePath: "+filePath);
        }
    }

    public void deleteFilePath(String filePath){
        try{
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (Exception e){
            throw new RuntimeException("Failed to Delete FilePath: "+filePath);
        }
    }

    public <T> void JavaBeansToJson(
            ConcurrentHashMap<LocalDate, List<T>> dataMap,
            String filePath, boolean dropFilePath){
        toJsonCommon(dataMap, filePath, dropFilePath);
    }

    public <T> void JavaBeansBySymbolToJson(
            ConcurrentHashMap<LocalDate, HashMap<String, T>> dataMap,
            String filePath, boolean dropFilePath){
        toJsonCommon(dataMap, filePath, dropFilePath);
    }

    public <T> void JavaBeansByTimeToJson(ConcurrentHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> dataMap,
                                          String filePath, boolean dropFilePath){
        toJsonCommon(dataMap, filePath, dropFilePath);
    }

    public <T> void JavaBeanBySymbolToJson(ConcurrentHashMap<LocalDate, HashMap<String, T>> dataMap,
                                          String filePath, boolean dropFilePath){
        toJsonCommon(dataMap, filePath, dropFilePath);
    }

    public <T> void JavaBeanByTimeToJson(ConcurrentHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> dataMap,
                                          String filePath, boolean dropFilePath){
        toJsonCommon(dataMap, filePath, dropFilePath);
    }

//    // 提取的公共逻辑多线程并发写入
//    public <T> void toJsonCommon(ConcurrentHashMap<LocalDate, T> dataMap,
//                                 String filePath, boolean dropFilePath){
//        // 是否删除目标文件路径
//        if (dropFilePath){
//            deleteFilePath(filePath);
//        }
//
//        // 首先创建目标文件路径
//        createFilePath(filePath);
//
//        // 之后多线程并发写入
//        CompletableFuture<?>[] futures = dataMap.entrySet().stream()
//            .map(entry -> CompletableFuture.runAsync(() -> {
//                LocalDate date = entry.getKey(); // 获取当前日期
//                String fileName = filePath + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".json"; // 构造文件名称
//                // 添加类型信息以便后续识别LocalTime等特殊类型
//                String json = JSON.toJSONString(entry.getValue(),
//                    JSONWriter.Feature.WriteClassName);
//                try (FileWriter fileWriter = new FileWriter(fileName)){
//                    fileWriter.write(json);  // 写入数据至缓冲区
//                    fileWriter.flush();      // 刷新缓冲区, 将数据写入文件
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            })).toArray(CompletableFuture[]::new);
//
//        // 等待所有任务完成
//        CompletableFuture.allOf(futures).join();
//    }



    // 提取的公共逻辑多线程并发写入
    public <T> void toJsonCommon(ConcurrentHashMap<LocalDate, T> dataMap,
                                 String filePath, boolean dropFilePath){
        // 是否删除目标文件路径
        if (dropFilePath){
            deleteFilePath(filePath);
        }

        // 首先创建目标文件路径
        createFilePath(filePath);

        // 之后多线程并发写入
        CompletableFuture<?>[] futures = dataMap.entrySet().stream()
            .map(entry -> CompletableFuture.runAsync(() -> {
                LocalDate date = entry.getKey(); // 获取当前日期
                String fileName = filePath + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".json"; // 构造文件名称
                String json = JSON.toJSONString(entry.getValue()); // 获取ConcurrentHashMap中值转化为的JSON字符串
                try (FileWriter fileWriter = new FileWriter(fileName)){
                    fileWriter.write(json);  // 写入数据至缓冲区
                    fileWriter.flush();      // 刷新缓冲区, 将数据写入文件, 但不会关闭流, 依然可以继续写入
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })).toArray(CompletableFuture[]::new);

        // 等待所有任务完成
        CompletableFuture.allOf(futures).join();
    }
}
