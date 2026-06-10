/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;

@Data
public class FSamplePoolStatus {

    @ApiModelProperty("样品池号")
    private Integer poolNum;

    @ApiModelProperty("样品池号名称")
    private String poolName;

    @ApiModelProperty("样品池用途")
    private Integer poolPurpose;

    @ApiModelProperty("样品池用途名称")
    private String poolPurposeName;

    @ApiModelProperty("样品池状态，true-可用，false-不可用")
    private Boolean poolStatus;

    @ApiModelProperty("样品池数据，比色皿匹配值，待定")
    private Map<String, String> poolData;

    @ApiModelProperty("比色皿校正时的光度模式")
    private String luminosity;
}
