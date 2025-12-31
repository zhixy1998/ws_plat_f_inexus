/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceOverview;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceStatistics;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionLampStatus;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionResponseInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 最终版设备连接管理器
 * 基于外部DeviceConnectInfo类，管理仪器连接生命周期
 */
@Slf4j
@Component
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
    private static final ConcurrentMap<String, DeviceConnectInfo> deviceMap = new ConcurrentHashMap<>(512);
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
        return deviceMap.get(deviceId);
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
        MessageStoreManager.getInstance().clearDevice(deviceId);

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

    /**
     * 手动断开并立即移除指定设备的所有信息。
     * 适用于用户主动断开、不再需要保留设备上下文的场景。
     *
     * @param deviceId 设备ID，不可为 null
     * @return 是否成功移除
     */
    public boolean removeDevice(String deviceId) {
        if (deviceId == null) {
            logWarning("尝试移除 null 设备ID");
            return false;
        }

        DeviceConnectInfo device = deviceMap.remove(deviceId);
        if (device == null) {
            logInfo("设备不存在，跳过移除: %s", deviceId);
            return false;
        }

        // 清理 identityMap 映射
        String identityKey = device.getConnectIdentity() + ":" + device.getPort();
        identityMap.remove(identityKey, deviceId);

        // 清理关联的消息存储（如果存在）
        MessageStoreManager.getInstance().clearDevice(deviceId);

        logInfo("手动移除设备: %s (%s:%s)", deviceId, device.getConnectIdentity(), device.getPort());
        return true;
    }


    /**
     * 部分更新currentStatus中的字段
     *
     * @param deviceId     设备ID
     * @param statusUpdates 要更新的字段Map
     * @return 是否成功
     */
    public boolean partialUpdateCurrentStatus(String deviceId,
                                              Map<String, Object> statusUpdates) {
        DeviceConnectInfo device = deviceMap.get(deviceId);
        if (device == null || statusUpdates == null || statusUpdates.isEmpty()) {
            return false;
        }

        synchronized (device) {
            DetectionResponseInfoVO currentStatus = device.getCurrentStatus();
            if (currentStatus == null) {
                currentStatus = new DetectionResponseInfoVO();
                device.setCurrentStatus(currentStatus);
            }

            // 使用反射动态更新字段（或者为DetectionResponseInfoVO添加更新方法）
            // 这里先提供简单的手动更新
            for (Map.Entry<String, Object> entry : statusUpdates.entrySet()) {
                switch (entry.getKey()) {
                    case "connectStatus":
                        currentStatus.setConnectStatus((String) entry.getValue());
                        break;
                    case "devCurrentStatus":
                        currentStatus.setDevCurrentStatus((String) entry.getValue());
                        break;
                    case "lampPos":
                        currentStatus.setLampPos((String) entry.getValue());
                        break;
                    case "dataList":
                        currentStatus.setDataList((List<DetectionLampStatus>) entry.getValue());
                        break;
                    case "x":
                        currentStatus.setX((String) entry.getValue());
                        break;
                    case "y":
                        currentStatus.setY((String) entry.getValue());
                        break;
                    case "pos":
                        currentStatus.setPos((String) entry.getValue());
                        break;
                    case "bandwidth":
                        currentStatus.setBandwidth((String) entry.getValue());
                        break;
                    case "changeLampWl":
                        currentStatus.setChangeLampWl((String) entry.getValue());
                        break;
                    case "time":
                        currentStatus.setTime((String) entry.getValue());
                        break;
                    case "attachmentName":
                        currentStatus.setAttachmentName((String) entry.getValue());
                        break;
                    case "attachmentKind":
                        currentStatus.setAttachmentKind((Integer) entry.getValue());
                        break;
                    case "totalPoolNum":
                        currentStatus.setTotalPoolNum((Integer) entry.getValue());
                        break;
                    case "luminosity":
                        currentStatus.setLuminosity((String) entry.getValue());
                        device.setLuminosityMode((String) entry.getValue()); // 同步更新
                        break;
                }
            }

            device.updateLastUseTime();
        }

        logInfo("部分更新设备状态: %s -> %s", deviceId, statusUpdates.keySet());
        return true;
    }

    /**
     * 设置设备序列号
     *
     * @param deviceId 设备ID
     * @param serialNumber 序列号
     * @return 是否成功
     */
    public boolean setDeviceSerialNumber(String deviceId, String serialNumber) {
        DeviceConnectInfo device = deviceMap.get(deviceId);
        if (device == null) {
            logWarning("设置序列号失败: 设备不存在 %s", deviceId);
            return false;
        }

        device.setDeviceSerialNumber(serialNumber);
        device.updateLastUseTime();
        logInfo("设置设备序列号: %s -> %s", deviceId, serialNumber);
        return true;
    }

    /**
     * 设置设备空闲状态
     *
     * @param deviceId 设备ID
     * @param isIdle 是否空闲
     * @return 是否成功
     */
    public boolean setDeviceIdleStatus(String deviceId, Boolean isIdle) {
        DeviceConnectInfo device = deviceMap.get(deviceId);
        if (device == null) {
            logWarning("设置空闲状态失败: 设备不存在 %s", deviceId);
            return false;
        }

        device.setIsIdle(isIdle);
        device.updateLastUseTime();
        logInfo("设置设备空闲状态: %s -> %s", deviceId, isIdle);
        return true;
    }

    /**
     * 添加或更新校零结果
     *
     * @param deviceId 设备ID
     * @param wavelength 波长
     * @param changeParam 校零参数
     * @return 是否成功
     */
    public boolean addZeroResult(String deviceId, String wavelength, ChangeParam changeParam) {
        DeviceConnectInfo device = deviceMap.get(deviceId);
        if (device == null) {
            logWarning("添加校零结果失败: 设备不存在 %s", deviceId);
            return false;
        }

        device.addZeroResult(wavelength, changeParam);
        device.updateLastUseTime();
        logInfo("添加校零结果: %s -> 波长 %s", deviceId, wavelength);
        return true;
    }


    /**
     * 设置当前校零波长
     * @param deviceId 设备ID
     * @param zeroWavelength 校零波长
     * @return 是否成功
     */
    public boolean setZeroWavelength(String deviceId, String zeroWavelength) {
        DeviceConnectInfo device = deviceMap.get(deviceId);
        if (device == null) {
            logWarning("设置校零波长失败: 设备不存在 %s", deviceId);
            return false;
        }

        device.setZeroWavelength(zeroWavelength);
        device.updateLastUseTime();
        logInfo("设置校零波长: %s -> %s", deviceId, zeroWavelength);
        return true;
    }

    /**
     * 设置当前波长
     * @param deviceId 设备ID
     * @param currentWavelength 当前波长
     * @return 是否成功
     */
    public boolean setCurrentWavelength(String deviceId, String currentWavelength) {
        DeviceConnectInfo device = deviceMap.get(deviceId);
        if (device == null) {
            logWarning("设置当前波长失败: 设备不存在 %s", deviceId);
            return false;
        }

        device.setCurrentWavelength(currentWavelength);
        device.updateLastUseTime();
        logInfo("设置当前波长: %s -> %s", deviceId, currentWavelength);
        return true;
    }

    /**
     * 设置光度模式
     * @param deviceId 设备ID
     * @param luminosityMode 光度模式
     * @return 是否成功
     */
    public boolean setLuminosityMode(String deviceId, String luminosityMode) {
        DeviceConnectInfo device = deviceMap.get(deviceId);
        if (device == null) {
            logWarning("设置光度模式失败: 设备不存在 %s", deviceId);
            return false;
        }

        device.setLuminosityMode(luminosityMode);
        device.updateLastUseTime();
        logInfo("设置光度模式: %s -> %s", deviceId, luminosityMode);
        return true;
    }

    /**
     * 获取仪器空闲状态
     */
    public boolean getDeviceIdleStatus(String deviceId) {
        DeviceConnectInfo deviceConnectInfo = deviceMap.get(deviceId);
        if (deviceConnectInfo != null && deviceConnectInfo.getConnectStatus()) {
            return deviceConnectInfo.getIsIdle();
        }
        return false;
    }

    /**
     * 设置扫描波长参数
     */
    public static boolean setScanWavelengthConfig(
            String deviceSerialNumber,
            double startWl,
            double endWl,
            double interval,
            boolean descending
    ) {
        DeviceConnectInfo device = deviceMap.get(deviceSerialNumber);
        if (device == null) {
            logWarning("设置扫描波长失败: 设备不存在 %s", deviceSerialNumber);
            return false;
        }

        ScanWavelengthConfig config = new ScanWavelengthConfig();
        config.setStartWavelength(startWl);
        config.setEndWavelength(endWl);
        config.setInterval(interval);
        config.setDescending(descending);

        device.setScanWavelengthConfig(config);
        device.updateLastUseTime();

        logInfo("设置扫描波长: device=%s start=%s end=%s interval=%s",
                deviceSerialNumber, startWl, endWl, interval);

        return true;
    }

    public ScanWavelengthConfig getScanWavelengthConfig(String deviceId) {
        DeviceConnectInfo device = deviceMap.get(deviceId);
        if (device == null) {
            log.warn("获取扫描波长配置失败: 设备不存在 {}", deviceId);
            return null;
        }

        ScanWavelengthConfig cfg = device.getScanWavelengthConfig();
        if (cfg == null) {
            log.warn("扫描波长配置未设置: deviceId={}", deviceId);
            return null;
        }

        return cfg;
    }



    /**
     * 一次性取出currentStatus的所有字段内容
     *
     * @param deviceId 设备ID
     * @return 包含所有字段的Map，如果设备不存在或currentStatus为null返回空Map
     */
    public Map<String, Object> getCurrentStatusAllFields(String deviceId) {
        DeviceConnectInfo device = deviceMap.get(deviceId);
        if (device == null) {
            return Collections.emptyMap();
        }

        DetectionResponseInfoVO status = device.getCurrentStatus();
        if (status == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> allFields = new LinkedHashMap<>();

        allFields.put("connectStatus", status.getConnectStatus());
        allFields.put("devCurrentStatus", status.getDevCurrentStatus());
        allFields.put("lampPos", status.getLampPos());
        allFields.put("dataList", status.getDataList() != null ?
                Collections.unmodifiableList(status.getDataList()) : Collections.emptyList());
        allFields.put("x", status.getX());
        allFields.put("y", status.getY());
        allFields.put("pos", status.getPos());
        allFields.put("bandwidth", status.getBandwidth());
        allFields.put("changeLampWl", status.getChangeLampWl());
        allFields.put("time", status.getTime());
        allFields.put("attachmentName", status.getAttachmentName());
        allFields.put("attachmentKind", status.getAttachmentKind());
        allFields.put("totalPoolNum", status.getTotalPoolNum());
        allFields.put("luminosity", status.getLuminosity());

        return Collections.unmodifiableMap(allFields);
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

    private static void logInfo(String format, Object... args) {
        String msg = String.format(format, args);
        String time = LocalDateTime.now().toString().substring(11, 19);
        System.out.printf("[%s][DeviceManager] %s%n", time, msg);
    }

    private static void logWarning(String format, Object... args) {
        String msg = String.format(format, args);
        String time = LocalDateTime.now().toString().substring(11, 19);
        System.out.printf("[%s][DeviceManager][WARN] %s%n", time, msg);
    }

    private void logError(Exception e) {
        String time = LocalDateTime.now().toString().substring(11, 19);
        System.err.printf("[%s][DeviceManager][ERROR] %s: %s%n", time, "清理过程异常", e.getMessage());
    }
}
