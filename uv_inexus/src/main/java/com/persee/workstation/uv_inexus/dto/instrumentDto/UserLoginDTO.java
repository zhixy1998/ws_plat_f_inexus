/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.instrumentDto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("用户登录数据类")
public class UserLoginDTO {

    @ApiModelProperty(value = "用户名", example = "admin")
    private String userName;

    @ApiModelProperty(value = "密码", example = "px123456")
    private String password;

    @ApiModelProperty(value = "是否快捷登录, 0-普通登录，1-快捷登录")
    private Integer isFastLogin;

    @ApiModelProperty(value = "是否自动开机", example = "0-非自动开机，1-自动开机")
    private String isOnlyData;

}

