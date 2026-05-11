/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.guide.parseReport;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 计算公式表实体类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_report_calculation_formulas")
public class CalculationFormula extends BasePO {

    @TableField("detection_id")
    private String detectionId;

    @TableField("result_identity")
    private String resultIdentity;

    @TableField("result_name")
    private String resultName;

    @TableField("unit")
    private String unit;

    // ====================== 参数1 ======================
    @TableField("input_param1_name")
    private String inputParam1Name;

    @TableField("input_param1_identity")
    private String inputParam1Identity;

    @TableField("input_param1_unit")
    private String inputParam1Unit;

    @TableField("is_sample_attribute1")
    private String isSampleAttribute1;

    @TableField("curve_identifier1")
    private String curveIdentifier1;

    // ====================== 参数2 ======================
    @TableField("input_param2_name")
    private String inputParam2Name;

    @TableField("input_param2_identity")
    private String inputParam2Identity;

    @TableField("input_param2_unit")
    private String inputParam2Unit;

    @TableField("is_sample_attribute2")
    private String isSampleAttribute2;

    @TableField("curve_identifier2")
    private String curveIdentifier2;

    // ====================== 参数3 ======================
    @TableField("input_param3_name")
    private String inputParam3Name;

    @TableField("input_param3_identity")
    private String inputParam3Identity;

    @TableField("input_param3_unit")
    private String inputParam3Unit;

    @TableField("is_sample_attribute3")
    private String isSampleAttribute3;

    @TableField("curve_identifier3")
    private String curveIdentifier3;

    // ====================== 参数4 ======================
    @TableField("input_param4_name")
    private String inputParam4Name;

    @TableField("input_param4_identity")
    private String inputParam4Identity;

    @TableField("input_param4_unit")
    private String inputParam4Unit;

    @TableField("is_sample_attribute4")
    private String isSampleAttribute4;

    @TableField("curve_identifier4")
    private String curveIdentifier4;

    // ====================== 参数5 ======================
    @TableField("input_param5_name")
    private String inputParam5Name;

    @TableField("input_param5_identity")
    private String inputParam5Identity;

    @TableField("input_param5_unit")
    private String inputParam5Unit;

    @TableField("is_sample_attribute5")
    private String isSampleAttribute5;

    @TableField("curve_identifier5")
    private String curveIdentifier5;

    // ====================== 参数6 ======================
    @TableField("input_param6_name")
    private String inputParam6Name;

    @TableField("input_param6_identity")
    private String inputParam6Identity;

    @TableField("input_param6_unit")
    private String inputParam6Unit;

    @TableField("is_sample_attribute6")
    private String isSampleAttribute6;

    @TableField("curve_identifier6")
    private String curveIdentifier6;

    // ====================== 参数7 ======================
    @TableField("input_param7_name")
    private String inputParam7Name;

    @TableField("input_param7_identity")
    private String inputParam7Identity;

    @TableField("input_param7_unit")
    private String inputParam7Unit;

    @TableField("is_sample_attribute7")
    private String isSampleAttribute7;

    @TableField("curve_identifier7")
    private String curveIdentifier7;

    // ====================== 参数8 ======================
    @TableField("input_param8_name")
    private String inputParam8Name;

    @TableField("input_param8_identity")
    private String inputParam8Identity;

    @TableField("input_param8_unit")
    private String inputParam8Unit;

    @TableField("is_sample_attribute8")
    private String isSampleAttribute8;

    @TableField("curve_identifier8")
    private String curveIdentifier8;

    // ====================== 参数9 ======================
    @TableField("input_param9_name")
    private String inputParam9Name;

    @TableField("input_param9_identity")
    private String inputParam9Identity;

    @TableField("input_param9_unit")
    private String inputParam9Unit;

    @TableField("is_sample_attribute9")
    private String isSampleAttribute9;

    @TableField("curve_identifier9")
    private String curveIdentifier9;

    // ====================== 参数10 ======================
    @TableField("input_param10_name")
    private String inputParam10Name;

    @TableField("input_param10_identity")
    private String inputParam10Identity;

    @TableField("input_param10_unit")
    private String inputParam10Unit;

    @TableField("is_sample_attribute10")
    private String isSampleAttribute10;

    @TableField("curve_identifier10")
    private String curveIdentifier10;

    @TableField("formula_text")
    private String formulaText;

    @TableField("formula_image")
    private String formulaImage;

    @TableField("detection_limit")
    private String detectionLimit;

}