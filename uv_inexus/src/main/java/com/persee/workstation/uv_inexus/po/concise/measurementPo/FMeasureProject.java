package com.persee.workstation.uv_inexus.po.concise.measurementPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目表(TMeasureProject)表实体类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Data
@TableName("f_t_measure_project")
public class FMeasureProject{
    // 项目id
    @TableId(value = "id",type = IdType.ASSIGN_UUID) // 主键字段
    private String id;

    // 项目名称
    @TableField("project_name")
    private String projectName;

    // 创建时间
    @TableField("create_time")
    private LocalDateTime createTime;

    // 创建人Key
    @TableField("creator_key")
    private String creatorKey;

    // 备注
    @TableField("remark")
    private String remark;

    // 最近更新人
    @TableField("Last_updated_by")
    private String lastUpdatedBy;

    // 最近更新时间
    @TableField("Last_updated_time")
    private LocalDateTime lastUpdatedTime;

    // 预留字段3 ~ 预留字段5
    @TableField("field3")
    private String field3;

    @TableField("field4")
    private String field4;

    @TableField("field5")
    private String field5;

}

