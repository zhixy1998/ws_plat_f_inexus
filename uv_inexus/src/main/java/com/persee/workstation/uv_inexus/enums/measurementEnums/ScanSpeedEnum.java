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
public enum ScanSpeedEnum {

//    ["慢速", "中速", "快速", "极快"]
    SLOW("0", "慢速"),
    MEDIUM("1", "中速"),
    FAST("2", "快速"),
    SUPER_FAST("3", "极快");


    private final String code;

    private final String name;


    ScanSpeedEnum(String code, String name){
        this.code = code;
        this.name = name;
    }

    public static String getCodeByName(String name) {
        for (ScanSpeedEnum c : ScanSpeedEnum.values()) {
            if (c.getName().equals(name)) {
                return c.code;
            }
        }
        return null;
    }
}
