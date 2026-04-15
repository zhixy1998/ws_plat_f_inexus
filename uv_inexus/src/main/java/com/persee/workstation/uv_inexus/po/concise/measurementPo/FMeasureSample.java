/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.concise.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.annotations.ApiModelProperty;
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
    @ApiModelProperty("主键id")
    private String id;

    // 样品编号
    @TableField("sample_number")
    @ApiModelProperty("样品编号")
    private String sampleNumber;

    // 样品录入id
    @TableField("sample_entering_id")
    @ApiModelProperty("样品录入id")
    private String sampleEnteringId;

    // 样品类型
    @TableField("sample_type")
    @ApiModelProperty("样品类型")
    private String sampleType;

    // 样品种类
    @TableField("sample_kind")
    @ApiModelProperty("样品种类")
    private String sampleKind;

    // 样品名称
    @TableField("sample_name")
    @ApiModelProperty("样品名称")
    private String sampleName;

    //平行样组Id
    @TableField("parallel_group_id")
    @ApiModelProperty("平行样组Id")
    private String parallelGroupId;

    // 稀释倍数
    @TableField("dilution_ratio")
    @ApiModelProperty("稀释倍数")
    private String dilutionRatio;

    // 取样量
    @TableField("sample_volume")
    @ApiModelProperty("取样量")
    private String sampleVolume;

    // 定容体积
    @TableField("constant_volume")
    @ApiModelProperty("定容体积")
    private String constantVolume;

    // 标准浓度
    @TableField("normal_concentration")
    @ApiModelProperty("标准浓度")
    private String normalConcentration;

    // 样品位置
    @TableField("sample_location")
    @ApiModelProperty("样品位置")
    private String sampleLocation;

    // 样品描述
    @TableField("sample_description")
    @ApiModelProperty("样品描述")
    private String sampleDescription;

    // 输入数据
    @TableField("input_data")
    @ApiModelProperty("输入数据")
    private String inputData;

    // 颜色
    @TableField("color")
    @ApiModelProperty("颜色")
    private String color;

    @TableField("heads")
    @ApiModelProperty("样品新增表头")
    private String heads;

    @TableField("new_col_data")
    @ApiModelProperty("新增表头数据")
    private String newColData;

    // 数据量
    @TableField("data_bulk")
    @ApiModelProperty("数据量")
    private Integer dataBulk;
}

