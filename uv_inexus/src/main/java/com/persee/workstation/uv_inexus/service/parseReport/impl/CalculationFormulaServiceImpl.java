package com.persee.workstation.uv_inexus.service.parseReport.impl;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.parseReport.CalculationFormulaDao;
import com.persee.workstation.uv_inexus.po.parseReport.CalculationFormula;
import com.persee.workstation.uv_inexus.service.parseReport.CalculationFormulaService;

import lombok.extern.slf4j.Slf4j;

    @Slf4j
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

        @Override
        public IPage<CalculationFormula> findPage(Page<CalculationFormula> page, Map<String, String> params) {
            QueryWrapper<CalculationFormula> queryWrapper = new QueryWrapper<>();
            
            // 动态构建查询条件
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                if ("pageNum".equals(key) || "pageSize".equals(key)) {
                    continue;
                }
                
                if (StringUtils.isNotBlank(value)) {
                    queryWrapper.like(key, value);
                }
            }
            
            // 确保分页稳定，按 ID 排序
            // queryWrapper.orderByAsc("id");
            
            // 打印 SQL（调试用）
            System.out.println("执行的 SQL: " + queryWrapper.getCustomSqlSegment());
            
            IPage<CalculationFormula> resultPage = CalculationFormulaDao.selectPage(page, queryWrapper);
            
            log.info("分页查询结果 - 总数: {}, 当前页记录数: {}, 页码: {}, 页大小: {}", 
                    resultPage.getTotal(), 
                    resultPage.getRecords().size(),
                    resultPage.getCurrent(),
                    resultPage.getSize());
                    
            return resultPage;
        }

    }
    