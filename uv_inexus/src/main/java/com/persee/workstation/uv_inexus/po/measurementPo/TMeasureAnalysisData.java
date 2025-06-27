package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 分析数据表(TMeasureAnalysisData)表实体类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Data
@TableName("t_measure_analysis_data")
public class TMeasureAnalysisData {
    // 分析数据id
    @TableId(value = "id",type = IdType.ASSIGN_UUID)// 假设id是主键
    private String id;

    // 测量任务id
    @TableField("task_id")
    private String taskId;

    // 分析参数
    @TableField("analysis_param")
    private String analysisParam;

    // 分析结果
    @TableField("analysis_result")
    private String analysisResult;

    // 预留字段1
    @TableField("field1")
    private String field1;

    // 预留字段2
    @TableField("field2")
    private String field2;

    // 预留字段3
    @TableField("field3")
    private String field3;

    // 预留字段4
    @TableField("field4")
    private String field4;

    // 预留字段5
    @TableField("field5")
    private String field5;

    // 预留字段6
    @TableField("field6")
    private String field6;

    // 预留字段7
    @TableField("field7")
    private String field7;

    // 预留字段8
    @TableField("field8")
    private String field8;

    // 预留字段9
    @TableField("field9")
    private String field9;

    // 预留字段10
    @TableField("field10")
    private String field10;
}

