package com.persee.workstation.uv_inexus.po.concise.measurementPo;


import com.persee.workstation.uv_inexus.dto.measurementDto.SelectMethodsByTypeResult;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class MethodAndTasks {
    @ApiModelProperty("已创建方法数")
    private Integer allMethodNum;
    @ApiModelProperty("使用过的方法数")
    private Integer usedMethodNum;
    @ApiModelProperty("参与任务数")
    private Integer allTaskNum;
    @ApiModelProperty("已完成任务数")
    private Integer finishTaskNum;
    @ApiModelProperty("方法统计")
    private List<SelectMethodsByTypeResult> methodCounts;
    @ApiModelProperty("任务统计")
    private TaskCount taskCounts;

}
