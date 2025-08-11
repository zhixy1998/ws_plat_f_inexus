package org.example;

import java.util.List;
import java.util.Map;

public class DynamicCommandService implements AutoCloseable{

    private final Map<String, ProtocolItem> protocolMap;
    private final CommandExecutor executor;

    public DynamicCommandService(String localIp, int localPort, int remotePort, String xmlPath) throws Exception {
        this.protocolMap = ProtocolItem.loadFormXml(xmlPath); // 修改点①
        this.executor = new CommandExecutor(localIp, localPort, remotePort);
    }

    public List<byte[]> executeCommand(String cmdId, Object[] params) throws Exception {
        ProtocolItem cmd = protocolMap.get(cmdId);  // 修改点②
        if (cmd == null) {
            throw new IllegalArgumentException("未找到协议定义: " + cmdId);
        }

        byte[] sendBytes = cmd.calcSendBytes(params);  // 生成发送字节
        List<byte[]> responses = executor.sendAndReceive(cmdId, sendBytes);  // 发送并接收
        return responses;
//        return ResponseParser.parse(cmdId, responses, sendBytes);  // 解析回应
    }

    @Override
    public void close() throws Exception {
        executor.close();
    }
}
