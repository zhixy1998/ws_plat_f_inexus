    /*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.parseReport.impl;

    import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.guide.parseReport.StandardPretreatmentDao;
import com.persee.workstation.uv_inexus.po.guide.parseReport.StandardPretreatment;
import com.persee.workstation.uv_inexus.service.guide.parseReport.StandardPretreatmentService;

import lombok.extern.slf4j.Slf4j;

    @Slf4j
    @Service // Spring注解，标记这是一个服务组件，会被Spring管理
    public class StandardPretreatmentServiceImpl implements StandardPretreatmentService {

        @Autowired // Spring注解，自动注入StandardPretreatmentDao的实例
        private StandardPretreatmentDao StandardPretreatmentDao;

        @Override
        public List<StandardPretreatment> findAll() {
            return StandardPretreatmentDao.findAll();
        }

        @Override
        public StandardPretreatment findById(Integer id) {
            return StandardPretreatmentDao.findById(id);
        }

        @Override
        public void insert(StandardPretreatment StandardPretreatment) {
            StandardPretreatmentDao.insert(StandardPretreatment);
        }

        @Override
        public void update(StandardPretreatment StandardPretreatment) {
            StandardPretreatmentDao.update(StandardPretreatment);
        }

        @Override
        public void deleteById(Integer id) {
            StandardPretreatmentDao.deleteById(id);
        }

        @Override
        public IPage<StandardPretreatment> findPage(Page<StandardPretreatment> page, Map<String, String> params) {
            QueryWrapper<StandardPretreatment> queryWrapper = new QueryWrapper<>();
            
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
            
            IPage<StandardPretreatment> resultPage = StandardPretreatmentDao.selectPage(page, queryWrapper);
            
            log.info("分页查询结果 - 总数: {}, 当前页记录数: {}, 页码: {}, 页大小: {}", 
                    resultPage.getTotal(), 
                    resultPage.getRecords().size(),
                    resultPage.getCurrent(),
                    resultPage.getSize());
                    
            return resultPage;
        }
    }
    