package com.persee.workstation.uv_inexus.dao.parseReport;

import com.persee.workstation.uv_inexus.po.parseReport.RoundingRules;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RoundingRulesMapper {

    // 插入单条记录
    @Insert("INSERT INTO t_report_rounding_rules (result_identity, standard_identity, rounding_type, rounding_condition, decimal_places) " +
            "VALUES (#{resultIdentity}, #{standardIdentity}, #{roundingType}, #{roundingCondition}, #{decimalPlaces})")
    void insert(RoundingRules roundingRules);

    // 批量插入记录（可选，如果数据量大建议使用）
    void batchInsert(@Param("list") List<RoundingRules> list);

    // 根据主键查询记录
    @Select("SELECT * FROM t_report_rounding_rules")
    List<RoundingRules> selectAll();

}
