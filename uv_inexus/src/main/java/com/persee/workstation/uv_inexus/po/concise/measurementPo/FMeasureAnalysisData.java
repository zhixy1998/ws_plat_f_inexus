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
import lombok.Data;

/**
 * 分析数据表(TMeasureAnalysisData)表实体类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Data
@TableName("f_t_measure_analysis_data")
public class FMeasureAnalysisData {
    // 分析数据id
    @TableId(value = "id",type = IdType.ASSIGN_UUID)// 假设id是主键
    private String id;

    // 分析编号
    @TableField("analysis_number")
    private Integer analysisNumber;

    // 分析名称
    @TableField("analysis_name")
    private String analysisName;

    // 测量任务id
    @TableField("task_id")
    private String taskId;

    // 样品id
    @TableField("sample_key")
    private String sampleKey;

    // 测量数据id
    @TableField("measure_data_id")
    private String measureDataId;

    // 数据版本
    @TableField("data_version")
    private Integer dataVersion;

    // 分析参数
    @TableField("analysis_param")
    private String analysisParam;

    // 分析结果
    @TableField("analysis_result")
    private String analysisResult;

    // 变换类型 (0.四则运算 1.高级运算 2.平滑 3.微分 4.光度模式转换.5.原始计算数据  )
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
    private byte[] analysisDataImage;

    // 是否保存(0.保存 1.不保存)
    @TableField("is_save")
    private String isSave;

    // 是否删除 (0:未删除 1:已删除)
    @TableField("is_delete")
    private String isDelete;
}

