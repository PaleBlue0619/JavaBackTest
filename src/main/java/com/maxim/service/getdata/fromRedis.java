package com.maxim.service.getdata;
// Redis模块
import com.alibaba.fastjson2.JSON;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
// 工具模块
import java.io.IOException;
import java.io.InputStream;
import java.lang.Void;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.io.File;
import java.io.FileWriter;
import com.alibaba.fastjson2.JSONObject;


public class fromRedis{
    /*
     * 从Redis内存直接输出JavaObject至内存
     * 设计: -> ConcurrentHashMap<LocalDate, Collection<JavaBean>>
       方式1[仅适用于单资产回测]: Redis(String): key: LocalDate, value: Collection<JavaBean>
       方式2[适用于多资产并行回测]: Redis(Hash): key: AssetType, field: LocalDate value: Collection<JavaBean>
     * */

    // 使用泛型
    // 返回一个对象
    public <T> T singleMode(Jedis jedis, LocalDate date, T clazz){
        // 给定一个clazz, 从Redis中读取LocalDate date键对应的该clazz对象, 输出至内存
        String strJson = jedis.get(date.toString());
        if (strJson == null) {
            System.out.println("Redis中未找到该LocalDate:" + date + "的数据");
            return null;
        }
        return JSON.parseObject(strJson, (Type) clazz);
    }

    // 返回一个集合
    public <T> Collection<T> singleMode(Jedis jedis, LocalDate date, Class<T> clazz) {
        // 给定一个clazz, 从Redis中读取LocalDate date键对应的该clazz对象, 返回一个Collection<JavaBean>
        String strJson = jedis.get(date.toString());
        if (strJson == null) {
            System.out.println("Redis中未找到该LocalDate:" + date + "的数据");
            return Collections.emptyList(); // 返回空集合
        }
        // 进行反序列化
        return JSON.parseArray(strJson, clazz);
    }

    public <T> LinkedHashMap<LocalDate, Collection<T>> singleModeFromList(Jedis jedis, Collection<LocalDate> date_list, Class<T> clazz){
    // Redis读取单类别对象格式, 返回一个ConcurrentHashMap, 键为LocalDate, 值为Collection<JavaBean>
    ConcurrentHashMap<LocalDate, Collection<T>> resultMap = new ConcurrentHashMap<>();

    // 创建异步任务集合
    Collection<CompletableFuture<Void>> futures = new ArrayList<>();

    for (LocalDate date : date_list){
        LocalDate finalDate = date; // 在lambda表达式中需要final变量
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                List<String> strJsonList = jedis.lrange(finalDate.toString(), 0, -1);
                if (strJsonList.isEmpty()){
                    System.out.println("Redis中未找到该LocalDate:" + finalDate + "的数据");
                    return;
                }
                // 进行反序列化
                List<T> objects = new ArrayList<>();
                for (String strJson : strJsonList) {
                    T object = JSON.parseObject(strJson, clazz);
                    objects.add(object);
                }

                resultMap.put(finalDate, objects);
            } catch (Exception e) {
                System.out.println("处理日期 " + finalDate + " 时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        });
        futures.add(future);
    }

    // 等待所有异步任务完成
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // 按照日期对ConcurrentHashMap进行正序排序, 返回LinkedHashMap
    return resultMap.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    public <T> T multiMode(Jedis jedis, LocalDate date, String redisKey, T clazz){
        // 给定一个clazz, 从Redis中读取redisKey键对应HashSet中对应date的该clazz对象, 输出至内存
        String strJson = jedis.hget(redisKey, date.toString());
        if (strJson == null) {
            System.out.println("Redis中未找到该LocalDate:" + date + "的数据");
            return null;
        }
        return JSON.parseObject(strJson, (Type) clazz);
    }

    public <T> Collection<T> multiMode(Jedis jedis, LocalDate date, String redisKey, Class<T> clazz) {
        // 给定一个clazz, 从Redis中读取redisKey键对应HashSet中对应date的该clazz对象, 返回一个Collection<JavaBean>
        String strJson = jedis.hget(redisKey, date.toString());
        if (strJson == null) {
            System.out.println("Redis中未找到该LocalDate:" + date + "的数据");
            return Collections.emptyList(); // 返回空集合
        }
        return JSON.parseArray(strJson, clazz);
    }
}