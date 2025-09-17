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

    @ApiModelProperty("快捷登录开关")
    @TableField(value = "fast_login_switch")
    private Boolean fastLoginSwitch;

    @ApiModelProperty("自动进入测量开关")
    @TableField(value = "auto_measure")
    private Boolean autoMeasure;

    @ApiModelProperty("经典模式开关")
    @TableField(value = "classic_mode")
    private Boolean classicMode;

    @ApiModelProperty("向导模式开关")
    @TableField(value = "wizard_mode")
    private Boolean wizardMode;

    @ApiModelProperty("自动开机开关")
    @TableField("auto_start")
    private Boolean autoStart;


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
