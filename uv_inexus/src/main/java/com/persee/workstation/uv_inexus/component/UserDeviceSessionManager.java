package com.persee.workstation.uv_inexus.component;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户设备会话管理器
 * 用于管理用户与多台设备的绑定关系（内存级别，不持久化）
 */
@Slf4j
@Component
public class UserDeviceSessionManager {

    // 用户 -> 设备集合
    private static final Map<String, Set<String>> userDeviceMap = new ConcurrentHashMap<>();

    // 设备 -> 用户（反向索引，便于快速查找）
    private static final Map<String, String> deviceUserMap = new ConcurrentHashMap<>();

    /**
     * 绑定用户与设备
     */
    public synchronized void bindUserDevice(String userId, String deviceSerialNumber) {
        if (userId == null || deviceSerialNumber == null) {
            log.warn("绑定失败：userId 或 deviceSerialNumber 为 null");
            return;
        }

        // 检查设备是否已被其他用户绑定
        String existingUser = deviceUserMap.get(deviceSerialNumber);
        if (existingUser != null && !existingUser.equals(userId)) {
            log.warn("设备已被其他用户绑定：device={}, existingUser={}", deviceSerialNumber, existingUser);
        }

        userDeviceMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(deviceSerialNumber);
        deviceUserMap.put(deviceSerialNumber, userId);

        log.info("绑定用户设备：userId={}, device={}, 该用户总设备数={}",
                userId, deviceSerialNumber, userDeviceMap.get(userId).size());
    }

    /**
     * 解绑用户与设备
     */
    public synchronized void unbindUserDevice(String userId, String deviceSerialNumber) {
        if (userId == null || deviceSerialNumber == null) {
            return;
        }

        Set<String> devices = userDeviceMap.get(userId);
        if (devices != null) {
            boolean removed = devices.remove(deviceSerialNumber);
            if (removed) {
                deviceUserMap.remove(deviceSerialNumber);
                if (devices.isEmpty()) {
                    userDeviceMap.remove(userId);
                }
                log.info("解绑用户设备：userId={}, device={}", userId, deviceSerialNumber);
            }
        }
    }

    /**
     * 获取用户的所有设备序列号
     */
    public Set<String> getUserDevices(String userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        Set<String> devices = userDeviceMap.get(userId);
        return devices != null ? Collections.unmodifiableSet(devices) : Collections.emptySet();
    }

    /**
     * 获取设备所属用户
     */
    public String getDeviceUser(String deviceSerialNumber) {
        return deviceUserMap.get(deviceSerialNumber);
    }

    /**
     * 检查用户是否已连接该设备
     */
    public boolean isUserDeviceBound(String userId, String deviceSerialNumber) {
        if (userId == null || deviceSerialNumber == null) {
            return false;
        }
        Set<String> devices = userDeviceMap.get(userId);
        return devices != null && devices.contains(deviceSerialNumber);
    }

    /**
     * 清理用户的所有设备绑定（用户断开连接时调用）
     */
    public synchronized void cleanupUser(String userId) {
        if (userId == null) {
            return;
        }

        Set<String> devices = userDeviceMap.remove(userId);
        if (devices != null) {
            devices.forEach(deviceUserMap::remove);
            log.info("清理用户所有设备绑定：userId={}, 设备数={}", userId, devices.size());
        }
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineUserCount() {
        return userDeviceMap.size();
    }

    /**
     * 获取用户的设备数量
     */
    public int getUserDeviceCount(String userId) {
        Set<String> devices = userDeviceMap.get(userId);
        return devices != null ? devices.size() : 0;
    }
}
