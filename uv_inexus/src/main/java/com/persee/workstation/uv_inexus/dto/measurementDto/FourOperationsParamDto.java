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

@Data
public class FourOperationsParamDto {
    // 测量数据id1
    private String measureDataId1;
    // 测量数据id2
    private String measureDataId2;
    // 四则运算符(0.加,1.减,2.乘,3.除)
    private String arithmeticOperators;
    // 数据类型(0.数值,1.谱图)
    private String dataType;
    // 分析数据名称
    private String analysisDataName;
    // 任务id
    private String taskId;

}
