package com.functions.helpers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CacheManager {
    private static CacheManager instance;
    private static Object monitor = new Object();
    private Map<String, String> cache = Collections.synchronizedMap(new HashMap<String, String>());

    private CacheManager() {
    }

    public void put(String cacheKey, String value) {
        cache.put(cacheKey, value);
    }

    public String get(String cacheKey) {
        return cache.get(cacheKey);
    }

    public void clear(String cacheKey) {
        cache.put(cacheKey, null);
    }

    public void clear() {
        cache.clear();
    }

    public static CacheManager getInstance() {
        if (instance == null) {
            synchronized (monitor) {
                if (instance == null) {
                    instance = new CacheManager();
                }
            }
        }
        return instance;
    }

}
