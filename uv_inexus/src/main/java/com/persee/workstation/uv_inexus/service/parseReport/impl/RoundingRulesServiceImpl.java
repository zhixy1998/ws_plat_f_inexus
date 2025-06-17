    package com.persee.workstation.uv_inexus.service.parseReport.impl;

    import com.persee.workstation.uv_inexus.dao.parseReport.RoundingRulesDao;
    import com.persee.workstation.uv_inexus.po.parseReport.RoundingRules;
    import com.persee.workstation.uv_inexus.service.parseReport.RoundingRulesService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    import java.util.List;

    @Service // Spring注解，标记这是一个服务组件，会被Spring管理
    public class RoundingRulesServiceImpl implements RoundingRulesService {

        @Autowired // Spring注解，自动注入RoundingRulesDao的实例
        private RoundingRulesDao RoundingRulesDao;

        @Override
        public List<RoundingRules> findAll() {
            return RoundingRulesDao.findAll();
        }

        @Override
        public RoundingRules findById(Integer id) {
            return RoundingRulesDao.findById(id);
        }

        @Override
        public void insert(RoundingRules RoundingRules) {
            RoundingRulesDao.insert(RoundingRules);
        }

        @Override
        public void update(RoundingRules RoundingRules) {
            RoundingRulesDao.update(RoundingRules);
        }

        @Override
        public void deleteById(Integer id) {
            RoundingRulesDao.deleteById(id);
        }
    }
    