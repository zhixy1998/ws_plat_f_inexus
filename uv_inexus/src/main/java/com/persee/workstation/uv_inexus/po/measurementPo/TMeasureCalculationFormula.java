package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 计算公式表(TMeasureCalculationFormula)表实体类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Data
@TableName("t_measure_calculation_formula")
public class TMeasureCalculationFormula {
    // 计算公式id
    @TableId(value = "id", type = IdType.NONE) // 主键字段
    private String id;

    // 测量标准uuid
    @TableField("standard_id")
    private String standardId;

    // 结果名称
    @TableField("result_name")
    private String resultName;

    // 输入参数1 ~ 输入参数10
    @TableField("input_param1")
    private String inputParam1;

    @TableField("input_param2")
    private String inputParam2;

    @TableField("input_param3")
    private String inputParam3;

    @TableField("input_param4")
    private String inputParam4;

    @TableField("input_param5")
    private String inputParam5;

    @TableField("input_param6")
    private String inputParam6;

    @TableField("input_param7")
    private String inputParam7;

    @TableField("input_param8")
    private String inputParam8;

    @TableField("input_param9")
    private String inputParam9;

    @TableField("input_param10")
    private String inputParam10;

    // 计算公式
    @TableField("calculation_formula")
    private String calculationFormula;

    // 预留字段1 ~ 预留字段10
    @TableField("field1")
    private String field1;

    @TableField("field2")
    private String field2;

    @TableField("field3")
    private String field3;

    @TableField("field4")
    private String field4;

    @TableField("field5")
    private String field5;

    @TableField("field6")
    private String field6;

    @TableField("field7")
    private String field7;

    @TableField("field8")
    private String field8;

    @TableField("field9")
    private String field9;

    @TableField("field10")
    private String field10;
}

