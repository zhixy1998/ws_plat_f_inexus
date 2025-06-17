    package com.persee.workstation.uv_inexus.service.parseReport;

    import com.persee.workstation.uv_inexus.po.parseReport.CalculationFormula;
    import java.util.List;

    public interface CalculationFormulaService {

        List<CalculationFormula> findAll();

        CalculationFormula findById(Integer id);

        void insert(CalculationFormula CalculationFormula);

        void update(CalculationFormula CalculationFormula);

        void deleteById(Integer id);
    }
    