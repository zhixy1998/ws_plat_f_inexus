package com.persee.workstation.uv_inexus.po.guide.measurementPo.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 项目协作组实体类
 * 对应数据库表：t_measure_project_collaborator_group
 */
@Data
@TableName("t_measure_project_collaborator_group")
public class TMeasureProjectCollaboratorGroup {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（UUID）
     */
    @TableId(value = "id",type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 项目ID（对应 project_id）
     */
    @TableField("project_id")
    private String projectId;

    /**
     * 协作组ID（对应 group_id）
     */
    @TableField("group_id")
    private String groupId;

    public TMeasureProjectCollaboratorGroup() {
        // 默认构造函数
    }

    public TMeasureProjectCollaboratorGroup(String projectId, String groupId) {
        this.projectId = projectId;
        this.groupId = groupId;
    }
}