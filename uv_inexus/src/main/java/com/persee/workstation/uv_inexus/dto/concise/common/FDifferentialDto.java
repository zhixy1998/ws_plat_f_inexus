/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.concise.common;

import lombok.Data;

/**
 * 微分接口参数
 */
@Data
public class FDifferentialDto {
    // 待处理数据id
    private String measureDataId;
    // 微分次数
    private String differentialNum;
    // 点数(点数要比系数小)
    private String pointNum;
    // 系数
    private String coefficient;
    // 分析数据名称
    private String analysisDataName;
    // 任务id
    private String taskId;
}
