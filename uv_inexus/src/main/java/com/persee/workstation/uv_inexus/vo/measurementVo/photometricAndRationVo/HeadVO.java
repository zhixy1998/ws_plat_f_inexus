/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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

    @ApiModelProperty("打印标题")
    private String printTitle;

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
    public HeadVO(String name, String identity, String locate, String printTitle) {
        this.name = name;
        this.identity = identity;
        this.printTitle = printTitle;
        this.locate = locate;
    }
}
