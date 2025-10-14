/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.po.guide.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 附属表实体类
 */
@Data
@TableName("t_measure_task_attachment")
public class TMeasureTaskAttachment {

    /**
     * 附属id
     */
    @TableId(value = "id",type = IdType.ASSIGN_UUID) // 主键字段
    private String id;

    /**
     * 任务id
     */
    @TableField("task_id")
    private String taskId;

    /**
     * 平行样
     */
    @TableField("parallel_sample")
    private String parallelSample;

    /**
     * 数据参数，任务所需要的参数，例如小数位数，sd,red，以及是否显示删除数据和是否同时显示abs和t%
     */
    @TableField("data_param")
    private String dataParam;

    /**
     * 重叠图像
     */
    @TableField("overlap_image")
    private byte[] overlapImage;

    /**
     * 平铺图像
     */
    @TableField("tiled_image")
    private byte[] tiledImage;

    /**
     * 3D图像
     */
    @TableField("addr_image")
    private byte[] addrImage;
}
