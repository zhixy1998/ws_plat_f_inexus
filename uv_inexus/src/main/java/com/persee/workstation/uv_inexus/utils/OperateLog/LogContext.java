package com.persee.workstation.uv_inexus.utils.OperateLog;

public class LogContext {
    private static final ThreadLocal<Object> OLD_DATA = new ThreadLocal<>();
    private static final ThreadLocal<Object> NEW_DATA = new ThreadLocal<>();

    public static void setOldData(Object data) {
        OLD_DATA.set(data);
    }

    public static void setNewData(Object data) {
        NEW_DATA.set(data);
    }

    public static Object getOldData() {
        return OLD_DATA.get();
    }

    public static Object getNewData() {
        return NEW_DATA.get();
    }

    public static void clear() {
        OLD_DATA.remove();
        NEW_DATA.remove();
    }
}
