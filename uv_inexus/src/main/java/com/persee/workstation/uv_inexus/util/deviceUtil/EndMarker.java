package com.persee.workstation.uv_inexus.util.deviceUtil;

import lombok.Getter;

@Getter
public final class EndMarker {

    public enum Reason {
        TIMEOUT,
        UN_NORMAL,
        NORMAL,
        TIME_STOP,
        NORMAL_STOP,
        NORMAL_OVERPOWER
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

    public static EndMarker unNormal(String sn, String cmd) {
        return new EndMarker(sn, cmd, Reason.UN_NORMAL);
    }

    public static EndMarker Normal (String sn, String cmd) {
        return new EndMarker(sn, cmd, Reason.NORMAL);
    }

    public static EndMarker timeStop (String sn, String cmd) {
        return new EndMarker(sn, cmd, Reason.TIME_STOP);
    }

    public static EndMarker NormalStop (String sn, String cmd) {
        return new EndMarker(sn, cmd, Reason.NORMAL_STOP);
    }

    public static EndMarker unNormalOverPower (String sn, String cmd) {
        return new EndMarker(sn, cmd, Reason.NORMAL_OVERPOWER);
    }
}

