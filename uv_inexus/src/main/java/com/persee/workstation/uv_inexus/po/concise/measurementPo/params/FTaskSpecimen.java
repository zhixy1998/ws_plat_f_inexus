/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.concise.measurementPo.params;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FTaskSpecimen {
    // 样本id
    private String specimenKey;
    // 删除状态(0.未删除,1.删除)
    private String deleteType;
    // 是否已测(0.未测,1.已测)
    private String isTested;
    //样品种类
    private String sampleType;
    //样品类型
    private String sampleKind;
}