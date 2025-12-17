package com.persee.workstation.uv_inexus.dto.factory_inspection;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Data
public class StdMaterialSelectionDto {
    @ApiModelProperty("任务id")
    private String taskId;
    //检测项名字
    @ApiModelProperty("检测项名字")
    private String itemName;
    //检测项id
    @ApiModelProperty("检测项id")
    private String itemId;
    //检测项组名字
    @ApiModelProperty("检测项组名字")
    private String groupName;
    //标准物id
    @ApiModelProperty("标准物id")
    private String standardId;
    @ApiModelProperty("标准物类型（校零 zero 测量 measurement）")
    private String standardType;
}
