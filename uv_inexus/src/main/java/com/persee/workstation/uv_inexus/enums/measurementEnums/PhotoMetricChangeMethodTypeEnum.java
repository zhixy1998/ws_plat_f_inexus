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
public enum PhotoMetricChangeMethodTypeEnum {

    newMethod("newMethod", "将为您生成新方法...."),
    noData("noData", "当前没有文件数据，将应用该方法为您生成新文件数据。"),
    measure("measure", "检测到您修改了测量参数，将为您生成新方法新文件数据，是否保存当前未保存的数据："),
    analyze("analyze", "检测到您修改了分析参数，将为您生成新方法，请选择数据处理方式："),
    noChange("noChange", "方法参数没有变化，无需重新应用！");

    private final String identity;

    private final String message;


    PhotoMetricChangeMethodTypeEnum(String identity, String message){
        this.identity = identity;
        this.message = message;
    }

    public static String getMessage(String identity) {
        for (PhotoMetricChangeMethodTypeEnum c : PhotoMetricChangeMethodTypeEnum.values()) {
            if (c.getIdentity().equals(identity)) {
                return c.message;
            }
        }
        return null;
    }
}
