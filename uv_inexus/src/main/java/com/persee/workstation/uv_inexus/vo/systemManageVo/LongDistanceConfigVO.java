/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.systemManageVo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class LongDistanceConfigVO {
    //是否允许远程控制
    @ApiModelProperty("是否允许远程控制")
    private Integer remote_control;
    //是否远程数据访问
    @ApiModelProperty("是否远程数据访问")
    private Integer remote_data_access;
}
