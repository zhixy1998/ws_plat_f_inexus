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
    