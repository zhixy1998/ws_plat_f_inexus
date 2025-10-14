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

package com.persee.workstation.uv_inexus.service.guide.measurementService;

import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTaskAttachment;

/**
 * 任务附属表(TMeasureTaskAttachment)表服务接口
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
public interface TMeasureTaskAttachmentService extends IService<TMeasureTaskAttachment> {

    R<TMeasureTaskAttachment> queryByTaskId(String taskId);
}
