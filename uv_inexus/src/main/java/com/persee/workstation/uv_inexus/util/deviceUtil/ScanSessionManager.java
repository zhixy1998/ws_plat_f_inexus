package com.persee.workstation.uv_inexus.util.deviceUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScanSessionManager {

    // deviceId -> 是否正在扫描
    private static final ConcurrentHashMap<String, AtomicBoolean> scanRunningMap = new ConcurrentHashMap<String, AtomicBoolean>();

    public static void startScan(String deviceId) {
        scanRunningMap
                .computeIfAbsent(deviceId, k -> new AtomicBoolean())
                .set(true);
    }

    public static void stopScan(String deviceId) {
        AtomicBoolean flag = scanRunningMap.get(deviceId);
        if (flag != null) {
            flag.set(false);
        }
    }

    public static boolean isRunning(String deviceId) {
        AtomicBoolean flag = scanRunningMap.get(deviceId);
        return flag != null && flag.get();
    }

    public static void clear(String deviceId) {
        scanRunningMap.remove(deviceId);
    }
}

