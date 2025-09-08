package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectManagementDto {
    //项目名称
    String projectName;
    //创建时间
    LocalDateTime createTime;
    //创建人
    String creatorKey;
    //备注
    String remark;

}
