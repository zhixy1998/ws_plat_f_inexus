/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.concise.common;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FMethodInfoVO {


    // 前处理Key
    @ApiModelProperty("前处理Key")
    private String preprocessKey;

    // 多个计算公式Key
    @ApiModelProperty("多个计算公式Key")
    private String multipleCalculationFormulaKey;

    // 所属项目Key(当选用的是公共方法则项目key为空)

    @ApiModelProperty("所属项目Key")
    private String projectItemKey;

    // 方法名称
    @ApiModelProperty("方法名称")
    private String methodName;

    // 测量类型
    @ApiModelProperty("测量类型")
    private String measurementType;

    // 测量参数信息
    @ApiModelProperty("测量参数信息")
    private JSONObject methodParam;

    // 是否共享方法(0.否 1.是)
    @ApiModelProperty("是否共享方法")
    private String isSharingMethod;

    //  方法创建时间
    @ApiModelProperty("方法创建时间")
    private LocalDateTime methodCreateTime;

    // 方法创建人
    @ApiModelProperty("方法创建人")
    private String methodCreator;

}
