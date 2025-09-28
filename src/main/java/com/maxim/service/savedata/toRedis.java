package com.maxim.service.savedata;
// Redis模块
import com.alibaba.fastjson2.JSON;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
// 工具模块
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import com.alibaba.fastjson2.JSONObject;

public class toRedis {
    /*
     * BasicTable Obj -> JavaBean -> 解析后的JavaBean对象存储至Redis
     * */

    // 不同类型的ConcurrentHashMap<LocalDate,JavaBean>逻辑
    public <T> void JavaBeansToRedis(JedisPool jedisPool,
                               ConcurrentHashMap<LocalDate, List<T>> dataMap,
                               String redisKey, boolean dropRedisKey) {
        toRedisHashCommon(jedisPool, dataMap, redisKey, dropRedisKey);
    }
    public <T> void JavaBeansBySymbolToRedis(JedisPool jedisPool,
                                ConcurrentHashMap<LocalDate, HashMap<String, T>> dataMap,
                                String redisKey, boolean dropRedisKey) {
        toRedisHashCommon(jedisPool, dataMap, redisKey, dropRedisKey);
    }

    public <T> void JavaBeansByTimeToRedis(JedisPool jedisPool,
                                 ConcurrentHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> dataMap,
                                 String redisKey, boolean dropRedisKey){
        toRedisHashCommon(jedisPool, dataMap, redisKey, dropRedisKey);
    }

    public <T> void JavaBeanBySymbolToRedis(JedisPool jedisPool,
                                 ConcurrentHashMap<LocalDate, HashMap<String, T>> dataMap,
                                 String redisKey, boolean dropRedisKey){
        toRedisHashCommon(jedisPool, dataMap, redisKey, dropRedisKey);
    }

    public <T> void JavaBeanByTimeToRedis(JedisPool jedisPool,
                                 ConcurrentHashMap<LocalDate, TreeMap<LocalTime, HashMap<String, T>>> dataMap,
                                 String redisKey, boolean dropRedisKey){
        toRedisHashCommon(jedisPool, dataMap, redisKey, dropRedisKey);
    }

    // 提取的公共逻辑多线程并发写入
    private <T> void toRedisHashCommon(JedisPool jedisPool,
                                      ConcurrentHashMap<LocalDate, T> dataMap,
                                      String redisKey, boolean dropRedisKey) {
        if (dropRedisKey) {
            try (Jedis jedis = jedisPool.getResource()) {
                if (jedis.exists(redisKey)) {
                    jedis.del(redisKey);
                }
            }
        }

        // 多线程并发写入
        CompletableFuture<?>[] futures = dataMap.entrySet().stream()
            .map(entry -> CompletableFuture.runAsync(() -> {
                try (Jedis jedis = jedisPool.getResource()) {
                    String json = JSON.toJSONString(entry.getValue());
                    jedis.hset(redisKey, entry.getKey().toString(), json);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }))
            .toArray(CompletableFuture[]::new);

        // 等待所有任务完成
        CompletableFuture.allOf(futures).join();
    }
    }