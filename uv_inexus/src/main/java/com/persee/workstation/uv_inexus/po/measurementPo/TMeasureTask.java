package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
    @TableId(value = "id", type = IdType.NONE) // 主键字段
    private String id;

    // 用户id
    @TableField("user_id")
    private String userId;

    // 任务名称
    @TableField("task_name")
    private String taskName;

    // 创建时间
    @TableField("create_time")
    private LocalDateTime createTime;

    // 采集起始时间
    @TableField("collection_start_time")
    private LocalDateTime collectionStartTime;

    // 方法Key
    @TableField("method_key")
    private String methodKey;

    // 所属项目Key
    @TableField("project_item_key")
    private String projectItemKey;

    //  开始时间
    @TableField("start_time")
    private LocalDateTime startTime;

    //  结束时间
    @TableField("end_time")
    private String endTime;

    // 序列号
    @TableField("device_serial_number")
    private String deviceSerialNumber;

    // 预留字段4 ~ 预留字段5
    @TableField("field4")
    private String field4;

    @TableField("field5")
    private String field5;
}

