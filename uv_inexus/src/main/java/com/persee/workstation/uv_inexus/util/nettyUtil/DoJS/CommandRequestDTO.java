package com.persee.workstation.uv_inexus.util.nettyUtil.DoJS;

import java.util.List;

/*
    映射前端发送的 命令列表，用于接收 JSON 数据
* */
public class CommandRequestDTO {
    private List<CommandDTO> commands;

    public List<CommandDTO> getCommands() {
        return commands;
    }

    public void setCommands(List<CommandDTO> commands) {
        this.commands = commands;
    }
}
