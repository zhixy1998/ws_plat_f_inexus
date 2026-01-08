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
public enum EnergyLampEnums {

    Tungsten("0", "钨灯"),
    Deuterium("1", "氘灯"),
    Mercury("2", "汞灯");

    private final String identity;

    private final String name;


    EnergyLampEnums(String identity, String name){
        this.identity = identity;
        this.name = name;
    }


    public static String getIdentityByName(String name) {
        for (EnergyLampEnums c : EnergyLampEnums.values()) {
            if (c.getName().equals(name)) {
                return c.identity;
            }
        }
        return null;
    }

    public static String getName(String identity) {
        for (EnergyLampEnums c : EnergyLampEnums.values()) {
            if (c.getIdentity().equals(identity)) {
                return c.name;
            }
        }
        return null;
    }
}
