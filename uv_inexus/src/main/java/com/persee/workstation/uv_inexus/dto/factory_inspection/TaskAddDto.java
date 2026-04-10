package com.persee.workstation.uv_inexus.dto.factory_inspection;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Size;
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
public class TaskAddDto {

    /**
     * 检测任务名称，必填项
     */
    @ApiModelProperty(value = "检测任务名称，必填项")
    //任务名称长度不能大于32
    @Size(max = 32,message = "任务名称长度不能大于32")
    private String detectName;

    /**
     * 任务所使用的模板名称或编号，可为空
     */
    @ApiModelProperty(value = "任务所使用的模板名称或编号")
    private String useTemplate;
    /**
     * 任务内容的分类，可为空
     */
    @ApiModelProperty(value = "任务分类")
    private String contentCategory;
    @ApiModelProperty("创建者")
    private String createBy;

    @ApiModelProperty("修改者")
    private String updateBy;

    @ApiModelProperty("创建时间,插入数据库时自动填充")
    private LocalDateTime created;

    @ApiModelProperty("修改时间，更新操作时自动填充")
    private LocalDateTime updated;
    @ApiModelProperty("仪器序列号")
    private String serialNumber;

    @ApiModelProperty("任务类型")
    private String taskType;
}
