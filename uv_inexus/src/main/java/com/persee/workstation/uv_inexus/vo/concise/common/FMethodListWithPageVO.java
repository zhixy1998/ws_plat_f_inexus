package com.persee.workstation.uv_inexus.vo.concise.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class FMethodListWithPageVO {

    @ApiModelProperty("总记录数")
    protected long total;

    @ApiModelProperty("每页记录数")
    protected long size;

    @ApiModelProperty("当前页码")
    protected long current;

    @ApiModelProperty("方法信息")
    protected List<FMethodInfoVO> records;
}
