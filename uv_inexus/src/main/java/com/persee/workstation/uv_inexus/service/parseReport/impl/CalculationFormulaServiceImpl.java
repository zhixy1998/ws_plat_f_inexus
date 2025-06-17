    package com.persee.workstation.uv_inexus.service.parseReport.impl;

    import com.persee.workstation.uv_inexus.dao.parseReport.CalculationFormulaDao;
    import com.persee.workstation.uv_inexus.po.parseReport.CalculationFormula;
    import com.persee.workstation.uv_inexus.service.parseReport.CalculationFormulaService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    import java.util.List;

    @Service // Spring注解，标记这是一个服务组件，会被Spring管理
    public class CalculationFormulaServiceImpl implements CalculationFormulaService {

        @Autowired // Spring注解，自动注入CalculationFormulaDao的实例
        private CalculationFormulaDao CalculationFormulaDao;

        @Override
        public List<CalculationFormula> findAll() {
            return CalculationFormulaDao.findAll();
        }

        @Override
        public CalculationFormula findById(Integer id) {
            return CalculationFormulaDao.findById(id);
        }

        @Override
        public void insert(CalculationFormula CalculationFormula) {
            CalculationFormulaDao.insert(CalculationFormula);
        }

        @Override
        public void update(CalculationFormula CalculationFormula) {
            CalculationFormulaDao.update(CalculationFormula);
        }

        @Override
        public void deleteById(Integer id) {
            CalculationFormulaDao.deleteById(id);
        }
    }
    