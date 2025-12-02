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
public enum RationChangeMethodTypeEnums {

    newMethod("newMethod", "将为您生成新方法...."),
    noData("noData", "当前没有文件数据，将应用该方法为您生成新文件数据。"),
    measure("measure", "您已修改测量参数，将生成新方法并清理测量和分析数据，是否要保存现有数据？"),
    curve("curve", "您已修改曲线相关参数，将生成新方法并清理分析数据，是否要保存现有数据？"),
    analyze("analyze", "您已修改分析参数，将生成新方法并清理分析数据，是否保存现有数据？"),
    noChange("noChange", "方法参数没有变化，无需重新应用！");

    private final String identity;

    private final String message;


    RationChangeMethodTypeEnums(String identity, String message){
        this.identity = identity;
        this.message = message;
    }

    public static String getMessage(String identity) {
        for (RationChangeMethodTypeEnums c : RationChangeMethodTypeEnums.values()) {
            if (c.getIdentity().equals(identity)) {
                return c.message;
            }
        }
        return null;
    }

}
