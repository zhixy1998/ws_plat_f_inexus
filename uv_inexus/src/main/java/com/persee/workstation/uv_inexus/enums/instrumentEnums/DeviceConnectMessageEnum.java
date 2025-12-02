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
public enum DeviceConnectMessageEnum {
//0-未连接，1-联机中，2-联机成功，3-联机失败

    CONNECT_NO(0,"未联机"),
    CONNECT_ING(1, "联机中"),
    CONNECT_SUCCESS(2,"联机成功"),
    CONNECT_FALSE(3,"联机失败"),
    CONNECT_STOP(4,"联机断开");

    private final Integer code;

    private final String name;

    private static final Map<Integer, String> map = new HashMap<>();

    static {
        for(DeviceConnectMessageEnum e : DeviceConnectMessageEnum.values()) {
            map.put(e.code,e.name);
        }
    }

    DeviceConnectMessageEnum(Integer code, String name){
        this.code = code;
        this.name = name;
    }


    public static String getName(Integer code) {
        if(map.containsKey(code)) {
            return map.get(code);
        }
        throw new IllegalArgumentException("Invalid key: " + code);
    }

    public static Integer getLength(){
        return map.size();
    }
}
