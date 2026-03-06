/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.systemUserPermissionPo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 快捷配置PO类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("快捷配置表")
@TableName("t_sys_quick_config")
public class SysQuickConfigPO extends BasePO {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("用户ID")
    private String id;

    @TableField("name")
    @ApiModelProperty("快捷配置名称")
    private String name;

    @TableField("perm_ids")
    @ApiModelProperty("绑定权限ids")
    private String permIds;

    @TableField("perm_ids_have")
    @ApiModelProperty("绑定权限ids拥有的回显")
    private String permIdsHave;

    @TableField("parent_id")
    @ApiModelProperty("父级ID（0为一级配置）")
    private Long parentId;

    @TableField("quick_config_type")
    @ApiModelProperty("状态：0-普通用户，0-GMP")
    private Integer quickConfigType;
}