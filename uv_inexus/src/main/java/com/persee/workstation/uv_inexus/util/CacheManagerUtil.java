package com.persee.workstation.uv_inexus.util;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Ehcache 缓存管理工具类
 * 提供完整的缓存操作功能
 */
@Component
public class CacheManagerUtil {

    @Autowired
    private CacheManager cacheManager;

    /**
     * 手动获取缓存
     */
    public Cache<String, Object> getCache(String cacheName) {
        return cacheManager.getCache(cacheName, String.class, Object.class);
    }

    /**
     * 手动放入缓存
     */
    public void putToCache(String cacheName, String key, Object value) {
        Cache<String, Object> cache = getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    /**
     * 手动获取缓存值
     */
    public Object getFromCache(String cacheName, String key) {
        Cache<String, Object> cache = getCache(cacheName);
        return cache != null ? cache.get(key) : null;
    }

    /**
     * 清除指定缓存
     */
    public void clearCache(String cacheName) {
        Cache<String, Object> cache = getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * 批量放入缓存
     */
    public void putAllToCache(String cacheName, Map<String, Object> entries) {
        Cache<String, Object> cache = getCache(cacheName);
        if (cache != null) {
            cache.putAll(entries);
        }
    }

    /**
     * 批量获取缓存值
     */
    public Map<String, Object> getAllFromCache(String cacheName, Set<String> keys) {
        Cache<String, Object> cache = getCache(cacheName);
        if (cache != null) {
            return cache.getAll(keys);
        }
        return new HashMap<>();
    }

    /**
     * 如果键不存在则放入缓存
     */
    public boolean putIfAbsent(String cacheName, String key, Object value) {
        Cache<String, Object> cache = getCache(cacheName);
        if (cache != null) {
            return cache.putIfAbsent(key, value) == null;
        }
        return false;
    }

    /**
     * 删除缓存项并返回旧值
     */
    public Object removeAndGet(String cacheName, String key) {
        Cache<String, Object> cache = getCache(cacheName);
        if (cache != null && cache.containsKey(key)) {
            Object oldValue = cache.get(key);
            cache.remove(key);
            return oldValue;
        }
        return null;
    }

    /**
     * 删除指定缓存项
     */
    public boolean removeFromCache(String cacheName, String key) {
        Cache<String, Object> cache = getCache(cacheName);
        if (cache != null) {
            boolean exists = cache.containsKey(key);
            cache.remove(key);
            return exists;
        }
        return false;
    }

    /**
     * 检查缓存项是否存在
     */
    public boolean containsKey(String cacheName, String key) {
        Cache<String, Object> cache = getCache(cacheName);
        if (cache != null) {
            return cache.containsKey(key);
        }
        return false;
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize(String cacheName) {
        Cache<String, Object> cache = getCache(cacheName);
        if (cache != null) {
            // 注意：Ehcache 3.x 没有直接获取大小的方法
            // 这里只是一个示意实现
            return -1; // 表示不支持
        }
        return 0;
    }

    /**
     * 获取所有缓存名称
     */
    public Set<String> getAllCacheNames() {
        Set<String> cacheNames = new HashSet<>();
        Iterable<String> names = cacheManager.getRuntimeConfiguration().getCacheConfigurations().keySet();
        for (String name : names) {
            cacheNames.add(name);
        }
        return cacheNames;
    }


    /**
     * 创建新的缓存（运行时动态创建）
     */
    public <K, V> Cache<K, V> createCache(String cacheName, Class<K> keyType, Class<V> valueType) {
        return cacheManager.createCache(cacheName,
                CacheConfigurationBuilder.newCacheConfigurationBuilder(keyType, valueType,
                        ResourcePoolsBuilder.heap(1000)).build());
    }

    /**
     * 删除缓存
     */
    public void removeCache(String cacheName) {
        cacheManager.removeCache(cacheName);
    }

    /**
     * 存储 List 数据
     */
    public void putListToCache(String cacheName, String key, List<?> list) {
        putToCache(cacheName, key, list);
    }

    /**
     * 获取 List 数据
     */
    public <T> List<T> getListFromCache(String cacheName, String key, Class<T> elementType) {
        Object cachedObject = getFromCache(cacheName, key);
        if (cachedObject instanceof List) {
            return (List<T>) cachedObject;
        }
        return new ArrayList<>();
    }

    /**
     * 存储 Map 数据
     */
    public void putMapToCache(String cacheName, String key, Map<?, ?> map) {
        putToCache(cacheName, key, map);
    }

    /**
     * 获取 Map 数据
     */
    public <K, V> Map<K, V> getMapFromCache(String cacheName, String key, Class<K> keyType, Class<V> valueType) {
        Object cachedObject = getFromCache(cacheName, key);
        if (cachedObject instanceof Map) {
            return (Map<K, V>) cachedObject;
        }
        return new HashMap<>();
    }

    /**
     * 向 List 缓存中添加元素
     */
    public <T> void addToListCache(String cacheName, String key, T item) {
        List<T> list = getListFromCache(cacheName, key, (Class<T>) item.getClass());
        list.add(item);
        putListToCache(cacheName, key, list);
    }

    /**
     * 从 List 缓存中移除元素
     */
    public <T> boolean removeFromListCache(String cacheName, String key, T item) {
        List<T> list = getListFromCache(cacheName, key, (Class<T>) item.getClass());
        boolean removed = list.remove(item);
        if (removed) {
            putListToCache(cacheName, key, list);
        }
        return removed;
    }

    /**
     * 向 Map 缓存中添加元素
     */
    public <K, V> void putToMapCache(String cacheName, String key, K mapKey, V mapValue) {
        Map<K, V> map = getMapFromCache(cacheName, key, (Class<K>) mapKey.getClass(), (Class<V>) mapValue.getClass());
        map.put(mapKey, mapValue);
        putMapToCache(cacheName, key, map);
    }

    /**
     * 从 Map 缓存中获取元素
     */
    public <K, V> V getFromMapCache(String cacheName, String key, K mapKey) {
        Map<K, V> map = getMapFromCache(cacheName, key, (Class<K>) mapKey.getClass(), null);
        return map.get(mapKey);
    }

    /**
     * 关闭缓存管理器
     */
    public void close() {
        if (cacheManager != null) {
            cacheManager.close();
        }
    }
}
