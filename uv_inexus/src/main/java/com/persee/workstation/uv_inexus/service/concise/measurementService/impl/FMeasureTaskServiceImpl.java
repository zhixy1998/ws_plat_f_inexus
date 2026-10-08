/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureTaskMapper;
import com.persee.workstation.uv_inexus.dto.concise.common.FMeasureTaskDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.FQueryDataDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.SelectTaskDto;
import com.persee.workstation.uv_inexus.model.Result;
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
    @Autowired
    FMeasureTaskMapper fMeasureTaskMapper;

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


    public boolean save(FMeasureTask entity, FQueryDataDTO queryDataDTO) {
        boolean save = super.save(entity);
        if (!save) {
            return false;
        }

        FMeasureTaskAttachment taskAttachment = new FMeasureTaskAttachment();
        taskAttachment.setTaskId(entity.getId());

        if (queryDataDTO == null) {
            queryDataDTO = new FQueryDataDTO();
            queryDataDTO.setTaskId(entity.getId());
            queryDataDTO.setAllDisplay(false);
            queryDataDTO.setDigit(Constants.DEFAULT_DECIMAL_NUM);
            queryDataDTO.setOption(new ArrayList<>());
        }
        taskAttachment.setDataParam(JSON.toJSONString(queryDataDTO));

        fMeasureTaskAttachmentService.save(taskAttachment);

        return true;
    }




    @Override
    public Result<Page<FMeasureTaskDTO>> queryByName(SelectTaskDto selectTaskDto) {

        try {
            //创建分页对象
            Page<FMeasureTaskDTO> page = new Page<>(selectTaskDto.getPageNum(), selectTaskDto.getPageSize());

            IPage<FMeasureTaskDTO> resultPage = fMeasureTaskMapper.queryByMethodName(page, selectTaskDto);

            return Result.ok((Page<FMeasureTaskDTO>) resultPage);
        } catch (Exception e) {
            return Result.fail();
        }
    }
}

