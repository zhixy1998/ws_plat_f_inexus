package com.persee.workstation.uv_inexus.vo.instrumentVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class DeviceAttachmentTypeVO {

    @ApiModelProperty("附件类型名称")
    private String attachmentName;

    @ApiModelProperty("仪器序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("样本池数量")
    private Integer samplePoolNum;
}
