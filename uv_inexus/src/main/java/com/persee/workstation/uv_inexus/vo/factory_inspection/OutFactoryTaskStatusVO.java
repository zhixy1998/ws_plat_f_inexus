package com.persee.workstation.uv_inexus.vo.factory_inspection;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 出厂检定任务状态信息
 */
@Data
@ApiModel("出厂检定任务状态信息")
public class OutFactoryTaskStatusVO {

    @ApiModelProperty("任务 ID")
    private String taskId;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("仪器序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("模板编码")
    private String templateCode;

    @ApiModelProperty("连接状态：0-未连接，1-已连接")
    private Integer connectStatus;



    @ApiModelProperty("当前初始化项名称")
    private String currentInitItem;


    @ApiModelProperty("测量状态：0-未开始，1-测量中，2-已完成，3-已停止")
    private Integer measureStatus;

    @ApiModelProperty("测量进度百分比 (0-100)")
    private Integer measureProgress;

    @ApiModelProperty("当前样品编号")
    private String currentSampleId;

    @ApiModelProperty("总样品数")
    private Integer totalSamples;

    @ApiModelProperty("已完成样品数")
    private Integer completedSamples;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;


}
