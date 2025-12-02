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

package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;

@Getter
public enum QualityConfEnums {

    slope_qc("1", "斜率质控"),
    intercept_qc("2", "截距质控"),
    curve_r_qc("3", "标曲R质控"),
    curve_r2_qc("4", "标曲R2质控"),
    blank_sample_qc("5", "空白样品质控");

    private final String code;

    private final String name;


    QualityConfEnums(String code, String name){
        this.code = code;
        this.name = name;
    }

    public static String getName(String code) {
        for (QualityConfEnums c : QualityConfEnums.values()) {
            if (c.getCode().equals(code)) {
                return c.name;
            }
        }
        return null;
    }
}
