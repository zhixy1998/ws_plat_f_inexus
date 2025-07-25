package com.persee.workstation.uv_inexus.po.parseReport;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;

import lombok.Data;
import lombok.EqualsAndHashCode;

// import java.time.LocalDateTime;

/**
 * 计算公式表实体类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_report_calculation_formulas") // 指定对应的数据库表名
public class CalculationFormula extends BasePO {

    @TableField("detection_id") // 映射到 detection_id 列
    private String detectionId;

    @TableField("result_identity") // 映射到 result_identity 列
    private String resultIdentity;

    @TableField("result_name") // 映射到 result_name 列
    private String resultName;

    @TableField("unit") // 映射到 unit 列
    private String unit;

    @TableField("input_param1_name") // 映射到 input_param1_name 列
    private String inputParam1Name;

    @TableField("input_param1_identity") // 映射到 input_param1_identity 列
    private String inputParam1Identity;

    @TableField("input_param2_name") // 映射到 input_param2_name 列
    private String inputParam2Name;

    @TableField("input_param2_identity") // 映射到 input_param2_identity 列
    private String inputParam2Identity;

    @TableField("input_param3_name") // 映射到 input_param3_name 列
    private String inputParam3Name;

    @TableField("input_param3_identity") // 映射到 input_param3_identity 列
    private String inputParam3Identity;

    @TableField("input_param4_name") // 映射到 input_param4_name 列
    private String inputParam4Name;

    @TableField("input_param4_identity") // 映射到 input_param4_identity 列
    private String inputParam4Identity;

    @TableField("input_param5_name") // 映射到 input_param5_name 列
    private String inputParam5Name;

    @TableField("input_param5_identity") // 映射到 input_param5_identity 列
    private String inputParam5Identity;

    @TableField("input_param6_name") // 映射到 input_param6_name 列
    private String inputParam6Name;

    @TableField("input_param6_identity") // 映射到 input_param6_identity 列
    private String inputParam6Identity;

    @TableField("input_param7_name") // 映射到 input_param7_name 列
    private String inputParam7Name;

    @TableField("input_param7_identity") // 映射到 input_param7_identity 列
    private String inputParam7Identity;

    @TableField("input_param8_name") // 映射到 input_param8_name 列
    private String inputParam8Name;

    @TableField("input_param8_identity") // 映射到 input_param8_identity 列
    private String inputParam8Identity;

    @TableField("input_param9_name") // 映射到 input_param9_name 列
    private String inputParam9Name;

    @TableField("input_param9_identity") // 映射到 input_param9_identity 列
    private String inputParam9Identity;

    @TableField("input_param10_name") // 映射到 input_param10_name 列
    private String inputParam10Name;

    @TableField("input_param10_identity") // 映射到 input_param10_identity 列
    private String inputParam10Identity;

    @TableField("formula_text") // 映射到 formula_text 列
    private String formulaText; // 使用 String 存储 TEXT 类型

    @TableField("formula_image") // 映射到 formula_image 列
    private String formulaImage; // 使用 byte[] 存储 BLOB 类型

    @TableField("detection_limit") // 映射到 detection_limit 列
    private String detectionLimit;

}
