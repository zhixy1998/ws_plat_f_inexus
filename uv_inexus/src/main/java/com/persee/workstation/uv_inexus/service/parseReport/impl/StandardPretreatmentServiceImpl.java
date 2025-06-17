    package com.persee.workstation.uv_inexus.service.parseReport.impl;

    import com.persee.workstation.uv_inexus.dao.parseReport.StandardPretreatmentDao;
    import com.persee.workstation.uv_inexus.po.parseReport.StandardPretreatment;
    import com.persee.workstation.uv_inexus.service.parseReport.StandardPretreatmentService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    import java.util.List;

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
    }
    