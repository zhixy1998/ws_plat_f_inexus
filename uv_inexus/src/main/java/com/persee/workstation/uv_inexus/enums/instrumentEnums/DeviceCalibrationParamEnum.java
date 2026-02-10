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
