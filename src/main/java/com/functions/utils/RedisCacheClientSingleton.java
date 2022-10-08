package com.functions.utils;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;

public class RedisCacheClientSingleton {

    private static RedisCacheClientSingleton instance;

    private Jedis jedis;

    private RedisCacheClientSingleton() {

        final boolean useSsl = true;
        final String REDIS_CACHE_HOSTNAME = System.getenv("REDISCACHEHOSTNAME");
        final String REDIS_CACHE_KEY = System.getenv("REDISCACHEKEY");

        this.jedis = new Jedis(REDIS_CACHE_HOSTNAME, 6380, DefaultJedisClientConfig.builder()
            .password(REDIS_CACHE_KEY)
            .ssl(useSsl)
            .build());
    }

    public static RedisCacheClientSingleton getInstance() {
        if (instance == null) {
            instance = new RedisCacheClientSingleton();
        }
        return instance;
    }

    public Jedis getJedis() {
        return this.jedis;
    }
}

