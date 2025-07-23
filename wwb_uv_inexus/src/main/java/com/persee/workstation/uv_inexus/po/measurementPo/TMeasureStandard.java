package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 测量标准表(TMeasureStandard)表实体类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Data
@TableName("t_measure_standard")
public class TMeasureStandard{
    // 标准id
    @TableId(value = "id", type = IdType.NONE) // 主键字段
    private String id;

    // 标准名称
    @TableField("standard_name")
    private String standardName;

    // 标准类型（内置/自定义）
    @TableField("standard_type")
    private String standardType;

    // 行业
    @TableField("industry")
    private String industry;

    // 测试类型（如：紫外-定量）
    @TableField("test_type")
    private String testType;

    // 标准号
    @TableField("standard_code")
    private String standardCode;

    // 检测方法全称
    @TableField("detection_method_name")
    private String detectionMethodName;

    // 检测项目
    @TableField("detection_item")
    private String detectionItem;

    // 测量参数信息
    @TableField("measurement_param_info")
    private String measurementParamInfo;

    // 预留字段1 ~ 预留字段5
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


}

