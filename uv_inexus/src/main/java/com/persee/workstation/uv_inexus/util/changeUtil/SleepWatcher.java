package com.persee.workstation.uv_inexus.util.changeUtil;

public class SleepWatcher implements Runnable {

    /** 休眠检测间隔（毫秒） */
    private final long checkInterval = 2000;

    /** 判定为“系统休眠”的时间间隔阈值（毫秒） */
    private final long sleepThreshold = 10000;

    /** 上一次检测时间 */
    private long lastTime = System.currentTimeMillis();

    /** 回调接口 */
    private final OnWakeListener listener;

    public interface OnWakeListener {
        void onWake(); // 唤醒后执行的逻辑
    }

    public SleepWatcher(OnWakeListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        System.out.println("SleepWatcher 启动中...");
        while (true) {
            try {
                Thread.sleep(checkInterval);

                long now = System.currentTimeMillis();
                long diff = now - lastTime;

                // 如果时间间隔明显超过检测间隔，说明系统可能休眠过
                if (diff > sleepThreshold) {
                    System.out.println("检测到系统休眠/唤醒，差值：" + diff + " ms");
                    if (listener != null) {
                        listener.onWake(); // 触发唤醒逻辑
                    }
                }

                lastTime = now;
            } catch (InterruptedException e) {
                System.out.println("SleepWatcher 停止检测。");
                break;
            }
        }
    }
}

