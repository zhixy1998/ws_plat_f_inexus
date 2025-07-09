package com.persee.workstation.uv_inexus.dao.parseReport;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.persee.workstation.uv_inexus.po.parseReport.RoundingRules;

@Mapper
public interface RoundingRulesMapper {

    // 检查记录是否存在（根据 standard_identity）
    @Select("SELECT COUNT(*) FROM t_report_rounding_rules WHERE standard_identity = #{standardIdentity}")
    boolean existsByStandardIdentity(@Param("standardIdentity") String standardIdentity);

    // 根据 standard_identity 更新记录
    @Update("UPDATE t_report_rounding_rules SET " +
            "rounding_type = #{roundingType}, " +
            "rounding_condition = #{roundingCondition}, " +
            "decimal_places = #{decimalPlaces} " +
            "WHERE standard_identity = #{standardIdentity}" )
    void updateByStandardIdentity(RoundingRules roundingRules);

    // 插入单条记录
    @Insert("INSERT INTO t_report_rounding_rules (result_identity, standard_identity, rounding_type, rounding_condition, decimal_places) " +
            "VALUES (#{resultIdentity}, #{standardIdentity}, #{roundingType}, #{roundingCondition}, #{decimalPlaces})")
    void insert(RoundingRules roundingRules);

    // 批量插入记录
    @Insert({
            "<script>",
            "INSERT INTO t_report_rounding_rules (result_identity, standard_identity, rounding_type, rounding_condition, decimal_places)",
            "VALUES",
            "<foreach collection='list' item='item' separator=','>",
            "    (#{item.resultIdentity}, #{item.standardIdentity}, #{item.roundingType}, #{item.roundingCondition}, #{item.decimalPlaces})",
            "</foreach>",
            "</script>"
    })
    void batchInsert(@Param("list") List<RoundingRules> list);

    // 查询所有记录
    @Select("SELECT * FROM t_report_rounding_rules")
    List<RoundingRules> selectAll();

    // 联表查询（导出用）
    @Select("SELECT " +
            "r.id, " +
            "r.result_identity AS resultIdentity, " +
            "d.standard_identity AS standardIdentity," +
            "r.rounding_type AS roundingType, " +
            "r.rounding_condition AS roundingCondition," +
            "r.decimal_places AS decimalPlaces " +
            "FROM t_report_rounding_rules r " +
            "LEFT JOIN t_report_standard_detection d " +  
            "ON r.standard_identity = d.id")  
    List<RoundingRules> selectAllForExport();

    // 新增：查询某主表ID下的所有修约记录
    @Select("SELECT * FROM t_report_rounding_rules WHERE standard_identity = #{standardIdentity}")
    List<RoundingRules> selectByStandardIdentity(String standardIdentity);

    // 新增：根据ID更新修约记录
    @Update("UPDATE t_report_rounding_rules SET " +
            "result_identity = #{resultIdentity}, " +
            "rounding_type = #{roundingType}, " +
            "rounding_condition = #{roundingCondition}, " +
            "decimal_places = #{decimalPlaces} " +
            "WHERE id = #{id}")
    void updateById(RoundingRules roundingRules);
}