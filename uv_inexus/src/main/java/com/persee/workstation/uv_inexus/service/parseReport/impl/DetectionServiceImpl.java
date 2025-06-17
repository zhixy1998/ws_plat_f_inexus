    package com.persee.workstation.uv_inexus.service.parseReport.impl;

    import com.persee.workstation.uv_inexus.dao.parseReport.DetectionDao;
    import com.persee.workstation.uv_inexus.po.parseReport.Detection;
    import com.persee.workstation.uv_inexus.service.parseReport.DetectionService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    import java.util.List;

    @Service // Spring注解，标记这是一个服务组件，会被Spring管理
    public class DetectionServiceImpl implements DetectionService {

        @Autowired // Spring注解，自动注入DetectionDao的实例
        private DetectionDao detectionDao;

        @Override
        public List<Detection> findAll() {
            return detectionDao.findAll();
        }

        @Override
        public Detection findById(Integer id) {
            return detectionDao.findById(id);
        }

        @Override
        public void insert(Detection detection) {
            detectionDao.insert(detection);
        }

        @Override
        public void update(Detection detection) {
            detectionDao.update(detection);
        }

        @Override
        public void deleteById(Integer id) {
            detectionDao.deleteById(id);
        }
    }
    