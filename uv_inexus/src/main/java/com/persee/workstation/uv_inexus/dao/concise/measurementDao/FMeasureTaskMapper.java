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

package com.persee.workstation.uv_inexus.dao.concise.measurementDao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FTaskBaseVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 测量任务表(TMeasureTask)表数据库访问层
 */

public interface FMeasureTaskMapper extends BaseMapper<FMeasureTask> {

    List<FTaskBaseVO> getAllTask(@Param("projectId") String projectId, @Param("measureType") String measureType);

    List<FHeadVO> getPreHead(@Param("measure_type") String measureType, @Param("sample_kind") String sampleKind);


    int delTask(String taskId);
}

