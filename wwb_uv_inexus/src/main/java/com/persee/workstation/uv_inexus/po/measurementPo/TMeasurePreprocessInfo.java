package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 前处理信息表(TMeasurePreprocessInfo)表实体类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Data
@TableName("t_measure_preprocess_info")
public class TMeasurePreprocessInfo{

    // 前处理信息id
    @TableId(value = "id", type = IdType.NONE) // 主键字段
    private String id;

    // 测量标准uuid
    @TableField("standard_id")
    private String standardId;

    // 前处理描述
    @TableField("preprocess_description")
    private String preprocessDescription;

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

