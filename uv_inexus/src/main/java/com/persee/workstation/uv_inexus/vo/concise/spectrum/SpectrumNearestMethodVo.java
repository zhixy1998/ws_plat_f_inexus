/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.concise.spectrum;

import lombok.Data;

/**
 * 最近方法返回
 */
@Data
public class SpectrumNearestMethodVo {

    // 方法id
    private String methodId;

    // 前处理Key
    private String preprocessKey;

    // 多个计算公式Key
    private String multipleCalculationFormulaKey;

    // 方法名称
    private String methodName;

    // 测量类型
    private String measurementType;

    // 测量参数信息
    private FSpectrumParameterVo methodParam;

    // 是否共享方法(0.否 1.是)
    private String isSharingMethod;

    //  方法创建时间
    private String methodCreateTime;

    // 方法创建人
    private String methodCreator;

    // 方法使用时间
    private String methodUseTime;

    // 是否草稿(0.不是草稿 1.是草稿)
    private String isDraft;

    // 使用状态
    private String useState;

    // 任务id
    private String taskId;
}
