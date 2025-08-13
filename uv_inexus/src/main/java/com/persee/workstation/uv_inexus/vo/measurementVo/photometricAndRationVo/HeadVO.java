package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HeadVO {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("标识")
    private String identity;

    @ApiModelProperty("定位")
    private String locate;

    public HeadVO(String name, String identity) {
        this.name = name;
        this.identity = identity;
        this.locate = null;
    }

    public HeadVO(String name, String identity, String locate) {
        this.name = name;
        this.identity = identity;
        this.locate = locate;
    }
}
