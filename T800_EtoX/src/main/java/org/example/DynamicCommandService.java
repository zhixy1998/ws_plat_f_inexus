package org.example;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DynamicCommandService implements AutoCloseable{

    private final Map<String, ProtocolItem> protocolMap;
    private final CommandExecutor executor;
    public static String sendIp = "192.168.51.36";

    public DynamicCommandService(String localIp, int localPort, int remotePort, String xmlPath) throws Exception {
        this.protocolMap = ProtocolItem.loadFormXml(xmlPath); // 修改点①
        this.executor = new CommandExecutor(localIp, localPort, remotePort, xmlPath);
    }

    public List<byte[]> executeCommand(String cmdId, Object[] params) throws Exception {
        ProtocolItem cmd = protocolMap.get(cmdId);  // 修改点②
        if (cmd == null) {
            throw new IllegalArgumentException("未找到协议定义: " + cmdId);
        }

        byte[] sendBytes = cmd.calcSendBytes(params);  // 生成发送字节
        executor.sendCommand(cmdId, sendBytes, sendIp);// 发送报文

        return null;
    }

    @Override
    public void close() throws Exception {
        executor.close();
    }
}
