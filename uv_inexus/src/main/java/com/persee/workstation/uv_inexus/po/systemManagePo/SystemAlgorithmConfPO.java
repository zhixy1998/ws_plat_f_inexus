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

    @ApiModelProperty("透过率门限系数k")
    @TableField("transmittance_k")
    private String Transmittance;

    @ApiModelProperty("积分球门限系数k")
    @TableField("integrating_sphere_k")
    private String IntegratingSphere;

    @ApiModelProperty("Abs门限系数k")
    @TableField("abs_k")
    private String Abs;
}
