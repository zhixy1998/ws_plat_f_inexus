package com.persee.workstation.uv_inexus.service.measurementService.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.measurementDao.TMeasureCalculationFormulaMapper;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureCalculationFormula;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureCalculationFormulaService;
import org.springframework.stereotype.Service;

/**
 * 计算公式表(TMeasureCalculationFormula)表服务实现类
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
@Service("tMeasureCalculationFormulaService")
public class TMeasureCalculationFormulaServiceImpl extends ServiceImpl<TMeasureCalculationFormulaMapper, TMeasureCalculationFormula> implements TMeasureCalculationFormulaService {

}

