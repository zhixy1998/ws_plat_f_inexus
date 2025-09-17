package com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MethodQueryVO {

    @ApiModelProperty("方法名")
    private String methodName;

    @ApiModelProperty("保存时间")
    private LocalDateTime createTime;

    @ApiModelProperty("最近使用时间")
    private LocalDateTime methodUseTime;

    @ApiModelProperty("方法参数")
    private JSONObject methodParam;
}
