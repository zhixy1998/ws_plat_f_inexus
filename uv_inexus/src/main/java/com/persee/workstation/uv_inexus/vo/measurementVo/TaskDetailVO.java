package com.persee.workstation.uv_inexus.vo.measurementVo;

import com.persee.workstation.uv_inexus.vo.measurementVo.SampleInfoVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

@Data
@ApiModel("任务详情")
public class TaskDetailVO {

    @ApiModelProperty("项目Id")
    private String projectId;

    @ApiModelProperty("任务Id")
    private String taskId;

    @ApiModelProperty("方法Id")
    private String methodId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("测量类型")
    private String measurementType;

    @ApiModelProperty("方法名称")
    private String methodName;

    @ApiModelProperty("使用仪器")
    private String instrumentName;

    @ApiModelProperty("样品信息列表")
    private List<SampleInfoVO> samples;
}