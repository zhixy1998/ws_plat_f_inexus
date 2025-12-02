/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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

