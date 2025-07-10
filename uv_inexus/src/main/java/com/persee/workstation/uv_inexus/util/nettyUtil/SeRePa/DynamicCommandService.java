package com.persee.workstation.uv_inexus.util.nettyUtil.SeRePa;/*
    动态执行单条指令
*/

import com.persee.workstation.uv_inexus.util.nettyUtil.ProtocolltemList.ProtocolItem;
import com.persee.workstation.uv_inexus.util.nettyUtil.ProtocolltemList.XmlToCommandList;

import java.util.List;
import java.util.Map;


public class DynamicCommandService {

    private final Map<String, XmlToCommandList.ProtocolItem> protocolMap;
    private final CommandExecutor executor;

    public DynamicCommandService(String localIp, int localPort, int remotePort, String xmlPath) throws Exception {
        this.protocolMap = XmlToCommandList.loadFromXml(xmlPath);
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
        XmlToCommandList.ProtocolItem protoItem = protocolMap.get(cmdId);
        if (protoItem == null) {
            throw new IllegalArgumentException("未找到协议定义: " + cmdId);
        }

        ProtocolItem cmd = new ProtocolItem();
        cmd.ID = protoItem.ID;
        cmd.Describe = protoItem.Describe;
        cmd.Timeout = String.valueOf(protoItem.Timeout);
        cmd.ACK = protoItem.ACK ? "Y" : "N";
        cmd.InputString = protoItem.InputStrings;
        cmd.OutputString = protoItem.OutputStrings;
        cmd.SendBytes = new String[]{protoItem.SendBytes};
        cmd.ReadBytes = new String[]{protoItem.ReadBytes};

        // 生成发送字节
        byte[] sendBytes = cmd.calcSendBytes(params);

        // 发送并接收回应
        List<byte[]> responses = executor.sendAndReceive(cmdId, sendBytes);

        // 解析回应
        return ResponseParser.parse(cmdId, responses,new byte[0]);
    }


//    public void close() {
//        executor.close();
//    }
}

