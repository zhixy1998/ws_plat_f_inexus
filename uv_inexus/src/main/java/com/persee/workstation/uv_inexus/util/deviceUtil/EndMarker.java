package com.persee.workstation.uv_inexus.util.deviceUtil;

public final class EndMarker {

    public enum Reason {
        TIMEOUT,
        STOP,
        NORMAL
    }

    private final String deviceSn;
    private final String cmdId;
    private final Reason reason;
    private final long time;

    private EndMarker(String deviceSn, String cmdId, Reason reason) {
        this.deviceSn = deviceSn;
        this.cmdId = cmdId;
        this.reason = reason;
        this.time = System.currentTimeMillis();
    }

    public static EndMarker timeout(String sn, String cmd) {
        return new EndMarker(sn, cmd, Reason.TIMEOUT);
    }

    public static EndMarker stop(String sn, String cmd) {
        return new EndMarker(sn, cmd, Reason.STOP);
    }

    public static EndMarker normal(String sn, String cmd) {
        return new EndMarker(sn, cmd, Reason.NORMAL);
    }
}
