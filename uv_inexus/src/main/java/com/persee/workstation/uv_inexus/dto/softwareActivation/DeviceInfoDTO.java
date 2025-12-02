/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.softwareActivation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("发现的设备信息")
public class DeviceInfoDTO {
    @ApiModelProperty("ID")
    private String id; // 新增，与PO对应
    
    @ApiModelProperty("设备序列号")
    private String deviceNumber;
    
    @ApiModelProperty("设备IP地址")
    private String deviceIp;
    
    @ApiModelProperty("设备类型")
    private String type;
    
    // 新增PO中的核心字段，与数据库表结构对应
    @ApiModelProperty("激活状态（0=未激活，1=已激活）")
    private int activationStatus;
    
    @ApiModelProperty("激活时间")
    private String activationTime;
    
    @ApiModelProperty("修改时间")
    private String modificationTime;
    
    @ApiModelProperty("过期时间")
    private String expirationDate;
    
    @ApiModelProperty("附加功能")
    private String additionalFunctions;

    @ApiModelProperty("是否是新发现")
    private int isAddStatus;
}