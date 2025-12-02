/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class DeviceAttachmentTypeVO {

    @ApiModelProperty("附件类型")
    private Integer attachmentNum;

    @ApiModelProperty("附件类型名称")
    private String attachmentName;

    @ApiModelProperty("仪器序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("样本池数量")
    private Integer samplePoolNum;

    @ApiModelProperty("附件种类")
    private Integer attachmentKind;
}
