package com.persee.workstation.uv_inexus.enums.factoryInspection;

import lombok.Getter;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Getter
public enum AalysisMethodEnum {
    //寻峰
    FIND_PEAK("findPeak", "寻峰"),
    //求半峰宽
    FIND_HALF_PEAK_WIDTH("findHalfPeakWidth", "求半峰宽"),
    //求最大偏移量
    FIND_MAX_OFFSET("findMaxOffset", "求最大偏移量"),
    //求平均
//    FIND_AVERAGE("findAverage", "求平均"),
    //求漂移
    FIND_DRIFT("findDrift", "求漂移"),
    //多次测量
//    MULTIPLE_MEASUREMENT("multipleMeasurement", "多次测量"),
    //最大最小值差
    FIND_MAX_MIN_DIFFERENCE("findMaxMinDifference", "最大最小值差");
    private final String code;
    private final String msg;

    AalysisMethodEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static AalysisMethodEnum getByMsg(String msg) {
        for (AalysisMethodEnum method : values()) {
            if (method.getMsg().equals(msg)) {
                return method;
            }
        }
        return null;
    }

    public static AalysisMethodEnum getByCode(String code) {
        for (AalysisMethodEnum method : values()) {
            if (method.getCode().equals(code)) {
                return method;
            }
        }
        return null;
    }
}
