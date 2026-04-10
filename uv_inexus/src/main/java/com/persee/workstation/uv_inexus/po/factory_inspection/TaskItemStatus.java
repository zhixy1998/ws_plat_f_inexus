package com.persee.workstation.uv_inexus.po.factory_inspection;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */@Data
@TableName("task_item_status")
public class TaskItemStatus {
    @TableId(value = "id")
    private String id;

    @TableField(value = "task_id")
    @ApiModelProperty(value = "任务ID")
    private String taskId;

    @TableField(value = "item_id")
    @ApiModelProperty(value = "检测项ID")
    private String itemId;

    @TableField(value = "measured")
    @ApiModelProperty(value = "检测状态：未检测0，检测成功1，检测失败2")
    private String measured;

    @TableField(value = "check_result")
    @ApiModelProperty(value = "检测结果：未检测0，合格1，不合格2")
    private String checkResult;
}