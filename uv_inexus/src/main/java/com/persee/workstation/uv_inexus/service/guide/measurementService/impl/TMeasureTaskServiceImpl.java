/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dto.measurementDto.QueryDataDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTaskAttachment;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureTaskAttachmentService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * 测量任务表(TMeasureTask)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("tMeasureTaskService")
public class TMeasureTaskServiceImpl extends ServiceImpl<TMeasureTaskMapper, TMeasureTask> implements TMeasureTaskService {


    @Autowired
    TMeasureTaskAttachmentService tMeasureTaskAttachmentService;
    @Autowired
    TMeasureTaskMapper tMeasureTaskMapper;

    @Override
    public boolean save(TMeasureTask entity) {
        entity.setIsDelete("0");
        boolean save = super.save(entity);
        if (!save) {
            return false;
        }

        R<TMeasureTaskAttachment> attachmentR = tMeasureTaskAttachmentService.queryByTaskId(entity.getId());
        if (!attachmentR.getSuccess()) {
            TMeasureTaskAttachment taskAttachment = new TMeasureTaskAttachment();
            taskAttachment.setTaskId(entity.getId());

            QueryDataDTO queryDataDTO = new QueryDataDTO();
            queryDataDTO.setTaskId(entity.getId());
            queryDataDTO.setAllDisplay(false);
            queryDataDTO.setDigit(Constants.DEFAULT_DECIMAL_NUM);
            queryDataDTO.setOption(new ArrayList<>());
            taskAttachment.setDataParam(JSON.toJSONString(queryDataDTO));

            tMeasureTaskAttachmentService.save(taskAttachment);
        }



        return true;
    }
}

