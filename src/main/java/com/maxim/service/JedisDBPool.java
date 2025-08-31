package com.maxim.service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class JedisDBPool {
    public static JedisPool jedisPool;
    // 这里为什么要用静态代码块?
    // 是因为我不需要每次都从properties加载配置参数去手工装配，
    // 我希望这个过程是自动执行的静态方法
    static {  // 静态代码块, 在类初始化时执行, 只执行一次
        try {
            Properties properties = new Properties();
            InputStream in = JedisDBPool.class.getClassLoader().getResourceAsStream("redis.properties");
            properties.load(in);
            JedisPoolConfig config = new JedisPoolConfig();

            // 设置Redis配置参数(host/port等参数->redis.properties->properties)
            config.setMaxTotal(Integer.parseInt(properties.getProperty("maxTotal")));
            config.setMaxIdle(Integer.parseInt(properties.getProperty("maxIdle")));
            config.setMinIdle(Integer.parseInt(properties.getProperty("minIdle")));
            config.setMaxWaitMillis(Long.parseLong(properties.getProperty("maxWaitMillis")));
            config.setTestOnBorrow(Boolean.parseBoolean(properties.getProperty("testOnBorrow")));
            config.setTestOnReturn(Boolean.parseBoolean(properties.getProperty("testOnReturn")));
            config.setTimeBetweenEvictionRunsMillis(Long.parseLong(properties.getProperty("timeBetweenEvictionRunsMillis")));
            config.setMinEvictableIdleTimeMillis(Long.parseLong(properties.getProperty("minEvictableIdleTimeMillis")));
            config.setNumTestsPerEvictionRun(Integer.parseInt(properties.getProperty("numTestsPerEvictionRun")));

            // 创建JedisPool
            jedisPool = new JedisPool(config, properties.getProperty("host"), Integer.parseInt(properties.getProperty("port")));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Getter 方法
    public static Jedis getConnectJedis() {
        // 获取Jedis连接实例
        return jedisPool.getResource();
    }
}

