package com.persee.workstation.uv_inexus.util.nettyUtil.DoJS;

public class CommandDTO {
    private String cmdId;
    private Object[] params;

    public String getCmdId() {
        return cmdId;
    }

    public void setCmdId(String cmdId) {
        this.cmdId = cmdId;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }
}

