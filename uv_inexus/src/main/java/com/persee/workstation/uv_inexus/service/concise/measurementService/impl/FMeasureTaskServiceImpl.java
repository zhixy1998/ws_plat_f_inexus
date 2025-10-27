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
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dto.concise.common.FQueryDataDTO;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTaskAttachment;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskAttachmentService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * 测量任务表(FMeasureTask)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("FMeasureTaskService")
@DS("concise")
public class FMeasureTaskServiceImpl extends ServiceImpl<FMeasureTaskMapper, FMeasureTask> implements FMeasureTaskService {

    @Autowired
    FMeasureTaskAttachmentService fMeasureTaskAttachmentService;

    @Override
    public boolean save(FMeasureTask entity) {
        boolean save = super.save(entity);
        if (!save) {
            return false;
        }
        FMeasureTaskAttachment taskAttachment = new FMeasureTaskAttachment();
        taskAttachment.setTaskId(entity.getId());

        FQueryDataDTO queryDataDTO = new FQueryDataDTO();
        queryDataDTO.setTaskId(entity.getId());
        queryDataDTO.setAllDisplay(false);
        queryDataDTO.setDigit(Constants.DEFAULT_DECIMAL_NUM);
        queryDataDTO.setOption(new ArrayList<>());
        taskAttachment.setDataParam(JSON.toJSONString(queryDataDTO));

        fMeasureTaskAttachmentService.save(taskAttachment);

        return true;
    }
}

