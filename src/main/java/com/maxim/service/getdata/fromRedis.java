package com.maxim.service.getdata;
// Redis模块
import com.alibaba.fastjson2.JSON;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
// 工具模块
import java.lang.Void;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.alibaba.fastjson2.JSONObject;


public class fromRedis{
    /*
    * 从Redis内存直接输出JavaObject至内存
    * 输入Redis的数据类型有以下几种, 均是按照redisHash格式进行存储, 其中LocalDate为Field, 对应的集合为value
    * ConcurrentHashMap<LocalDate, List<T>>
    * ConcurrentHashMap<LocalDate, HashMap<String, T>>
    * ConcurrentHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>>
    * ConcurrentHashMap<LocalDate, HashMap<String, T>>
    * ConcurrentHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>>

    * 异步多线程 返回原始格式
    *
    * */

    public <T> LinkedHashMap<LocalDate, List<T>> RedisToJavaBeans(Jedis jedis,
                                                                Collection<LocalDate> dateList,
                                                                String redisKey,
                                                                Class<T> clazz){
        LinkedHashMap<LocalDate, List<T>> resultMap = fromRedisHashCommon(jedis, dateList, redisKey,
                (Class<List<T>>) (Class<?>) List.class); // 类型转换示例
        return resultMap;
    }

    public <T> LinkedHashMap<LocalDate, HashMap<String, T>> RedisToJavaBeansBySymbol(Jedis jedis,
                                                                          Collection<LocalDate> dateList,
                                                                          String redisKey,
                                                                          Class<T> clazz){
        LinkedHashMap<LocalDate, HashMap<String, T>> resultMap = fromRedisHashCommon(jedis, dateList, redisKey,
                (Class<HashMap<String, T>>) (Class<?>) HashMap.class); // 类型转换示例
        return resultMap;
    }

    public <T> LinkedHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> RedisToJavaBeansByTime(Jedis jedis,
                                                                                                       Collection<LocalDate> dateList,
                                                                                                       String redisKey,
                                                                                                       Class<T> clazz){
        LinkedHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> resultMap =
                (LinkedHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>>)
                        fromRedisHashCommon(jedis, dateList, redisKey, clazz);
        return resultMap;
    }

    public <T> LinkedHashMap<LocalDate, HashMap<String, T>> RedisToJavaBeanBySymbol(Jedis jedis,
                                                                                    Collection<LocalDate> dateList,
                                                                                    String rediskey,
                                                                                    Class<T> clazz){
        LinkedHashMap<LocalDate, HashMap<String, T>> resultMap =
                (LinkedHashMap<LocalDate, HashMap<String, T>>)
                        fromRedisHashCommon(jedis, dateList, rediskey, clazz);
        return resultMap;
    }

    public <T> LinkedHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> RedisToJavaBeanByTime(Jedis jedis,
                                                                                                       Collection<LocalDate> dateList,
                                                                                                       String redisKey,
                                                                                                       Class<T> clazz){
        LinkedHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> resultMap =
                (LinkedHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>>)
                        fromRedisHashCommon(jedis, dateList, redisKey, clazz);
        return resultMap;
                                                                   }

    // 多线程异步从Redis中获取Object的通用函数
    public <T> LinkedHashMap<LocalDate, T> fromRedisHashCommon(Jedis jedis,
                                                               Collection<LocalDate> dateList,
                                                               String redisKey,
                                                               Class<T> clazz){
        // 创建异步任务结果集合
        LinkedHashMap<LocalDate, T> resultMap = new LinkedHashMap<>();
        Collection<CompletableFuture<Void>> futures = new ArrayList<>();

        for (LocalDate date: dateList){
            LocalDate finalDate = date; // 多线程中需要final变量
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try{
                    String strJson = jedis.hget(redisKey, finalDate.toString()); // 获取Redis中序列化的字符串数据
                    if (strJson != null){
                        T object = JSON.parseObject(strJson, clazz);
                        resultMap.put(finalDate, object);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        // 等待所有异步任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 最后需要对结果按照日期进行排序
        return resultMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        }
}


//    public <T> LinkedHashMap<LocalDate, Collection<T>> singleModeFromList(Jedis jedis, Collection<LocalDate> date_list, Class<T> clazz){
//    // Redis读取单类别对象格式, 返回一个ConcurrentHashMap, 键为LocalDate, 值为Collection<JavaBean>
//    ConcurrentHashMap<LocalDate, Collection<T>> resultMap = new ConcurrentHashMap<>();
//
//    // 创建异步任务集合
//    Collection<CompletableFuture<Void>> futures = new ArrayList<>();
//
//    for (LocalDate date : date_list){
//        LocalDate finalDate = date; // 在lambda表达式中需要final变量
//        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//            try {
//                List<String> strJsonList = jedis.lrange(finalDate.toString(), 0, -1);
//                if (strJsonList.isEmpty()){
//                    System.out.println("Redis中未找到该LocalDate:" + finalDate + "的数据");
//                    return;
//                }
//                // 进行反序列化
//                List<T> objects = new ArrayList<>();
//                for (String strJson : strJsonList) {
//                    T object = JSON.parseObject(strJson, clazz);
//                    objects.add(object);
//                }
//
//                resultMap.put(finalDate, objects);
//            } catch (Exception e) {
//                System.out.println("处理日期 " + finalDate + " 时发生错误: " + e.getMessage());
//                e.printStackTrace();
//            }
//        });
//        futures.add(future);
//    }
//
//    // 等待所有异步任务完成
//    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//
//    // 按照日期对ConcurrentHashMap进行正序排序, 返回LinkedHashMap
//    return resultMap.entrySet().stream()
//            .sorted(Map.Entry.comparingByKey())
//            .collect(Collectors.toMap(
//                Map.Entry::getKey,
//                Map.Entry::getValue,
//                (e1, e2) -> e1,
//                LinkedHashMap::new
//            ));
//    }
