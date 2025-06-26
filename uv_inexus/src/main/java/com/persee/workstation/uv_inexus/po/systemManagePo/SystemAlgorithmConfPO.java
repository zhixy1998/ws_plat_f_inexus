package com.persee.workstation.uv_inexus.po.systemManagePo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("系统算法配置")
@TableName("t_system_algorithm_conf")
public class SystemAlgorithmConfPO extends BasePO {

    @ApiModelProperty("算法类型")
    @TableField(value = "algorithm_type")
    private Integer algorithmType;

    @ApiModelProperty("算法名称")
    @TableField(value = "algorithm_name")
    private String algorithmName;

    @ApiModelProperty("参数，配置对应的参，后端查询使用")
    @TableField(value = "conf_param")
    private String confParam;

    @ApiModelProperty("参数名，中文名，供前端参考")
    @TableField(value = "conf_param_name")
    private String confParamName;

    @ApiModelProperty("默认值")
    @TableField(value = "conf_default_value")
    private String confDefaultValue;

    @ApiModelProperty("用户值")
    @TableField(value = "conf_user_value")
    private String confUserValue;

    @ApiModelProperty("最小值")
    @TableField(value = "min_value")
    private String minValue;

    @ApiModelProperty("最大值")
    @TableField(value = "max_value")
    private String maxValue;
}
