package com.persee.workstation.uv_inexus.dto.factory_inspection;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

/*{
  "taskId": 123,
  "items": [
    {
      "id": 456,
      "zeroId": 789,
      "measureId": 101
    },
    {
      "id": 457,
      "zeroId": 790,
      "measureId": 102
    }
  ]
}*/
@Data
public class TaskDetectionDTO {
    //任务id
    @ApiModelProperty("任务ID")
    private String taskId;
    @ApiModelProperty("检测项列表")
    private List<DetectionItem> items;

    @Data
    public static class DetectionItem {
        @ApiModelProperty("检测项ID")
        private String id;        // 检测项ID
        @ApiModelProperty("校零物ID")
        private String zeroId;    // 校零物质ID
        @ApiModelProperty("测量物ID")
        private String measureId; // 测量物质ID
    }
}


