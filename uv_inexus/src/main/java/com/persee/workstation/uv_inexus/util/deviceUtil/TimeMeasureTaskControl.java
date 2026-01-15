package com.persee.workstation.uv_inexus.util.deviceUtil;

import java.util.concurrent.ScheduledExecutorService;

public class TimeMeasureTaskControl {
    private final ScheduledExecutorService scheduler;
    private volatile boolean stopped = false;

    public TimeMeasureTaskControl(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    public void stop() {
        stopped = true;
        scheduler.shutdownNow(); // 立即打断
    }

    public boolean isStopped() {
        return stopped;
    }
}

