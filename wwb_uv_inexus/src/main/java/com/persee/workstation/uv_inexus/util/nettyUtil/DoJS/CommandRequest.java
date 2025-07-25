package com.persee.workstation.uv_inexus.util.nettyUtil.DoJS;

public class CommandRequest {
    private String cmdId;
    private Object[] params;

    public CommandRequest() {} // 必须有无参构造

    public CommandRequest(String cmdId, Object[] params) {
        this.cmdId = cmdId;
        this.params = params;
    }

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

