package com.persee.workstation.uv_inexus.po.factory_inspection;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 任务管理表(task_management)实体类
 *
 * @author 
 * @since 2025-12-09 18:55:53
 * @description 
 */
@Data
@TableName("task_management")
public class TaskManagement extends BasePO {
    /**
     * 任务唯一标识，主键
     */
    @TableId(value = "id")
    @ApiModelProperty(value = "任务唯一标识，主键")
    private String id;
    /**
     * 检测任务名称，必填项
     */
    @TableField("detect_name")
    @ApiModelProperty(value = "检测任务名称，必填项")
    private String detectName;
    /**
     * 执行此任务所使用的操作仪器，可为空
     */
    @TableField("operation_instrument")
    @ApiModelProperty(value = "执行此任务所使用的操作仪器，可为空")
    private String operationInstrument;
    /**
     * 任务所使用的模板名称或编号，可为空
     */
    @TableField("use_template")
    @ApiModelProperty(value = "任务所使用的模板名称或编号，可为空")
    private String useTemplate;
    /**
     * 任务内容的分类，可为空
     */
    @TableField("content_category")
    @ApiModelProperty(value = "任务内容的分类，可为空")
    private String contentCategory;
    /**
     * 任务的计划或实际开始时间，可为空
     */
    @TableField("start_time")
    @ApiModelProperty(value = "任务的计划或实际开始时间，可为空")
    private Date startTime;
    /**
     * 任务的完成状态枚举：未开始、未完成、已完成
     */
    @TableField("completion_status")
    @ApiModelProperty(value = "任务的完成状态枚举：未开始、未完成、已完成")
    private String completionStatus;
    /**
     * 任务的实际完成时间，可为空
     */
    @TableField("completion_time")
    @ApiModelProperty(value = "任务的实际完成时间，可为空")
    private Date completionTime;
    /**
     * 结果模板
     */
    @TableField("result_template")
    @ApiModelProperty(value = "结果模板")
    private String resultTemplate;
    @TableField(exist = false)
    @ApiModelProperty(value = "templateCode")
    private String templateCode;
    @TableField(exist = false)
    @ApiModelProperty(value = "模板名称")
    private String templateName;
}