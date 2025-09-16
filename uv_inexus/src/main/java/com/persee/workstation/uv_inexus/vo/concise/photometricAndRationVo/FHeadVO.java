package com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FHeadVO {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("标识")
    private String identity;

    @ApiModelProperty("打印标题")
    private String printTitle;

    @ApiModelProperty("定位")
    private String locate;

    public FHeadVO(String name, String identity) {
        this.name = name;
        this.identity = identity;
        this.locate = null;
    }

    public FHeadVO(String name, String identity, String locate) {
        this.name = name;
        this.identity = identity;
        this.locate = locate;
    }
    public FHeadVO(String name, String identity, String locate, String printTitle) {
        this.name = name;
        this.identity = identity;
        this.printTitle = printTitle;
        this.locate = locate;
    }
}
