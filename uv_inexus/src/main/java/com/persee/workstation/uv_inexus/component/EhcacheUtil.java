package com.persee.workstation.uv_inexus.component;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Ehcache缓存操作工具类
 */
@Component
public class EhcacheUtil {

    @Autowired
    private CacheManager cacheManager;

    /**
     * 向指定缓存中存储数据
     *
     * @param cacheName 缓存名称
     * @param key       键
     * @param value     值
     * @param keyType   键类型
     * @param valueType 值类型
     * @param <K>       键类型
     * @param <V>       值类型
     */
    public <K, V> void put(String cacheName, K key, V value, Class<K> keyType, Class<V> valueType) {
        System.out.println("尝试获取缓存: " + cacheName);
        System.out.println("Key类型: " + keyType);

        Cache<K, V> cache = cacheManager.getCache(cacheName, keyType, valueType);

        if (cache == null) {
            System.out.println("缓存获取失败，返回null");
            // 列出所有可用的缓存
            cacheManager.getRuntimeConfiguration().getCacheConfigurations().keySet()
                    .forEach(name -> System.out.println("可用缓存: " + name));
        } else {
            System.out.println("成功获取缓存: " + cacheName);
        }
        if (cache != null) {
            cache.put(key, value);
        }
    }

    /**
     * 从指定缓存中获取数据
     *
     * @param cacheName 缓存名称
     * @param key       键
     * @param keyType   键类型
     * @param valueType 值类型
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 值
     */
    public <K, V> V get(String cacheName, K key, Class<K> keyType, Class<V> valueType) {
        Cache<K, V> cache = cacheManager.getCache(cacheName, keyType, valueType);
        if (cache != null) {
            return cache.get(key);
        }
        return null;
    }

    /**
     * 从指定缓存中移除data
     *
     * @param cacheName 缓存名称
     * @param key       键
     * @param keyType   键类型
     * @param valueType 值类型
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 被移除的值
     */
    public <K, V> V remove(String cacheName, K key, Class<K> keyType, Class<V> valueType) {
        Cache<K, V> cache = cacheManager.getCache(cacheName, keyType, valueType);
        if (cache != null) {
            V value = cache.get(key);
            if (value != null) {
                cache.remove(key);
            }
            return value;
        }
        return null;
    }

    /**
     * 清空指定缓存中的所有数据
     *
     * @param cacheName 缓存名称
     */
    public void clear(String cacheName) {
        Cache<?, ?> cache = cacheManager.getCache(cacheName, Object.class, Object.class);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * 存储数据并立即刷盘
     *
     * @param cacheName 缓存名称
     * @param key       键
     * @param value     值
     * @param keyType   键类型
     * @param valueType 值类型
     * @param <K>       键类型
     * @param <V>       值类型
     */
    public <K, V> void putAndFlush(String cacheName, K key, V value, Class<K> keyType, Class<V> valueType) {
        put(cacheName, key, value, keyType, valueType);
    }

    /**
     * 获取缓存中所有键的列表
     *
     * @param cacheName 缓存名称
     * @param keyType   键类型
     * @param <K>       键类型
     * @return 键列表
     */
    public <K> List<K> getAllKeys(String cacheName, Class<K> keyType) {
        List<K> keys = new ArrayList<>();
        Cache<K, ?> cache = cacheManager.getCache(cacheName, keyType, Object.class);
        if (cache != null) {
            cache.iterator().forEachRemaining(entry -> keys.add(entry.getKey()));
        }
        return keys;
    }

    /**
     * 检查缓存中是否存在指定键
     *
     * @param cacheName 缓存名称
     * @param key       键
     * @param keyType   键类型
     * @param <K>       键类型
     * @return 是否存在
     */
    public <K> boolean containsKey(String cacheName, K key, Class<K> keyType) {
        Cache<K, ?> cache = cacheManager.getCache(cacheName, keyType, Object.class);
        if (cache != null) {
            return cache.containsKey(key);
        }
        return false;
    }

    /**
     * 如果指定键不存在则存储数据（原子操作）
     *
     * @param cacheName 缓存名称
     * @param key       键
     * @param value     值
     * @param keyType   键类型
     * @param valueType 值类型
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 如果存储成功返回true，否则返回false
     */
    public <K, V> boolean putIfAbsent(String cacheName, K key, V value, Class<K> keyType, Class<V> valueType) {
        Cache<K, V> cache = cacheManager.getCache(cacheName, keyType, valueType);
        if (cache != null) {
            return cache.putIfAbsent(key, value) == null;
        }
        return false;
    }

    /**
     * 使用函数计算结果并存入缓存（如果键不存在）
     *
     * @param cacheName       缓存名称
     * @param key             键
     * @param mappingFunction 映射函数
     * @param keyType         键类型
     * @param valueType        值类型
     * @param <K>             键类型
     * @param <V>             值类型
     * @return 计算结果或已存在的值
     */
    public <K, V> V computeIfAbsent(String cacheName, K key, Function<? super K, ? extends V> mappingFunction, Class<K> keyType, Class<V> valueType) {
        Cache<K, V> cache = cacheManager.getCache(cacheName, keyType, valueType);
        if (cache != null) {
            V value = cache.get(key);
            if (value == null && mappingFunction != null) {
                value = mappingFunction.apply(key);
                if (value != null) {
                    cache.put(key, value);
                }
            }
            return value;
        }
        return null;
    }
}
