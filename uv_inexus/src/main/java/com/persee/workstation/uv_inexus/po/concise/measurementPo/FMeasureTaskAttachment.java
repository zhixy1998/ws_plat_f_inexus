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
 * 附属表实体类
 */
@Data
@TableName("f_t_measure_task_attachment")
public class FMeasureTaskAttachment {

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
