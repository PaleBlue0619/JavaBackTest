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


public class fromRedis {
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

    public <T> TreeMap<LocalDate, List<T>> RedisToJavaBeans(JedisPool jedis,
                                                            Collection<LocalDate> dateList,
                                                            String redisKey,
                                                            Class<T> clazz) {
        TreeMap<LocalDate, List<T>> resultMap = fromRedisHashCommon(jedis, dateList, redisKey,
                (Class<List<T>>) (Class<?>) List.class); // 类型转换示例
        return resultMap;
    }

    public <T> TreeMap<LocalDate, HashMap<String, T>> RedisToJavaBeansBySymbol(JedisPool jedis,
                                                                               Collection<LocalDate> dateList,
                                                                               String redisKey,
                                                                               Class<T> clazz) {
        TreeMap<LocalDate, HashMap<String, T>> resultMap = fromRedisHashCommon(jedis, dateList, redisKey,
                (Class<HashMap<String, T>>) (Class<?>) HashMap.class); // 类型转换示例
        return resultMap;
    }

    public <T> TreeMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> RedisToJavaBeansByTime(JedisPool jedis,
                                                                                                 Collection<LocalDate> dateList,
                                                                                                 String redisKey,
                                                                                                 Class<T> clazz) {
        TreeMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> resultMap =
                (TreeMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>>)
                        fromRedisHashCommon(jedis, dateList, redisKey, clazz);
        return resultMap;
    }

    public <T> TreeMap<LocalDate, HashMap<String, T>> RedisToJavaBeanBySymbol(JedisPool jedis,
                                                                              Collection<LocalDate> dateList,
                                                                              String redisKey,
                                                                              Class<T> clazz) {
        TreeMap<LocalDate, HashMap<String, T>> resultMap =
                (TreeMap<LocalDate, HashMap<String, T>>)
                        fromRedisHashCommon(jedis, dateList, redisKey, clazz);
        return resultMap;
    }

    public <T> TreeMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> RedisToJavaBeanByTime(JedisPool jedis,
                                                                                                Collection<LocalDate> dateList,
                                                                                                String redisKey,
                                                                                                Class<T> clazz) {
        TreeMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> resultMap =
                (TreeMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>>)
                        fromRedisHashCommon(jedis, dateList, redisKey, clazz);
        return resultMap;
    }

    // 多线程异步从Redis中获取Object的通用函数
    public <T> TreeMap<LocalDate, T> fromRedisHashCommon(JedisPool jedisPool,
                                                         Collection<LocalDate> dateList,
                                                         String redisKey,
                                                         Class<T> clazz) {
        // 创建异步任务结果集合
        TreeMap<LocalDate, T> resultMap = new TreeMap<>();
        Collection<CompletableFuture<Void>> futures = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDate finalDate = date; // 多线程中需要final变量
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Jedis jedis = jedisPool.getResource();
                    String strJson = jedis.hget(redisKey, finalDate.toString()); // 获取Redis中序列化的字符串数据
                    if (strJson != null) {
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

        // 返回TreeMap
        return resultMap;
    }
}