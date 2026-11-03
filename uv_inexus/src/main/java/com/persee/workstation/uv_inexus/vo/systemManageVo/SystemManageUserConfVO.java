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
public class SystemManageUserConfVO {

    @ApiModelProperty("全局端口")
    private String globalPort;

//    @ApiModelProperty("默认进入经典模式")
//    private Boolean defaultEnterClassic;

    @ApiModelProperty("主数据库ip")
    private String mainDatabaseIp;

    @ApiModelProperty("主数据库端口")
    private String mainDatabasePort;

    @ApiModelProperty("备数据库ip")
    private String spareDatabaseIp;

    @ApiModelProperty("备数据库端口")
    private String spareDatabasePort;

}
