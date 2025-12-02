/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.guide.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测量方法表(TMeasureMethod)表实体类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Data
@TableName("t_measure_method")
public class TMeasureMethod {
    // 方法id
    @TableId(value = "id",type = IdType.ASSIGN_UUID) // 主键字段
    private String id;

    // 前处理Key
    @TableField("preprocess_key")
    private String preprocessKey;

    // 多个计算公式Key
    @TableField("multiple_calculation_formula_key")
    private String multipleCalculationFormulaKey;

    // 所属项目Key(当选用的是公共方法则项目key为空)
    @TableField("project_item_key")
    private String projectItemKey;

    // 方法名称
    @TableField("method_name")
    private String methodName;

    // 测量类型
    @TableField("measurement_type")
    private String measurementType;

    // 测量参数信息
    @TableField("measurement_param_info")
    private String measurementParamInfo;

    // 是否共享方法(0.否 1.是)
    @TableField("is_sharing_method")
    private String isSharingMethod;

    //  方法创建时间
    @TableField("method_create_time")
    private LocalDateTime methodCreateTime;

    // 方法创建人
    @TableField("method_creator")
    private String methodCreator;

    // 预留字段2 ~ 预留字段5
    @TableField("field2")
    private String field2;

    @TableField("field3")
    private String field3;

    @TableField("field4")
    private String field4;

    @TableField("field5")
    private String field5;
}

