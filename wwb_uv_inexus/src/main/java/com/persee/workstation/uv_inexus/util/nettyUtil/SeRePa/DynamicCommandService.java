/*
    动态执行单条指令
*/
package com.persee.workstation.uv_inexus.util.nettyUtil.SeRePa;

import com.persee.workstation.uv_inexus.util.nettyUtil.ProtocolltemList.ProtocolItem;

import java.util.List;
import java.util.Map;

public class DynamicCommandService implements AutoCloseable {

    private final Map<String, ProtocolItem> protocolMap;
    private final CommandExecutor executor;

    public DynamicCommandService(String localIp, int localPort, int remotePort, String xmlPath) throws Exception {
        this.protocolMap = ProtocolItem.loadFromXml(xmlPath); // 修改点①
        this.executor = new CommandExecutor(localIp, localPort, remotePort);
    }

    /**
     * 动态执行单条指令
     * @param cmdId 指令ID
     * @param params 指令参数数组
     * @return 解析后的结果对象
     * @throws Exception
     */
    public Object executeCommand(String cmdId, Object[] params) throws Exception {
        ProtocolItem cmd = protocolMap.get(cmdId);  // 修改点②
        if (cmd == null) {
            throw new IllegalArgumentException("未找到协议定义: " + cmdId);
        }

        byte[] sendBytes = cmd.calcSendBytes(params);  // 生成发送字节
        List<byte[]> responses = executor.sendAndReceive(cmdId, sendBytes);  // 发送并接收
        return ResponseParser.parse(cmdId, responses, sendBytes);  // 解析回应
    }

    @Override
    public void close() {
        executor.close();
    }
}

