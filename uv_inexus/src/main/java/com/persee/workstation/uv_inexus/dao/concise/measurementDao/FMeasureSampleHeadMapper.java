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
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureSampleHead;
import org.apache.ibatis.annotations.Mapper;

/**
 * 样本头表(FMeasureSampleHead)表数据库访问层
 */
@Mapper
public interface FMeasureSampleHeadMapper extends BaseMapper<FMeasureSampleHead> {
}
