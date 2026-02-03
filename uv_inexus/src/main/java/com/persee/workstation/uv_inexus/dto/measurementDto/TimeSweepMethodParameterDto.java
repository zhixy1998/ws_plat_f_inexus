/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.persee.workstation.uv_inexus.vo.measurementVo.timeSweep.TimeSweepParamterVo;
import lombok.Data;

/**
 * 时间扫描修改方法参数DTO
 */
@Data
public class TimeSweepMethodParameterDto extends TimeSweepParamterVo {
    // 项目ID
    private String projectId;
    // 测量类型
    private String measureType;
    // 方法ID
    private String methodId;
    // 方法名称
    private String methodName;
    // 任务id
    private String taskId;
    // 任务名称
    private String taskName;
    // 用户名
    private String updateBy;
    // 是否共享
    private String isShare;
    // 序列号
    private String deviceSerialNumber;
}
