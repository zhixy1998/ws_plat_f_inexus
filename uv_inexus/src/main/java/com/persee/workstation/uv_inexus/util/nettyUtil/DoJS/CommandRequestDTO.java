package com.persee.workstation.uv_inexus.util.nettyUtil.DoJS;/*
    映射前端发送的 命令列表，用于接收 JSON 数据
* */


import java.util.List;

public class CommandRequestDTO {
    private List<CommandDTO> commands;

    public List<CommandDTO> getCommands() {
        return commands;
    }

    public void setCommands(List<CommandDTO> commands) {
        this.commands = commands;
    }

    public static class CommandDTO {
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
}
