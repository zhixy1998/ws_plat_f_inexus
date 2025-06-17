    package com.persee.workstation.uv_inexus.service.parseReport;

    import com.persee.workstation.uv_inexus.po.parseReport.Detection;
    import java.util.List;

    public interface DetectionService {

        List<Detection> findAll();

        Detection findById(Integer id);

        void insert(Detection detection);

        void update(Detection detection);

        void deleteById(Integer id);
    }
    