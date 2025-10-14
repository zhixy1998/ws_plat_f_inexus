/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.dto.concise.common;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName("f_t_measure_equal_sample")
public class EqualAndSample extends BasePO {

    @TableField("equal_id")
    @ApiModelProperty("平行样id")
    private Integer equalId;

    @TableField("sample_id")
    @ApiModelProperty("样品id")
    private String sampleId;

    @TableField("task_id")
    @ApiModelProperty("任务id")
    private Object taskId;

    @TableField("equal_sample_key")
    @ApiModelProperty("平行样唯一标识")
    private String equalSampleKey;

}
