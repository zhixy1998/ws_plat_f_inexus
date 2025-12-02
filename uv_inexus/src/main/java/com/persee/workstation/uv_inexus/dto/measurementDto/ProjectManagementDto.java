/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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
