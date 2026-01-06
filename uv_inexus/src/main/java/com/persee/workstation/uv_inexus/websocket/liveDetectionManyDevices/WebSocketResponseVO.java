/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WebSocketResponseVO {

    @ApiModelProperty(value = "操作类型")
    private String operation;

    @ApiModelProperty(value = "数据")
    private Object data;

    @ApiModelProperty(value = "当前时间")
    private LocalDateTime currentTime;

    @ApiModelProperty(value = "错误信息")
    private String errorMsg;
}
