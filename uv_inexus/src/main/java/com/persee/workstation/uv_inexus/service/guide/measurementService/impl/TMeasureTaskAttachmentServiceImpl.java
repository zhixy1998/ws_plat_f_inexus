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

package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureTaskAttachmentMapper;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTaskAttachment;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureTaskAttachmentService;
import org.springframework.stereotype.Service;

@Service
("tMeasureTaskAttachmentService")
public class TMeasureTaskAttachmentServiceImpl extends ServiceImpl<TMeasureTaskAttachmentMapper, TMeasureTaskAttachment> implements TMeasureTaskAttachmentService {
    @Override
    public R<TMeasureTaskAttachment> queryByTaskId(String taskId) {

        LambdaQueryWrapper<TMeasureTaskAttachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureTaskAttachment::getTaskId, taskId);
        TMeasureTaskAttachment attachment = this.getOne(queryWrapper);
        if (attachment != null) {
            return R.ok(attachment);
        }
        return R.fail("未找到记录");
    }
}
