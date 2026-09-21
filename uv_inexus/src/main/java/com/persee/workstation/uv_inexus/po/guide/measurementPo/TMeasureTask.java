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
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测量任务表(TMeasureTask)表实体类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Data
@TableName("t_measure_task")
public class TMeasureTask{
    // 任务id
    @TableId(value = "id",type = IdType.ASSIGN_UUID) // 主键字段
    private String id;

    // 用户id
    @TableField("user_id")
    private String userId;

    // 任务名称
    @TableField("task_name")
    private String taskName;

    // 创建时间
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    // 方法Key
    @TableField("method_key")
    private String methodKey;

    // 样品Key 由TaskSpecimen类转JSONString
    @TableField("sample_key")
    private String sampleKey;

    // 样品自动编号
    @TableField("sample_auto_num")
    private Integer sampleAutoNum;

    // 测量类型
    @TableField("measure_type")
    private String measureType;

    // 所属项目Key
    @TableField("project_item_key")
    private String projectItemKey;

    // 校零参数
    @TableField("zero_param")
    private String zeroParam;

    // 拟合参数
    @TableField("fitting_param")
    private String fittingParam;

    //  开始时间
    @TableField("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    //  结束时间
    @TableField("end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    // 序列号
    @TableField("device_serial_number")
    private String deviceSerialNumber;

    // 图片数据
    @TableField(typeHandler = JacksonTypeHandler.class, value = "image_data")
    private String imageData;

    // 是否删除(0:未删除 1:已删除)
    @TableField("is_delete")
    private String isDelete;

    // 任务状态(0:未测 1:已测 2:结束)
    @TableField("task_status")
    private String taskStatus;

    // 表头信息
    @TableField("table_info")
    private String tableInfo;

    // 分析样品选中状态(由AnalysisSampleSelect列表转JSONString,存分析数据id与是否选中)
    @TableField("analysis_sample")
    private String analysisSample;
}

