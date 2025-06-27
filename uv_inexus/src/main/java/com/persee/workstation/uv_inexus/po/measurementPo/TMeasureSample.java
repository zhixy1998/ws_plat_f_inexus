package com.persee.workstation.uv_inexus.po.measurementPo;

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
@TableName("t_measure_sample")
public class TMeasureSample{
    // 样品id
    @TableId(value = "id",type = IdType.ASSIGN_UUID) // 主键字段
    private String id;

    // 样品编号
    @TableField("sample_number")
    private String sampleNumber;

    // 样品类型
    @TableField("sample_type")
    private String sampleType;

    // 样品名称
    @TableField("sample_name")
    private String sampleName;

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

