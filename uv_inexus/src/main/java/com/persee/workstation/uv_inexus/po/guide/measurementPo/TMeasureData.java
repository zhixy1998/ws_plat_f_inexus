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
    @TableId(value = "id",type = IdType.ASSIGN_UUID) // 主键字段
    private String id;

    // 测量任务id
    @TableField("task_id")
    private String taskId;

    // 序列号(当多个测量数据的时候按照序列号进行部分排序)
    @TableField("serial_number")
    private Integer serialNumber;

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

    // 标记点
    @TableField("gauge_point")
    private String gaugePoint;

    // 峰
    @TableField("peak")
    private String peak;

    // 谷
    @TableField("grain")
    private String grain;

    // 采样时间
    @TableField("sampling_time")
    private String samplingTime;

    // 数据图像
    @TableField("data_image")
    private byte[] dataImage;

    // 备注
    @TableField("remark")
    private String remark;

    // 是否删除
    @TableField("is_delete")
    private String isDelete;
}



