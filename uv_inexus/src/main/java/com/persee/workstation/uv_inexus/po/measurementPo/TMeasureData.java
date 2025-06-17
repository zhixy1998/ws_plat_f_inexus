package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测量数据表(TMeasureData)表实体类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Data
@TableName("t_measure_data")
public class TMeasureData {
    // 测量数据id
    @TableId(value = "id", type = IdType.NONE) // 主键字段
    private String id;

    // 测量任务id
    @TableField("task_id")
    private String taskId;

    // 样品Key
    @TableField("sample_key")
    private String sampleKey;

    // 仪器数据
    @TableField("instrument_data")
    private String instrumentData;

    // 原始计算数据
    @TableField("original_calculation_data")
    private String originalCalculationData;

    // 测量开始时间
    @TableField("measurement_start_time")
    private LocalDateTime measurementStartTime;

    // 测量结束时间
    @TableField("measurement_end_time")
    private LocalDateTime measurementEndTime;

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

