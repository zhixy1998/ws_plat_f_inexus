/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.guide.systemManagePo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "t_system_database")
public class SystemDatabaseConfPO extends BasePO {

    @ApiModelProperty("全局端口")
    @TableField(value = "global_port")
    private String globalPort;

    @ApiModelProperty("主数据库ip")
    @TableField(value = "main_database_ip")
    private String mainDatabaseIp;

    @ApiModelProperty("主数据库端口")
    @TableField(value = "main_database_port")
    private String mainDatabasePort;

    @ApiModelProperty("主数据库用户名")
    @TableField(value = "main_database_username")
    private String mainDatabaseUsername;

    @ApiModelProperty("主数据库密码")
    @TableField(value = "main_database_password")
    private String mainDatabasePassword;

    @ApiModelProperty("备数据库ip")
    @TableField(value = "spare_database_ip")
    private String spareDatabaseIp;

    @ApiModelProperty("备数据库端口")
    @TableField(value = "spare_database_port")
    private String spareDatabasePort;

    @ApiModelProperty("备数据库用户名")
    @TableField(value = "spare_database_username")
    private String spareDatabaseUsername;

    @ApiModelProperty("备数据库密码")
    @TableField(value = "spare_database_password")
    private String spareDatabasePassword;


}
