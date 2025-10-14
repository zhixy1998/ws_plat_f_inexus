/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

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
