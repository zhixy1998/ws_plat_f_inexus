package com.persee.workstation.uv_inexus.dto.concise.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.internal.util.privilegedactions.LoadClass;

import java.time.LocalDateTime;

@Data
public class FMeasureMethodDto {
    @ApiModelProperty("方法名称")
    private String methodName;
    @ApiModelProperty("使用时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime methodUseTime;



}
