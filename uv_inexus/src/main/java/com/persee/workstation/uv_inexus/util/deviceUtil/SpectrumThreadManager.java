package com.persee.workstation.uv_inexus.util.deviceUtil;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SpectrumThreadManager {

    /** Scan 线程 */
    private final Map<String, Thread> scanThreadMap = new ConcurrentHashMap<>();

    /** Baseline 线程 */
    private final Map<String, Thread> baselineThreadMap = new ConcurrentHashMap<>();

    /** 统一出口（关键） */
    public Map<String, Thread> getTargetThreadMap(boolean isScan) {
        return isScan ? scanThreadMap : baselineThreadMap;
    }

    public void putThread(String deviceSerial, boolean isScan, Thread thread) {
        getTargetThreadMap(isScan).put(deviceSerial, thread);
    }

    public void stopAndRemove(String deviceSerial, boolean isScan) {
        Thread t = getTargetThreadMap(isScan).remove(deviceSerial);
        if (t != null) {
            t.interrupt();
        }
    }

    public boolean isRunning(String deviceSerial, boolean isScan) {
        Thread t = getTargetThreadMap(isScan).get(deviceSerial);
        return t != null && t.isAlive();
    }
}

