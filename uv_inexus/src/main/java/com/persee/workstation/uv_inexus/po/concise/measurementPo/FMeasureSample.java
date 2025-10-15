/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.po.concise.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 样品信息表(TMeasureSample)表实体类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Data
@TableName("f_t_measure_sample")
public class FMeasureSample{
    // 样品id
    @TableId(value = "id",type = IdType.ASSIGN_UUID) // 主键字段
    private String id;

    // 样品编号
    @TableField("sample_number")
    private String sampleNumber;

    // 样品类型
    @TableField("sample_type")
    private String sampleType;

    // 样品种类
    @TableField("sample_kind")
    private String sampleKind;

    // 样品名称
    @TableField("sample_name")
    private String sampleName;

    //平行样组Id
    @TableField("parallel_group_id")
    private String parallelGroupId;

    // 稀释倍数
    @TableField("dilution_ratio")
    private String dilutionRatio;

    // 取样量
    @TableField("sample_volume")
    private String sampleVolume;

    // 定容体积
    @TableField("constant_volume")
    private String constantVolume;

    // 标准浓度
    @TableField("normal_concentration")
    private String normalConcentration;

    // 样品位置
    @TableField("sample_location")
    private String sampleLocation;

    // 样品描述
    @TableField("sample_description")
    private String sampleDescription;

    // 输入数据
    @TableField("input_data")
    private String inputData;

    // 颜色
    @TableField("color")
    private String color;

    @TableField("heads")
    private String heads;

    @TableField("new_col_data")
    private String newColData;

    // 数据量
    @TableField("data_bulk")
    private Integer dataBulk;
}

