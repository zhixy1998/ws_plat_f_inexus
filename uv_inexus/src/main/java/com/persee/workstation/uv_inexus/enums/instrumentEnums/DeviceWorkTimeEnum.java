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
public enum DeviceWorkTimeEnum {

    DEVICE_TOTAL_WORKING_TIME("11","仪器总工作时长"),
    TUNGSTEN_LAMP_TOTAL_WORKING_TIME("12","钨灯工作时长"),
    DEUTERIUM_LAMP_TOTAL_WORKING_TIME("13","氘灯总工作时长"),

    DEVICE_TODAY_WORKING_TIME("14","仪器此次开机工作时长"),
    TUNGSTEN_LAMP_TODAY_WORKING_TIME("15","钨灯此次开机工作时长"),
    DEUTERIUM_LAMP_TODAY_WORKING_TIME("16","氘灯此次工作时长");

    private final String code;

    private final String name;

    private static final Map<String, String> map = new HashMap<>();

    static {
        for(DeviceWorkTimeEnum e : DeviceWorkTimeEnum.values()) {
            map.put(e.code,e.name);
        }
    }

    private DeviceWorkTimeEnum(String code, String name){
        this.code = code;
        this.name = name;
    }

    public static String getName(String code) {
        if(map.containsKey(code)) {
            return map.get(code);
        }
        throw new IllegalArgumentException("Invalid key: " + code);
    }
}
