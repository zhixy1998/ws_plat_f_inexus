/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;

@Getter
public enum WaveLengthEnum {

    abs("1", "Abs"),
    tPercent("2", "T%"),
    es("3", "Es"),
    er("4", "Er");

    private final String code;

    private final String name;


    WaveLengthEnum(String code, String name){
        this.code = code;
        this.name = name;
    }


    public static WaveLengthEnum getByName(String name) {
        for (WaveLengthEnum c : WaveLengthEnum.values()) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    public static String getCode(String name) {
        for (WaveLengthEnum c : WaveLengthEnum.values()) {
            if (c.getName().equals(name)) {
                return c.code;
            }
        }
        return null;
    }
    public static String getName(String code) {
        for (WaveLengthEnum c : WaveLengthEnum.values()) {
            if (c.getCode().equals(code)) {
                return c.name;
            }
        }
        return null;
    }
}
