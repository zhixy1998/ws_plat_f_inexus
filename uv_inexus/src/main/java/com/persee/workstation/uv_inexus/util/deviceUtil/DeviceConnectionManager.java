/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceOverview;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceStatistics;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 最终版设备连接管理器
 * 基于外部DeviceConnectInfo类，管理仪器连接生命周期
 */
public class DeviceConnectionManager {

    private static final DeviceConnectionManager INSTANCE = new DeviceConnectionManager();

    private DeviceConnectionManager() {
        startCleanupService();
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public static DeviceConnectionManager getInstance() {
        return INSTANCE;
    }

    // ==================== 核心数据结构 ====================
    private final ConcurrentMap<String, DeviceConnectInfo> deviceMap = new ConcurrentHashMap<>(512);
    private final ConcurrentMap<String, String> identityMap = new ConcurrentHashMap<>(512);

    // ==================== 统计辅助 ====================
    private final AtomicInteger cleanupCount = new AtomicInteger(0);
    private volatile LocalDateTime lastCleanupTime;

    // ==================== 配置参数 ====================
    private volatile int inactiveTimeoutMinutes = 300;
    private volatile int disconnectedTimeoutMinutes = 5;

    // ==================== 清理服务 ====================
    private ScheduledExecutorService cleanupService;
    private volatile boolean running = true;

    // ==================== 核心API ====================

    public String getOrCreateDeviceId(String connectIdentity, Integer port, Integer connectType) {
        String key = connectIdentity + ":" + port;

        String existingId = identityMap.get(key);
        if (existingId != null) {
            DeviceConnectInfo device = deviceMap.get(existingId);
            if (device != null) {
                device.recordMessage();
                return existingId;
            }
            identityMap.remove(key, existingId);
        }

        return createNewDevice(connectIdentity, port, key, connectType);
    }

    private String createNewDevice(String connectIdentity, Integer port, String key, Integer connectType) {
        DeviceConnectInfo newDevice = new DeviceConnectInfo(connectIdentity, port, connectType);
        newDevice.recordMessage();
        String newId = newDevice.getId();

        String existingId = identityMap.putIfAbsent(key, newId);
        if (existingId != null) {
            DeviceConnectInfo existingDevice = deviceMap.get(existingId);
            if (existingDevice != null) {
                existingDevice.recordMessage();
                return existingId;
            }
            identityMap.remove(key, existingId);
        }

        deviceMap.put(newId, newDevice);
        logInfo("创建新设备: %s:%s -> %s", connectIdentity, port, newId);
        return newId;
    }

    public DeviceConnectInfo getDeviceForMessage(String deviceId) {
        DeviceConnectInfo device = deviceMap.get(deviceId);
        if (device != null) {
            device.recordMessage();
        }
        return device;
    }

    /**
     * 获取所有临时设备的 ID 列表（即 id 以 "temp--" 开头的设备）
     *
     * @return 不可变的临时设备 ID 列表，按内部顺序返回（无特定排序）
     */
    public List<String> getAllTempDeviceIds() {
        List<String> temporaryIds = new ArrayList<>();
        for (DeviceConnectInfo device : deviceMap.values()) {
            if (device.isTemporary()) {
                temporaryIds.add(device.getId());
            }
        }
        return Collections.unmodifiableList(temporaryIds);
    }

    public DeviceConnectInfo getDevice(String deviceId) {
        DeviceConnectInfo deviceConnectInfo = deviceMap.get(deviceId);
        return deviceConnectInfo;
    }

    public String convertToPermanent(String connectIdentity, Integer port) {
        String key = connectIdentity + ":" + port;
        String tempId = identityMap.get(key);

        if (tempId == null) {
            logWarning("转换失败: 未找到设备 %s:%s", connectIdentity, port);
            return null;
        }

        DeviceConnectInfo device = deviceMap.get(tempId);
        if (device == null) {
            logWarning("转换失败: 设备信息不存在 %s", tempId);
            identityMap.remove(key, tempId);
            return null;
        }

        if (!device.isTemporary()) {
            return tempId;
        }

        DeviceConnectInfo permanentDevice = device.createCopy();
        permanentDevice.convertToPermanent();
        String permanentId = permanentDevice.getId();

        deviceMap.put(permanentId, permanentDevice);
        identityMap.put(key, permanentId);
        deviceMap.remove(tempId, device);

        logInfo("转换为永久标识: %s -> %s", tempId, permanentId);
        return permanentId;
    }

    /**
     * 将指定的临时设备转换为永久设备（推荐使用）
     *
     * @param deviceId 临时设备的 ID（必须以 "temp--" 开头）
     * @return 永久设备的新 ID，若设备不存在或已是永久则返回原 ID
     */
    public String convertToPermanentByDeviceId(String deviceId) {
        if (deviceId == null) {
            logWarning("转换失败: deviceId 为 null");
            return null;
        }

        DeviceConnectInfo device = deviceMap.get(deviceId);
        if (device == null) {
            logWarning("转换失败: 设备不存在 %s", deviceId);
            return null;
        }

        if (!device.isTemporary()) {
            return deviceId; // 已是永久设备，直接返回
        }

        // 创建永久副本
        DeviceConnectInfo permanentDevice = device.createCopy();
        permanentDevice.convertToPermanent();
        String permanentId = permanentDevice.getId();

        // 原子性更新
        deviceMap.put(permanentId, permanentDevice);

        // 更新 identityMap：将 (ip:port) 映射到新 permanentId
        String identityKey = device.getIdentityKey(); // 假设 DeviceConnectInfo 有此方法
        if (identityKey != null) {
            identityMap.put(identityKey, permanentId);
        }

        // 移除旧临时设备
        deviceMap.remove(deviceId);

        logInfo("转换为永久标识: %s -> %s", deviceId, permanentId);
        return permanentId;
    }

    public void updateDeviceInfo(String deviceId, Boolean connectStatus, String macInfo, String xmlKey) {
        DeviceConnectInfo device = deviceMap.get(deviceId);
        if (device == null) return;

        boolean wasConnected = Boolean.TRUE.equals(device.getConnectStatus());
        device.setConnectStatus(connectStatus);

        if (macInfo != null) device.setMacInfo(macInfo);
        if (xmlKey != null) device.setXmlKey(xmlKey);
        device.updateLastUseTime();
    }

    public String getIpByDeviceId(String deviceId) {
        DeviceConnectInfo device = deviceMap.get(deviceId);
        return device != null ? device.getConnectIdentity() : null;
    }

    public Integer getPortByDeviceId(String deviceId) {
        DeviceConnectInfo device = deviceMap.get(deviceId);
        return device != null ? device.getPort() : null;
    }

    public void disconnectDevice(String deviceId) {
        updateDeviceInfo(deviceId, false, null, null);
        logInfo("设备断开连接: %s", deviceId);
    }

    public Set<String> getAllDeviceIds() {
        return Collections.unmodifiableSet(new HashSet<>(deviceMap.keySet()));
    }

    public List<DeviceConnectInfo> getConnectedDevices() {
        List<DeviceConnectInfo> connected = new ArrayList<>();
        for (DeviceConnectInfo device : deviceMap.values()) {
            if (Boolean.TRUE.equals(device.getConnectStatus())) {
                connected.add(device);
            }
        }
        return connected;
    }

    public int getDeviceCount() {
        return deviceMap.size();
    }

    // ==================== 统计信息 ====================

    public DeviceStatistics getStatistics() {
        DeviceStatistics stats = new DeviceStatistics();

        int total = 0, connected = 0, temp = 0;
        long messages = 0;

        for (DeviceConnectInfo device : deviceMap.values()) {
            total++;
            messages += device.getMessageCount();
            if (Boolean.TRUE.equals(device.getConnectStatus())) {
                connected++;
            }
            if (device.isTemporary()) {
                temp++;
            }
        }

        stats.totalDevices = total;
        stats.connectedDevices = connected;
        stats.temporaryDevices = temp;
        stats.permanentDevices = total - temp;
        stats.totalMessages = messages;
        stats.cleanupCount = cleanupCount.get();
        stats.lastCleanupTime = lastCleanupTime;
        stats.averageMessagesPerDevice = total > 0 ? (double) messages / total : 0;

        return stats;
    }

    public DeviceOverview getDeviceOverview() {
        DeviceOverview overview = new DeviceOverview();
        overview.totalDevices = deviceMap.size();

        for (DeviceConnectInfo device : deviceMap.values()) {
            if (Boolean.TRUE.equals(device.getConnectStatus())) {
                overview.connectedDevices++;
            }
            if (device.isTemporary()) {
                overview.temporaryDevices++;
            }
            overview.totalMessages += device.getMessageCount();
        }

        overview.permanentDevices = overview.totalDevices - overview.temporaryDevices;
        return overview;
    }

    // ==================== 清理功能 ====================

    private void startCleanupService() {
        cleanupService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DeviceManager-Cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupService.scheduleWithFixedDelay(this::cleanupExpiredDevices, 5, 5, TimeUnit.MINUTES);
        logInfo("清理服务已启动");
    }

    private void cleanupExpiredDevices() {
        if (!running) return;

        try {
            List<String> toRemove = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (Map.Entry<String, DeviceConnectInfo> entry : deviceMap.entrySet()) {
                DeviceConnectInfo device = entry.getValue();
                LocalDateTime lastUse = device.getLastUseTime();
                if (lastUse == null) continue;

                long minutesInactive = java.time.Duration.between(lastUse, now).toMinutes();
                boolean shouldRemove = Boolean.TRUE.equals(device.getConnectStatus())
                        ? minutesInactive > inactiveTimeoutMinutes
                        : minutesInactive > disconnectedTimeoutMinutes;

                if (shouldRemove) {
                    toRemove.add(entry.getKey());
                }
            }

            int removedTotal = 0;
            for (String deviceId : toRemove) {
                DeviceConnectInfo device = deviceMap.remove(deviceId);
                if (device != null) {
                    identityMap.remove(device.getIdentityKey(), deviceId);
                    removedTotal++;
                    logInfo("清理过期设备: %s (%s:%s)", deviceId, device.getConnectIdentity(), device.getPort());
                }
            }

            if (removedTotal > 0) {
                lastCleanupTime = LocalDateTime.now();
                cleanupCount.incrementAndGet();
                logInfo("清理完成: 移除 %d 个设备", removedTotal);
            }

        } catch (Exception e) {
            logError(e);
        }
    }

    public void triggerCleanup() {
        if (cleanupService != null) {
            cleanupService.submit(this::cleanupExpiredDevices);
        }
    }

    // ==================== 配置方法 ====================

    public void setInactiveTimeoutMinutes(int minutes) {
        if (minutes <= 0) throw new IllegalArgumentException("inactiveTimeoutMinutes must be > 0");
        this.inactiveTimeoutMinutes = minutes;
        logInfo("设置无活动超时: %d 分钟", minutes);
    }

    public void setDisconnectedTimeoutMinutes(int minutes) {
        if (minutes <= 0) throw new IllegalArgumentException("disconnectedTimeoutMinutes must be > 0");
        this.disconnectedTimeoutMinutes = minutes;
        logInfo("设置断开连接清理时间: %d 分钟", minutes);
    }

    // ==================== 生命周期 ====================

    public void shutdown() {
        running = false;
        if (cleanupService != null) {
            cleanupService.shutdown();
            try {
                if (!cleanupService.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupService.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        DeviceStatistics stats = getStatistics();
        logInfo("设备管理器已关闭。最终统计: %s", stats);
    }

    public void reset() {
        // 不调用 shutdown，避免服务中断
        if (cleanupService != null) {
            cleanupService.shutdownNow();
        }

        deviceMap.clear();
        identityMap.clear();
        cleanupCount.set(0);
        lastCleanupTime = null;
        running = true;

        startCleanupService();
        logInfo("设备管理器已重置");
    }

    // ==================== 日志 ====================

    private void logInfo(String format, Object... args) {
        String msg = String.format(format, args);
        String time = LocalDateTime.now().toString().substring(11, 19);
        System.out.printf("[%s][DeviceManager] %s%n", time, msg);
    }

    private void logWarning(String format, Object... args) {
        String msg = String.format(format, args);
        String time = LocalDateTime.now().toString().substring(11, 19);
        System.out.printf("[%s][DeviceManager][WARN] %s%n", time, msg);
    }

    private void logError(Exception e) {
        String time = LocalDateTime.now().toString().substring(11, 19);
        System.err.printf("[%s][DeviceManager][ERROR] %s: %s%n", time, "清理过程异常", e.getMessage());
    }
}
