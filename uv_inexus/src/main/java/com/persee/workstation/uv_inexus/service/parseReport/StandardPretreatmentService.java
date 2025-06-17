    package com.persee.workstation.uv_inexus.service.parseReport;

    import com.persee.workstation.uv_inexus.po.parseReport.StandardPretreatment;
    import java.util.List;

    public interface StandardPretreatmentService {

        List<StandardPretreatment> findAll();

        StandardPretreatment findById(Integer id);

        void insert(StandardPretreatment standardPretreatment);

        void update(StandardPretreatment standardPretreatment);

        void deleteById(Integer id);
    }
    