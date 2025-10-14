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

package com.persee.workstation.uv_inexus.util.nettyUtil.DoIt;


import com.alibaba.fastjson.JSON;
import com.persee.workstation.uv_inexus.util.nettyUtil.DoJS.CommandDTO;
import com.persee.workstation.uv_inexus.util.nettyUtil.DoJS.CommandRequest;
import com.persee.workstation.uv_inexus.util.nettyUtil.DoJS.CommandRequestDTO;
import com.persee.workstation.uv_inexus.util.nettyUtil.SeRePa.DynamicCommandService;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // 假设你从前端获取了一个 JSON 字符串（你可以从请求体获取，下面是示例字符串）
        String jsonString = "{\n" +
                "  \"commands\": [\n" +
                "    {\"cmdId\": \"SearchAround\", \"params\": []},\n" +
                "    {\"cmdId\": \"InitSampleCell\", \"params\": [20]},\n" +
                "    {\"cmdId\": \"SetSlit1\", \"params\": [100]}\n" +
                "  ]\n" +
                "}";

        // 使用 Jackson 解析 JSON 字符串
        CommandRequestDTO dto = JSON.parseObject(jsonString, CommandRequestDTO.class);
        // 创建 DynamicCommandService 实例
        try {
            DynamicCommandService service = new DynamicCommandService(
                    "169.254.136.28", 6000, 5000, "D:\\project\\java\\T600.1.xml");

            // 存储所有指令的执行结果
            Map<String, Object> resultMap = new HashMap<>();

            // 遍历接收到的指令
            for (CommandDTO command : dto.getCommands()) {
                // 创建 CommandRequest 对象
                CommandRequest request = new CommandRequest(command.getCmdId(), command.getParams());

                // 执行指令
                Object result = service.executeCommand(request.getCmdId(), request.getParams());

                // 将结果存入 resultMap
                resultMap.put(request.getCmdId(), result);
                System.out.println("指令 [" + request.getCmdId() + "] 返回结果：" + result);
            }

            // 打印所有指令的执行结果
            System.out.println("所有执行结果：" + resultMap);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

