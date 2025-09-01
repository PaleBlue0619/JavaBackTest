package com.maxim.service.savedata;
// Redis模块
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
// 工具模块
import java.io.IOException;
import java.io.InputStream;
import java.lang.Void;
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

public class toRedis{
    /*
     * 从DolphinDB内存直接输出JavaBean至Redis
     * 设计:ConCurrentHashMap<LocalDate, Collection<JavaBean>> ->
       方式1[仅适用于单资产回测]: Redis(String ): key: LocalDate, value: Collection<JavaBean>
       方式2[适用于多资产并行回测]: Redis(Hash): key: AssetType, field: LocalDate value: Collection<JavaBean>
     注: 统一使用LocalDate.toString()作为键, 即"2020-01-01"
     */


    // LocalDate: ClazzObj
    public <T> void singleModeObject(Jedis jedis, ConcurrentHashMap<LocalDate, Class<T>> resultMap, Boolean dropKey){
        // 单类别对象存储至Redis, 键的名称为LocalDate, 值为JavaBean
        // 删除对应的键
        if (dropKey){
            for (LocalDate tradeDate: resultMap.keySet()){
                if (jedis.exists(tradeDate.toString())){
                    jedis.del(tradeDate.toString());
                }
            }
        }

        for (LocalDate tradeDate: resultMap.keySet()){
            // 利用FastJson2将对象序列化为字符串格式
            String strJson = JSONObject.toJSONString(resultMap.get(tradeDate));
            jedis.set(tradeDate.toString(), strJson);
        }
    }

    // LocalDate: Collection<ClazzObj>
    public void singleMode(Jedis jedis, ConcurrentHashMap<LocalDate, Collection<Class<?>>> resultMap, Boolean dropKey){
        // 单类别对象存储至Redis, 键的名称为LocalDate, 值为Collection<JavaBean>
        // 删除对应的键
        if (dropKey){
            for (LocalDate tradeDate: resultMap.keySet()){
                if (jedis.exists(tradeDate.toString())){
                    jedis.del(tradeDate.toString());
                }
            }
        }

        for (LocalDate tradeDate: resultMap.keySet()){
            // 利用FastJson2将对象序列化为字符串格式
            String strJson = JSONObject.toJSONString(resultMap.get(tradeDate));
            jedis.set(tradeDate.toString(), strJson);
        }
    }

    public void singleModeToList(Jedis jedis, ConcurrentHashMap<LocalDate, Collection<Class<?>>> resultMap, Boolean dropKey){
        // 单类别对象存储至Redis, 键的名称为LocalDate, 值为Redis List格式, 每个元素为JavaBean
        // 删除对应的键
        if (dropKey){
            for (LocalDate tradeDate: resultMap.keySet()){
                if (jedis.exists(tradeDate.toString())){
                    jedis.del(tradeDate.toString());
                }
            }
        }

        for (LocalDate tradeDate: resultMap.keySet()){
            for (Class<?> clazzObj: resultMap.get(tradeDate)){
                String strJson = JSONObject.toJSONString(clazzObj);
                jedis.rpush(tradeDate.toString(), strJson);  // 这里用rpush保证顺序
            }
        }

    }

    public <T> void multiModeObject(Jedis jedis, ConcurrentHashMap<LocalDate, Class<T>> resultMap, String redisKey, Boolean dropKey){
        // 多类别对象存储至Redis, 键的名称为redisKey, field为LocalDate, 值为JavaBean
        if (dropKey && jedis.exists(redisKey)){
            // 说明需要提前删除Redis中的键
            jedis.del(redisKey);
        }

        for (LocalDate tradeDate: resultMap.keySet()){
            String strJson = JSONObject.toJSONString(resultMap.get(tradeDate));
            jedis.hset(redisKey, tradeDate.toString(), strJson);
        }
    }

    public void multiMode(Jedis jedis, ConcurrentHashMap<LocalDate, Collection<Class<?>>> resultMap, String redisKey, Boolean dropKey){
        // 多类别对象存储至Redis, 键的名称为redisKey, field为LocalDate, 值为Collection<JavaBean>
        if (dropKey && jedis.exists(redisKey)){
            // 说明需要提前删除Redis中的键
            jedis.del(redisKey);
        }

        for (LocalDate tradeDate: resultMap.keySet()){
            String strJson = JSONObject.toJSONString(resultMap.get(tradeDate));
            jedis.hset(redisKey, tradeDate.toString(), strJson);
        }
    }

    public void multiModeToList(Jedis jedis, ConcurrentHashMap<LocalDate, Collection<Class<?>>> resultMap, String redisKey, Boolean dropKey){
        // 多类别对象存储至Redis, 键的名称为redisKey, 值为Redis List格式, 每个元素为JavaBean
        if (dropKey && jedis.exists(redisKey)){
            // 说明需要提前删除Redis中的键
            jedis.del(redisKey);
        }

        for (LocalDate tradeDate: resultMap.keySet()){
            for (Class<?> clazzObj: resultMap.get(tradeDate)){
                String strJson = JSONObject.toJSONString(clazzObj);
                jedis.rpush(redisKey, strJson);  // 这里用rpush保证顺序
            }
        }
    }

}