/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.measurementVo;

import lombok.Data;

@Data
public class RecentlyCompletedTasksVo {
    // 任务名称
    private String taskName;
    // 任务id
    private String taskId;
    // 方法id
    private String methodId;
    // 方法名称
    private String methodName;
    // 项目名称
    private String projectName;
    // 测量样品
    private String measurementSample;
    // 测量开始时间
    private String measurementStartTime;
    // 测量结束时间
    private String measurementEndTime;
}
