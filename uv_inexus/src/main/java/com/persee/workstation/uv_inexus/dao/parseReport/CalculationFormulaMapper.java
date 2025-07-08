package com.persee.workstation.uv_inexus.dao.parseReport;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.persee.workstation.uv_inexus.po.parseReport.CalculationFormula;

@Mapper
public interface CalculationFormulaMapper {

    // 检查记录是否存在（根据 standard_identity 和 result_identity）
    @Select("SELECT COUNT(*) FROM t_report_calculation_formulas WHERE standard_identity = #{standardIdentity}")
    boolean existsByStandardIdentity(@Param("standardIdentity") String standardIdentity);

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

    // 更新记录（根据 standard_identity 和 result_identity）
    @Update("UPDATE t_report_calculation_formulas SET " +
            "result_identity = #{resultIdentity},result_name = #{resultName}, unit = #{unit}, " +
            "input_param1_name = #{inputParam1Name}, input_param1_identity = #{inputParam1Identity}, " +
            "input_param2_name = #{inputParam2Name}, input_param2_identity = #{inputParam2Identity}, " +
            "input_param3_name = #{inputParam3Name}, input_param3_identity = #{inputParam3Identity}, " +
            "input_param4_name = #{inputParam4Name}, input_param4_identity = #{inputParam4Identity}, " +
            "input_param5_name = #{inputParam5Name}, input_param5_identity = #{inputParam5Identity}, " +
            "input_param6_name = #{inputParam6Name}, input_param6_identity = #{inputParam6Identity}, " +
            "input_param7_name = #{inputParam7Name}, input_param7_identity = #{inputParam7Identity}, " +
            "input_param8_name = #{inputParam8Name}, input_param8_identity = #{inputParam8Identity}, " +
            "input_param9_name = #{inputParam9Name}, input_param9_identity = #{inputParam9Identity}, " +
            "input_param10_name = #{inputParam10Name}, input_param10_identity = #{inputParam10Identity}, " +
            "formula_text = #{formulaText}, formula_image = #{formulaImage}, detection_limit = #{detectionLimit} " +
            "WHERE standard_identity = #{standardIdentity}")
    void updateByStandardIdentity(CalculationFormula calculationFormula);

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

    // 新增：联表查询，转换 standard_identity 为文字，直接返回Excel模型类
    @Select("SELECT " +
        "c.id, " +
        "d.standard_identity AS standardIdentity, " +
        "c.result_identity AS resultIdentity, " +
        "c.result_name AS resultName, " +
        "c.unit AS unit, " +
        "c.input_param1_name AS inputParam1Name, " +
        "c.input_param1_identity AS inputParam1Identity, " +
        "c.input_param2_name AS inputParam2Name, " +
        "c.input_param2_identity AS inputParam2Identity, " +
        "c.input_param3_name AS inputParam3Name, " +
        "c.input_param3_identity AS inputParam3Identity, " +
        "c.input_param4_name AS inputParam4Name, " +
        "c.input_param4_identity AS inputParam4Identity, " +
        "c.input_param5_name AS inputParam5Name, " +
        "c.input_param5_identity AS inputParam5Identity, " +
        "c.input_param6_name AS inputParam6Name, " +
        "c.input_param6_identity AS inputParam6Identity, " +
        "c.input_param7_name AS inputParam7Name, " +
        "c.input_param7_identity AS inputParam7Identity, " +
        "c.input_param8_name AS inputParam8Name, " +
        "c.input_param8_identity AS inputParam8Identity, " +
        "c.input_param9_name AS inputParam9Name, " +
        "c.input_param9_identity AS inputParam9Identity, " +
        "c.input_param10_name AS inputParam10Name, " +
        "c.input_param10_identity AS inputParam10Identity, " +
        "c.formula_text AS formulaText, " +
        "c.formula_image AS formulaImage, " +
        "c.detection_limit AS detectionLimit " +
        "FROM t_report_calculation_formulas c " +
        "LEFT JOIN t_report_standard_detection d " +
        "ON c.standard_identity = d.id")
    List<CalculationFormula> selectAllForExport();
}
