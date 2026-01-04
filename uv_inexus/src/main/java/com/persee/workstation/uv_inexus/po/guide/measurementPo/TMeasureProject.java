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
 * 项目表(TMeasureProject)表实体类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Data
@TableName("t_measure_project")
public class TMeasureProject{
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

    // 主责组
    @TableField("owner_group_id")
    private String ownerGroupId;

    // 是否删除(0:未删除 1:已删除)
    @TableField("is_delete")
    private String isDelete;

    // 预留字段5
    @TableField("field5")
    private String field5;

}

