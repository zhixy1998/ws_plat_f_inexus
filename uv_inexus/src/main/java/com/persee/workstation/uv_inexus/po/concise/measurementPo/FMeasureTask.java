package com.persee.workstation.uv_inexus.po.concise.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测量任务表(TMeasureTask)表实体类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Data
@TableName("f_t_measure_task")
public class FMeasureTask{
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
    private LocalDateTime createTime;

    // 方法Key
    @TableField("method_key")
    private String methodKey;

    // 样品Key 由TaskSpecimen类转JSONString
    @TableField("sample_key")
    private String sampleKey;

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

    //  最后保存时间
    @TableField("end_save_time")
    private LocalDateTime endSaveTime;

    // 序列号
    @TableField("device_serial_number")
    private String deviceSerialNumber;

    // 图片数据
    @TableField(typeHandler = JacksonTypeHandler.class, value = "image_data")
    private String imageData;

    // 是否删除
    @TableField("is_delete")
    private String isDelete;

    @TableField("table_info")
    private String tableInfo;
}

