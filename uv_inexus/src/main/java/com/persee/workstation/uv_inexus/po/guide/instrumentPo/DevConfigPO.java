/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.guide.instrumentPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xudian
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_ins_config")
public class DevConfigPO extends BasePO {


    @TableField("is_time_startup")
    @ApiModelProperty(value = "是否定时开机",example = "0-否，1-是")
    private Integer isTimeStartUp;

    @TableField("startup_time")
    @ApiModelProperty("定时开机时间")
    private String startUpTime;

    @TableField("message_port")
    @ApiModelProperty("通讯端口")
    private Integer messagePort;

}
