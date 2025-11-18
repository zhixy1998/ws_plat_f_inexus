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


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.concise.common.FMeasureTaskDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.SelectTaskDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;

/**
 * 测量任务表(FMeasureTask)表服务接口
 */
public interface FMeasureTaskService extends IService<FMeasureTask> {

    boolean save(FMeasureTask entity);

    boolean delTask(String taskId);

    Result<Page<FMeasureTaskDTO>> querybyName(SelectTaskDto selectTaskDto);
}

