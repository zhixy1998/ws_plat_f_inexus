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

import java.util.List;

@Data
public class DeviceMonitorVO {

    @ApiModelProperty(value = "数据集合")
    private List<DataNameValue> data;

    @ApiModelProperty("数据类型，区分要展示的数据，0-data,1-totalStatus")
    private String type;

    @ApiModelProperty("数据集合名称，eg:使用时长")
    private String dataName;

    @ApiModelProperty("状态")
    private String totalStatus;

}
