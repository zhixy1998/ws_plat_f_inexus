/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.enums.instrumentEnums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum DeviceSamplePoolPurpose {
    ZERO(1,"校零"),
    BASE_LINE(2, "基线"),
    MEASURE(3,"测量");

    private final Integer code;

    private final String name;

    private static final Map<Integer, String> map = new HashMap<>();

    static {
        for(DeviceSamplePoolPurpose e : DeviceSamplePoolPurpose.values()) {
            map.put(e.code,e.name);
        }
    }

    private DeviceSamplePoolPurpose(Integer code, String name){
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
