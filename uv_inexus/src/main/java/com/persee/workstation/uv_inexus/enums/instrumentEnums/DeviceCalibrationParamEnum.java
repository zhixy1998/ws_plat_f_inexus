/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.enums.instrumentEnums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum DeviceCalibrationParamEnum {

    WaveBrand("wavebrand","光谱带宽"),
    Start("start", "起始波长"),
    End("end", "终止波长"),
    ScanSpeed("scanSpeed", "扫描速度"),
    Table("table", "表数据"),
    Graph("graph", "图表数据"),

    //光度模式，标准波长，实际波长
    Name("name", "物质名称"),
    Mode("luminosity", "光度模式"),
    StandWavelength("standLength", "标准波长"),
    RealWavelength("realLength", "实际波长"),
    Gain("gain", "增益"),
    Lamp("lamp", "灯类型"),
    IsSendToDevice("isSendToDevice", "是否已经发送给设备"),
    Identifier("identifier", "物质标识"),

    Baseline("baseline", "基线"),
    Scan("scan", "扫描"),

    Send("1", "测试过"),
    NoSend("2", "未测试"),

    XName("波长(nm)", "X轴名称"),
    Peak("1", "峰"),

    Step("step", "扫描间隔");


    private final String type;

    private final String description;

    private static final Map<String, DeviceCalibrationParamEnum> map = new HashMap<>();

    static {
        for(DeviceCalibrationParamEnum e : DeviceCalibrationParamEnum.values()) {
            map.put(e.type,e);
        }
    }

    DeviceCalibrationParamEnum(String type, String description){
        this.type = type;
        this.description = description;
    }



    public static String getDescription(String type) {
        if(map.containsKey(type)) {

            return map.get(type).getDescription();
        }
        throw new IllegalArgumentException("Invalid key: " + type);
    }

    public static Integer getLength(){
        return map.size();
    }

}
