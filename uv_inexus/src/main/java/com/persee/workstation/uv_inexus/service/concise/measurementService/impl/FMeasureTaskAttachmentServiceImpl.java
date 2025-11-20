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

package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureTaskAttachmentMapper;
import com.persee.workstation.uv_inexus.dto.concise.common.FQueryDataDTO;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTaskAttachment;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskAttachmentService;
import org.springframework.stereotype.Service;

/**
 * 测量任务附件服务实现类(FMeasureTaskAttachment)
 */
@Service
("FMeasureTaskAttachmentService")
@DS("concise")
public class FMeasureTaskAttachmentServiceImpl extends ServiceImpl<FMeasureTaskAttachmentMapper, FMeasureTaskAttachment> implements FMeasureTaskAttachmentService {
    @Override
    public R<FMeasureTaskAttachment> queryByTaskId(String taskId) {
        LambdaQueryWrapper<FMeasureTaskAttachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureTaskAttachment::getTaskId, taskId);
        FMeasureTaskAttachment attachment = this.getOne(queryWrapper);
        if (attachment != null) {
            return R.ok(attachment);
        }
        //未找到记录
        return R.fail(YamlMessageSource.getMessageStatic("00100025"));
    }

    @Override
    public Integer queryLastDigit(String taskId) {
        LambdaQueryWrapper<FMeasureTaskAttachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureTaskAttachment::getTaskId, taskId);
        FMeasureTaskAttachment attachment = this.getOne(queryWrapper);

        if (attachment != null && attachment.getDataParam() != null) {
            FQueryDataDTO dataDTO = JSON.parseObject(attachment.getDataParam(), FQueryDataDTO.class);
            if (dataDTO.getDigit() != null) {
                return dataDTO.getDigit();
            }
        }
        return null;
    }
}
