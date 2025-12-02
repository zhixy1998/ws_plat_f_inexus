/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;


import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureAnalysisDataMapper;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureAnalysisData;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureAnalysisDataService;
import org.springframework.stereotype.Service;

/**
 * 分析数据表(FMeasureAnalysisData)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("FMeasureAnalysisDataService")
@DS("concise")
public class FMeasureAnalysisDataServiceImpl extends ServiceImpl<FMeasureAnalysisDataMapper, FMeasureAnalysisData> implements FMeasureAnalysisDataService {

}

