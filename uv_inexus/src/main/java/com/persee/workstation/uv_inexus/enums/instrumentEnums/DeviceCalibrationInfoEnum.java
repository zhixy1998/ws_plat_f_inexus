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
public enum DeviceCalibrationInfoEnum {

    Wavelength(1,"CheckWl"),
    Dark(2, "Dark");


    private final Integer type;

    private final String comId;

    private static final Map<Integer, String> map = new HashMap<>();

    static {
        for(DeviceCalibrationInfoEnum e : DeviceCalibrationInfoEnum.values()) {
            map.put(e.type,e.comId);
        }
    }

    private DeviceCalibrationInfoEnum(Integer type, String comId){
        this.type = type;
        this.comId = comId;
    }


    public static String getComId(Integer type) {
        if(map.containsKey(type)) {
            return map.get(type);
        }
        throw new IllegalArgumentException("Invalid key: " + type);
    }

    public static Integer getLength(){
        return map.size();
    }
}
