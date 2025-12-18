/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import com.persee.workstation.uv_inexus.util.changeUtil.Transition;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 线程安全的双Map容器（简化版）
 * Map1: 一次性写入，多线程读取
 * Map2: Map<String, String>，多线程读写
 */
@Component
public class DeviceXmlManage {


    // Map1: 一次性写入，多线程读取
    private final Map<String, Map<String, Transition>> allXmlMap = new ConcurrentHashMap<>();
    private volatile boolean allXmlInitialized = false;

    // Map2: Map<String, String>，多线程读写
    private final Map<String, String> deviceMap = new ConcurrentHashMap<>();

    // ==================== Map1 操作 ====================

    /**
     * 初始化Map1（一次性）
     */
    public synchronized void initAllXml(Map<String, Map<String, Transition>> data) {
        if (!allXmlInitialized) {
            allXmlMap.putAll(data);
            allXmlInitialized = true;
        }
    }

    /**
     * 获取Map1的值（只读）
     */
    public Map<String, Transition> getXmlInfo(String key) {
        Map<String, Transition> value = allXmlMap.get(key);
        return value == null ? null : Collections.unmodifiableMap(value);
    }

    // ==================== Map2 操作 ====================

    /**
     * 设置Map2的值
     */
    public String setDevice(String key, String value) {
        return deviceMap.put(key, value);
    }

    /**
     * 获取Map2的值
     */
    public String getDevType(String key) {
        return deviceMap.get(key);
    }

    public boolean containsKeyInDeviceMap(String key) {
        return deviceMap.containsKey(key);
    }

    public boolean containsValueInDeviceMap(String value) {
        return deviceMap.containsValue(value);
    }

    /**
     * 删除Map2的键
     */
    public String removeDevice(String key) {
        return deviceMap.remove(key);
    }

    /**
     * 获取所有Map2数据
     */
    public Map<String, String> getAllDevice() {
        return new HashMap<>(deviceMap); // 返回副本
    }

    /**
     * 获取Map2大小
     */
    public int getMap2Size() {
        return deviceMap.size();
    }
}