package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;

@Getter
public enum PhotoMetricChangeMethodTypeEnum {

    newMethod("newMethod", "将为您生成新方法...."),
    measure("measure", "您已修改测量参数，将生成新方法并清理测量和分析数据，是否要保存测量和分析数据？"),
    analyze("analyze", "您已修改分析参数，将生成新方法并清理分析数据，是否保存分析数据？"),
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
