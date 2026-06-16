package com.persee.workstation.uv_inexus.po.concise.measurementPo;/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.persee.workstation.uv_inexus.model.BasePO;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "t_sample_pool_config", autoResultMap = true)
public class SamplePoolConfigPO extends BasePO {

    @ApiModelProperty("设备序列号")
    @TableField(value = "device_serial_number")
    private String deviceSerialNumber;

    @ApiModelProperty("样品池类型")
    @TableField(value = "attachment_type")
    private Integer attachmentType;

    @ApiModelProperty("样品池类型名称")
    @TableField(value = "attachment_type_name")
    private String attachmentTypeName;

    @ApiModelProperty("是否自动切换样品池")
    @TableField(value = "is_auto_change_pool")
    private Boolean isAutoChangePool;

    @ApiModelProperty("当前样品池编号")
    @TableField(value = "current_pool_num")
    private Integer currentPoolNum;

//    @ApiModelProperty("样品池信息")
//    @TableField(value = "pool_info")
//    private String poolInfo;

    @ApiModelProperty("样品池信息")
    @TableField(value = "pool_info", typeHandler = JacksonTypeHandler .class)
    private List<FSamplePoolStatus> poolInfo;


}
