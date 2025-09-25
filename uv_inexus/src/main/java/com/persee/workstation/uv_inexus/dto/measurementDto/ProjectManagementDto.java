package com.persee.workstation.uv_inexus.dto.measurementDto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectManagementDto {

    private String id;
    //项目名称
    private String projectName;
    //创建时间
    private LocalDateTime createTime;
    //创建人
    private String creatorKey;
    //方法个数
    private Long methodNum;
    //任务个数
    private Long taskNum;
    //更新时间
    private LocalDateTime lastUpdatedTime;

}
