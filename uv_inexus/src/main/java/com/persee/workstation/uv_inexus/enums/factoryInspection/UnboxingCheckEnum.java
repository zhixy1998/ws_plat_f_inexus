package com.persee.workstation.uv_inexus.enums.factoryInspection;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

public enum UnboxingCheckEnum {
    //开箱检验-波长示值误差
    WAVELENGTH_ACCURACY_OPEN_BOX("波长示值误差与重复性校验","波长示值误差", "士0.2nm(氚灯656.1nm、486.0nm)"),
    //开箱检验-波长重复性
    WAVELENGTH_REPEATABILITY_OPEN_BOX("波长示值误差与重复性校验","波长重复性", "≤0.1nm"),
    //开箱检验-基线平直度
    BASELINE_STRAIGHTNESS_OPEN_BOX("基线平直度检验","基线平直度", "士0.0005Abs(1090nm~200nm)"),
    //开箱检验-漂移
    DRIFT_OPEN_BOX("漂移检验","漂移", "≤0.1%"),
    //开箱检验-噪声
    NOISE_OPEN_BOX("噪声检验","噪声","≤0.1%"),
    //开箱检验-透射比示值误差
    TRANSMITTANCE_ACCURACY_OPEN_BOX("透射比示值误差与重复性校验","透射比示值误差", "士0.3%(重铬酸钾标准溶液或标准滤光片)"),
    ;

    private final String sheetName;
    private final String acceptanceItem;
    private final String acceptanceCriteriaValue;

    UnboxingCheckEnum(String sheetName, String acceptanceItem, String acceptanceCriteriaValue) {
        this.sheetName = sheetName;
        this.acceptanceItem = acceptanceItem;
        this.acceptanceCriteriaValue = acceptanceCriteriaValue;
    }

    public String getSheetName() {
        return sheetName;
    }

    public String getAcceptanceItem() {
        return acceptanceItem;
    }

    public String getAcceptanceCriteriaValue() {
        return acceptanceCriteriaValue;
    }
    public static UnboxingCheckEnum fromSheetName(String sheetName) {
        if (sheetName == null || sheetName.trim().isEmpty()) {
            return null;
        }
        for (UnboxingCheckEnum item : values()) {
            if (item.getSheetName().equals(sheetName)) {
                return item;
            }
        }
        return null;
    }
}
