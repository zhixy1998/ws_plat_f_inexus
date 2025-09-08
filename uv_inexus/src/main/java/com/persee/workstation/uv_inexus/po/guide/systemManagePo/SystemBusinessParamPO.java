package com.persee.workstation.uv_inexus.po.guide.systemManagePo;

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

    @ApiModelProperty("峰间距")
    @TableField("peak_interval")
    private String peakInterval;

    @ApiModelProperty("峰谷差")
    @TableField("peak_valley_diff")
    private String peakValleyDiff;


    @ApiModelProperty("自动保存间隔")
    @TableField("auto_save_time")
    private String autoSaveTime;

    @ApiModelProperty("自动登出时间")
    @TableField("auto_leave_time")
    private String autoLeaveTime;

    @TableField("exe_file_path")
    @ApiModelProperty("exe文件路径")
    private String filePath;

}
