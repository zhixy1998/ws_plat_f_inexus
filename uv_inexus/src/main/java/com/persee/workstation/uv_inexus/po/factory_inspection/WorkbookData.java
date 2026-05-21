package com.persee.workstation.uv_inexus.po.factory_inspection;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Data
@TableName("calibration_workbook_data")
public class WorkbookData implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableField("id")
    @ApiModelProperty(value = "id")
    private String id;
    @TableField("task_id")
    @ApiModelProperty(value = "任务id")
    private String taskId;
    @TableField("template_code")
    @ApiModelProperty(value = "模板编号")
    private String templateCode;
    @TableField("update_by")
    @ApiModelProperty(value = "更新人")
    private String updateBy;
    @TableField("update_time")
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
    @TableField("create_time")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @TableField("is_active")
    @ApiModelProperty(value = "是否存活")
    private Boolean isActive;
    @TableField("last_access_time")
    @ApiModelProperty(value = "最后访问时间")
    private LocalDateTime lastAccessTime;
    @TableField("workbook_data")
    //类型是longtext
    private byte[] workbookData;  // 修正数据类型

    @TableField("yellow_highlighted_columns")
    @ApiModelProperty(value = "黄色高亮列信息(JSON格式)，存储RGB(255,192,0)的列名")
    private String yellowHighlightedColumns;
}
