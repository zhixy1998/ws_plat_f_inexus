package com.persee.workstation.uv_inexus.dao.parseReport;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.persee.workstation.uv_inexus.po.parseReport.CalculationFormula;

@Mapper
public interface CalculationFormulaMapper {

    // 插入单条记录
    @Insert("INSERT INTO t_report_calculation_formulas " +
            "(standard_identity, result_identity, result_name, unit, " +
            "input_param1_name, input_param1_identity, input_param2_name, input_param2_identity, " +
            "input_param3_name, input_param3_identity, input_param4_name, input_param4_identity, " +
            "input_param5_name, input_param5_identity, input_param6_name, input_param6_identity, " +
            "input_param7_name, input_param7_identity, input_param8_name, input_param8_identity, " +
            "input_param9_name, input_param9_identity, input_param10_name, input_param10_identity, " +
            "formula_text, formula_image, detection_limit) " +
            "VALUES " +
            "(#{standardIdentity}, #{resultIdentity}, #{resultName}, #{unit}, " +
            "#{inputParam1Name}, #{inputParam1Identity}, #{inputParam2Name}, #{inputParam2Identity}, " +
            "#{inputParam3Name}, #{inputParam3Identity}, #{inputParam4Name}, #{inputParam4Identity}, " +
            "#{inputParam5Name}, #{inputParam5Identity}, #{inputParam6Name}, #{inputParam6Identity}, " +
            "#{inputParam7Name}, #{inputParam7Identity}, #{inputParam8Name}, #{inputParam8Identity}, " +
            "#{inputParam9Name}, #{inputParam9Identity}, #{inputParam10Name}, #{inputParam10Identity}, " +
            "#{formulaText}, #{formulaImage}, #{detectionLimit})")
    void insert(CalculationFormula calculationFormula);

    // 批量插入记录（可选，如果数据量大建议使用）
    @Insert("<script>" +
            "INSERT INTO t_report_calculation_formulas " +
            "(standard_identity, result_identity, result_name, unit, " +
            "input_param1_name, input_param1_identity, input_param2_name, input_param2_identity, " +
            "input_param3_name, input_param3_identity, input_param4_name, input_param4_identity, " +
            "input_param5_name, input_param5_identity, input_param6_name, input_param6_identity, " +
            "input_param7_name, input_param7_identity, input_param8_name, input_param8_identity, " +
            "input_param9_name, input_param9_identity, input_param10_name, input_param10_identity, " +
            "formula_text, formula_image, detection_limit) " +
            "VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.standardIdentity}, #{item.resultIdentity}, #{item.resultName}, #{item.unit}, " +
            "#{item.inputParam1Name}, #{item.inputParam1Identity}, #{item.inputParam2Name}, #{item.inputParam2Identity}, " +
            "#{item.inputParam3Name}, #{item.inputParam3Identity}, #{item.inputParam4Name}, #{item.inputParam4Identity}, " +
            "#{item.inputParam5Name}, #{item.inputParam5Identity}, #{item.inputParam6Name}, #{item.inputParam6Identity}, " +
            "#{item.inputParam7Name}, #{item.inputParam7Identity}, #{item.inputParam8Name}, #{item.inputParam8Identity}, " +
            "#{item.inputParam9Name}, #{item.inputParam9Identity}, #{item.inputParam10Name}, #{item.inputParam10Identity}, " +
            "#{item.formulaText}, #{item.formulaImage}, #{item.detectionLimit})" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("list") List<CalculationFormula> list);

    // 手动实现 selectList 方法
    @Select("SELECT * FROM t_report_calculation_formulas")
    List<CalculationFormula> selectAll();
}
