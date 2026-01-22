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

import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
public class QueryTaskDto {
    // 项目ID
    @NotBlank(message = "项目ID不能为空")
    private String projectId;
    // 方法ID
    private List<@Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "方法ID只能包含字母、数字、下划线和减号")String> methodIds;
    // 测量类型
    @NotBlank(message = "测量类型不能为空")
    private String measurementType;
    // 页码 - 可选，有默认值
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;
    // 每页大小 - 可选，有默认值
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    private Integer pageSize = 20;
    // 开始时间
    private String startTime;
    // 结束时间
    private String endTime;
    // 用户名称
    @NotBlank(message = "用户名不能为空")
    private String updateBy;

    /**
     * 自定义校验方法 - 校验结束时间必须晚于开始时间
     */
    @AssertTrue(message = "结束时间必须晚于开始时间")
    public boolean isTimeRangeValid() {
        if (startTime == null || endTime == null) {
            return true; // 让@NotBlank处理空值情况
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime start = LocalDateTime.parse(startTime, formatter);
            LocalDateTime end = LocalDateTime.parse(endTime, formatter);
            return end.isAfter(start);
        } catch (Exception e) {
            return false; // 格式错误会在@Pattern中捕获
        }
    }
}
