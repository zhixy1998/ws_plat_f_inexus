package com.persee.workstation.uv_inexus.dto.guide;

import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TSamplePoolStatus;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
@Data
public class TSampleAttachTypeSaveDTO {

    @ApiModelProperty("设备序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("样品池类型")
    private Integer attachmentType;

    @ApiModelProperty("样品池类型名称")
    private String attachmentTypeName;

    @ApiModelProperty("当前样品池编号")
    private Integer currentPoolNum;

    @ApiModelProperty("样品池信息")
    private List<TSamplePoolStatus> poolStatusList;

    @ApiModelProperty("更新人")
    private String updateBy;
}
