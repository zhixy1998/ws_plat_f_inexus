package com.persee.workstation.uv_inexus.util.nettyUtil.DoIt;

import com.persee.workstation.uv_inexus.util.nettyUtil.DoJS.CommandRequest;
import com.persee.workstation.uv_inexus.util.nettyUtil.DoJS.CommandRequestDTO;
import com.persee.workstation.uv_inexus.util.nettyUtil.SeRePa.DynamicCommandService;

import java.util.HashMap;
import java.util.Map;

public class TestUtil {

    private static String path = "E:\\product\\ws_plat_inexus\\uv_inexus\\src\\main\\java\\com\\persee\\workstation\\uv_inexus\\util\\nettyUtil\\DoIt\\T600.1.xml";

    public static String connect(CommandRequestDTO dto) {
        // 创建 DynamicCommandService 实例
        try {
            DynamicCommandService service = new DynamicCommandService(
                    "169.254.110.67", 6000, 5000, path);

            // 存储所有指令的执行结果
            Map<String, Object> resultMap = new HashMap<>();

            // 遍历接收到的指令
            for (CommandRequestDTO.CommandDTO command : dto.getCommands()) {
                // 创建 CommandRequest 对象
                CommandRequest request = new CommandRequest(command.getCmdId(), command.getParams());

                // 执行指令
                Object result = service.executeCommand(request.getCmdId(), request.getParams());

                // 将结果存入 resultMap
                resultMap.put(request.getCmdId(), result);
                System.out.println("指令 [" + request.getCmdId() + "] 返回结果：" + result);
            }

            // 打印所有指令的执行结果
            System.err.println("所有执行结果：" + resultMap);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
