    package com.persee.workstation.uv_inexus.service.parseReport;

    import com.persee.workstation.uv_inexus.po.parseReport.RoundingRules;
    import java.util.List;

    public interface RoundingRulesService {

        List<RoundingRules> findAll();

        RoundingRules findById(Integer id);

        void insert(RoundingRules RoundingRules);

        void update(RoundingRules RoundingRules);

        void deleteById(Integer id);
    }
    