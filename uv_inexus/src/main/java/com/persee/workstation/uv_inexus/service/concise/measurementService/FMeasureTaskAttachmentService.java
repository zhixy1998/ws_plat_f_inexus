/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.service.concise.measurementService;

import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTaskAttachment;

/**
 * 任务附属表(FMeasureTaskAttachment)表服务接口
 */
public interface FMeasureTaskAttachmentService extends IService<FMeasureTaskAttachment> {
    R<FMeasureTaskAttachment> queryByTaskId(String taskId);

    Integer queryLastDigit(String taskId);
}
