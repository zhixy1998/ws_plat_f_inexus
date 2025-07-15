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

    // 分析名称
    @TableField("analysis_name")
    private String analysisName;

    // 测量任务id
    @TableField("task_id")
    private String taskId;

    // 样品id
    @TableField("sample_key")
    private String sampleKey;

    // 分析参数
    @TableField("analysis_param")
    private String analysisParam;

    // 分析结果
    @TableField("analysis_result")
    private String analysisResult;

    // 变换类型 (0.四则运算 1.高级运算 2.平滑 3.微分 4.光度模式转换)
    @TableField("analysis_type")
    private String analysisType;

    // 标记点
    @TableField("gauge_point")
    private String gaugePoint;

    // 峰
    @TableField("peak")
    private String peak;

    // 谷
    @TableField("grain")
    private String grain;

    // 颜色
    @TableField("color")
    private String color;

    // 分析数据图像
    @TableField("analysis_data_image")
    private String analysisDataImage;

    // 是否删除 (0:未删除 1:已删除)
    @TableField("is_delete")
    private String isDelete;
}

