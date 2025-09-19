package com.persee.workstation.uv_inexus.po.concise.measurementPo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("f_t_sample_pool_config")
public class FSamplePoolConfigPO extends BasePO {

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

    @ApiModelProperty("样品池信息")
    @TableField(value = "pool_info")
    private String poolInfo;


}
