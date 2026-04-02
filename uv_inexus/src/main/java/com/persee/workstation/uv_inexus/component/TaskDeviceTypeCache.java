package com.persee.workstation.uv_inexus.component;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.persee.workstation.uv_inexus.enums.Cache.CacheEnums.DEFAULT_CACHE;

/**
 * 任务 - 设备型号缓存包装类
 * 内部使用 Ehcache 实现，支持跨类访问和持久化
 */
@Component
public class TaskDeviceTypeCache {
    @Autowired
    private EhcacheUtil ehcacheUtil;

    private static final String CACHE_KEY_PREFIX = "task_device_type:";
    /**
     * 从缓存获取设备型号
     */
    public String get(String taskId) {
        Object value = ehcacheUtil.get(
                DEFAULT_CACHE.getCacheName(),
                CACHE_KEY_PREFIX + taskId,
                String.class,
                Object.class
        );
        return value != null ? (String) value : null;
    }

    /**
     * 向缓存放入设备型号
     */
    public void put(String taskId, String deviceType) {
        ehcacheUtil.put(
                DEFAULT_CACHE.getCacheName(),
                CACHE_KEY_PREFIX + taskId,
                deviceType,
                String.class,
                Object.class
        );
    }

    /**
     * 检查缓存是否存在
     */
    public boolean containsKey(String taskId) {
        return ehcacheUtil.containsKey(
                DEFAULT_CACHE.getCacheName(),
                CACHE_KEY_PREFIX + taskId,
                String.class
        );
    }

    /**
     * 从缓存移除
     */
    public void remove(String taskId) {
        ehcacheUtil.remove(
                DEFAULT_CACHE.getCacheName(),
                CACHE_KEY_PREFIX + taskId,
                String.class,
                Object.class
        );
    }
}

