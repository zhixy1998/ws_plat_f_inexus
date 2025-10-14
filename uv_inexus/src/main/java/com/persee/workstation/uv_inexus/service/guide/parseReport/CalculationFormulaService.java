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

package com.persee.workstation.uv_inexus.service.guide.parseReport;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.po.guide.parseReport.CalculationFormula;

public interface CalculationFormulaService {

    List<CalculationFormula> findAll();

    CalculationFormula findById(Integer id);

    void insert(CalculationFormula CalculationFormula);

    void update(CalculationFormula CalculationFormula);

    void deleteById(Integer id);
    // 新增分页方法
    // IPage<CalculationFormula> findPage(Page<CalculationFormula> page, String keyword);

    IPage<CalculationFormula> findPage(Page<CalculationFormula> page, Map<String, String> params);
}
    