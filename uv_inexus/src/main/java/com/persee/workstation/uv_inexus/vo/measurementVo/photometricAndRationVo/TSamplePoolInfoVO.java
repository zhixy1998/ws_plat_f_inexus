package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TSamplePoolStatus;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
@Data
public class TSamplePoolInfoVO {

    @ApiModelProperty("设备序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("样品池类型")
    private Integer attachmentType;

    @ApiModelProperty("样品池类型名称")
    private String attachmentTypeName;

    @ApiModelProperty("是否自动切换样品池")
    private Boolean isAutoChangePool;

    @ApiModelProperty("当前样品池编号")
    private Integer currentPoolNum;

    @ApiModelProperty("样品池信息")
    private List<TSamplePoolStatus> poolStatusList;
}
