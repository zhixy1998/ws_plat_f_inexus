package com.persee.workstation.uv_inexus.po.systemManagePo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_system_business_param")
public class SystemBusinessParamPO extends BasePO {

    @ApiModelProperty("参数配置类型")
    @TableField(value = "business_type")
    private Integer businessType;

    @ApiModelProperty("业务参数名称")
    @TableField(value = "business_name")
    private String businessName;

    @ApiModelProperty("配置参数")
    @TableField(value = "conf_param")
    private String confParam;

    @ApiModelProperty("配置参数名")
    @TableField(value = "param_name")
    private String paramName;

    @ApiModelProperty("配置参数值")
    @TableField(value = "param_type")
    private Integer paramType;

    @ApiModelProperty("配置参数默认值")
    @TableField(value = "param_default_value")
    private String paramDefaultValue;

    @ApiModelProperty("配置参数用户值")
    @TableField(value = "param_user_value")
    private String paramUserValue;

    @ApiModelProperty("配置参数最小值")
    @TableField(value = "param_min_value")
    private String paramMinValue;

    @ApiModelProperty("配置参数最大值")
    @TableField(value = "param_max_value")
    private String paramMaxValue;
}
