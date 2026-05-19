/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import lombok.Getter;

import java.util.*;

/**
 * @author xudian
 */

@Getter
public enum DevXmlEnums {

    T800("T800", "newFile/T800.xml", "T800", 8000,
            Arrays.asList("Connect", "GetSn", "GetAccessoryType", "SetCellPos", "GetInitStatus", "GetCellPos", "T800GetWlList"),
            Arrays.asList("GetPhotoMode", "GetResponseTime", "LiveMeasure", "GetChangeLampWl", "GetCellPos", "GetLampPos", "GetWLampStatus", "GetDLampStatus", "GetCurrentNegativeHV"),
 new byte[]{(byte) 0xFF, (byte) 0x65, (byte) 0xFF, (byte) 0x07,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x6A},

            new byte[]{(byte) 0xFF, (byte) 0x65, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x6A},
            false),

    T600AndT700("T600/T700", "newFile/T600.1.xml", "T600/T700", 5000,
            Arrays.asList("Connect", "GetSn", "GetAccessoryType", "SetCellPos", "GetInitStatus", "GetCellPos", "GetWlList"),
            Arrays.asList("GetPhotoMode", "GetResponseTime", "Measure", "GetChangeLampWl", "GetCellPos", "GetLampPos","GetWLampStatus","GetDLampStatus"),
            new byte[]{(byte) 0xFF, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBA},
            new byte[]{(byte) 0xFE, (byte) 0xB5, (byte) 0xFF, (byte) 0x07,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB9},
            true);

    // 仪器型号
    private final String devType;
    // xml地址
    private final String xmlPath;
    // 描述
    private final String desc;
    // 发送端口
    private final int port;
    // 连接指令
    private final List<String> connectCommands;

    private final List<String> detectionCommands;
    // 起始报文
    private final byte[] startMessage;

    private final byte[] newIpStartMessage;
    // 设备是否需要数据拟合
    private final boolean needFit;


    private static final Map<String, DevXmlEnums> enums = new HashMap<>();

    static {
        for(DevXmlEnums e : DevXmlEnums.values()) {
            enums.put(e.devType, e);
        }
    }

    // 构造函数
    DevXmlEnums(String devType, String xmlPath, String desc, int port, List<String> connectCommands, List<String> detectionCommands,
                byte[] startMessage, byte[] newIpStartMessage, boolean needFit) {
        this.devType = devType;
        this.xmlPath = xmlPath;
        this.desc = desc;
        this.port = port;
        this.connectCommands = connectCommands;
        this.detectionCommands = detectionCommands;
        this.startMessage = startMessage;
        this.newIpStartMessage = newIpStartMessage;
        this.needFit = needFit;
    }
    public static DevXmlEnums getEnum(String type) {
        return enums.get(type);
    }

    public static List<DevXmlEnums> getAll(){
        return Arrays.asList(values());
    }
}
