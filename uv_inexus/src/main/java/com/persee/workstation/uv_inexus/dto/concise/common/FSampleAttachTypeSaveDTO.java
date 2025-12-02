/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.concise.common;

import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class FSampleAttachTypeSaveDTO {

    @ApiModelProperty("设备序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("样品池类型")
    private Integer attachmentType;

    @ApiModelProperty("样品池类型名称")
    private String attachmentTypeName;

    @ApiModelProperty("当前样品池编号")
    private Integer currentPoolNum;

    @ApiModelProperty("样品池信息")
    private List<FSamplePoolStatus> poolStatusList;

    @ApiModelProperty("更新人")
    private String updateBy;
}
