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


import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureDataMapper;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureData;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureDataService;
import org.springframework.stereotype.Service;

/**
 * 测量数据表(FMeasureData)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("FMeasureDataService")
@DS("concise")
public class FMeasureDataServiceImpl extends ServiceImpl<FMeasureDataMapper, FMeasureData> implements FMeasureDataService {

}

