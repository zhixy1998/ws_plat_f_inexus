/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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

    Wavelength(1,"CorrectWl", "波长校正"),
    T800Dark(3, "Dark", "T800暗电流校正"),
    Dark(2, "Dark", "暗电流校正");


    private final Integer type;

    private final String comId;

    private final String description;

    private static final Map<Integer, DeviceCalibrationInfoEnum> map = new HashMap<>();

    static {
        for(DeviceCalibrationInfoEnum e : DeviceCalibrationInfoEnum.values()) {
            map.put(e.type,e);
        }
    }

    DeviceCalibrationInfoEnum(Integer type, String comId, String description){
        this.type = type;
        this.comId = comId;
        this.description = description;
    }


    public static String getComId(Integer type) {
        if(map.containsKey(type)) {

            return map.get(type).getComId();
        }
        throw new IllegalArgumentException("Invalid key: " + type);
    }

    public static String getDescription(Integer type) {
        if(map.containsKey(type)) {

            return map.get(type).getDescription();
        }
        throw new IllegalArgumentException("Invalid key: " + type);
    }

    public static Integer getLength(){
        return map.size();
    }
}
