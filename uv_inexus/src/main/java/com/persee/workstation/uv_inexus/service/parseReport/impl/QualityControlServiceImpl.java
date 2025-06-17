    package com.persee.workstation.uv_inexus.service.parseReport.impl;

    import com.persee.workstation.uv_inexus.dao.parseReport.QualityControlDao;
    import com.persee.workstation.uv_inexus.po.parseReport.QualityControl;
    import com.persee.workstation.uv_inexus.service.parseReport.QualityControlService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    import java.util.List;

    @Service // Spring注解，标记这是一个服务组件，会被Spring管理
    public class QualityControlServiceImpl implements QualityControlService {

        @Autowired // Spring注解，自动注入QualityControlDao的实例
        private QualityControlDao QualityControlDao;

        @Override
        public List<QualityControl> findAll() {
            return QualityControlDao.findAll();
        }

        @Override
        public QualityControl findById(Integer id) {
            return QualityControlDao.findById(id);
        }

        @Override
        public void insert(QualityControl QualityControl) {
            QualityControlDao.insert(QualityControl);
        }

        @Override
        public void update(QualityControl QualityControl) {
            QualityControlDao.update(QualityControl);
        }

        @Override
        public void deleteById(Integer id) {
            QualityControlDao.deleteById(id);
        }
    }
    