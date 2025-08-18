package com.persee.workstation.uv_inexus.po.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 附属表实体类
 */
@Data
@TableName("t_measure_task_attachment")
public class TMeasureTaskAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 表格信息，保护映射信息和样品表头信息
     */
    @TableField("table_info")
    private String tableInfo;

    /**
     * 平行样
     */
    @TableField("parallel_sample")
    private String parallelSample;

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
