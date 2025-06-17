    package com.persee.workstation.uv_inexus.service.parseReport;

    import com.persee.workstation.uv_inexus.po.parseReport.QualityControl;
    import java.util.List;

    public interface QualityControlService {

        List<QualityControl> findAll();

        QualityControl findById(Integer id);

        void insert(QualityControl QualityControl);

        void update(QualityControl QualityControl);

        void deleteById(Integer id);
    }
    