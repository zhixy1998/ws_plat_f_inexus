package com.persee.workstation.uv_inexus.po.factory_inspection;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Data
@TableName("task_execution_detail")
public class TaskExecutionDetail {
    /**
     * 详情ID，主键
     */
    @TableId(value = "id")
    private String id;
    /**
     * 任务ID，外键
     */
    @TableField(value = "task_id")
    private String taskId;
    /**
     * 检测项ID，外键
     */
    @TableField(value = "item_id")
    private String itemId;
    /**
     * 标准物id
     */
    @TableField(value = "standard_id")
    private String standardId;
    /**
     * 标准物类型
     */
    @TableField(value = "standard_type")
    private String standardType;
}
