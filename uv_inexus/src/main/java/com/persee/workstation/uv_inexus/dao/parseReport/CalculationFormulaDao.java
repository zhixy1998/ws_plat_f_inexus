package com.persee.workstation.uv_inexus.dao.parseReport;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.po.parseReport.CalculationFormula;

@Mapper
public interface CalculationFormulaDao {

    // 原有方法保持不变...
    @Select("SELECT * FROM t_report_calculation_formulas")
    List<CalculationFormula> findAll();

    @Select("SELECT * FROM t_report_calculation_formulas WHERE id = #{id}")
    CalculationFormula findById(Integer id);

    @Insert("INSERT INTO t_report_calculation_formulas (" +
            "standard_identity, result_identity, result_name, unit, input_param1_name, input_param1_identity, " +
            "input_param2_name, input_param2_identity, input_param3_name, input_param3_identity, input_param4_name, " +
            "input_param4_identity, input_param5_name, input_param5_identity, input_param6_name, input_param6_identity, " +
            "input_param7_name, input_param7_identity, input_param8_name, input_param8_identity, input_param9_name, " +
            "input_param9_identity, input_param10_name, input_param10_identity, formula_text, formula_image, detection_limit" +
            ") VALUES (" +
            "#{standardIdentity}, #{resultIdentity}, #{resultName}, #{unit}, #{inputParam1Name}, #{inputParam1Identity}, " +
            "#{inputParam2Name}, #{inputParam2Identity}, #{inputParam3Name}, #{inputParam3Identity}, #{inputParam4Name}, " +
            "#{inputParam4Identity}, #{inputParam5Name}, #{inputParam5Identity}, #{inputParam6Name}, #{inputParam6Identity}, " +
            "#{inputParam7Name}, #{inputParam7Identity}, #{inputParam8Name}, #{inputParam8Identity}, #{inputParam9Name}, " +
            "#{inputParam9Identity}, #{inputParam10Name}, #{inputParam10Identity}, #{formulaText}, #{formulaImage}, #{detectionLimit}" +
            ")")
    void insert(CalculationFormula calculationFormula);

    @Update("UPDATE t_report_calculation_formulas SET " +
            "standard_identity=#{standardIdentity}, result_identity=#{resultIdentity}, result_name=#{resultName}, " +
            "unit=#{unit}, input_param1_name=#{inputParam1Name}, input_param1_identity=#{inputParam1Identity}, " +
            "input_param2_name=#{inputParam2Name}, input_param2_identity=#{inputParam2Identity}, " +
            "input_param3_name=#{inputParam3Name}, input_param3_identity=#{inputParam3Identity}, " +
            "input_param4_name=#{inputParam4Name}, input_param4_identity=#{inputParam4Identity}, " +
            "input_param5_name=#{inputParam5Name}, input_param5_identity=#{inputParam5Identity}, " +
            "input_param6_name=#{inputParam6Name}, input_param6_identity=#{inputParam6Identity}, " +
            "input_param7_name=#{inputParam7Name}, input_param7_identity=#{inputParam7Identity}, " +
            "input_param8_name=#{inputParam8Name}, input_param8_identity=#{inputParam8Identity}, " +
            "input_param9_name=#{inputParam9Name}, input_param9_identity=#{inputParam9Identity}, " +
            "input_param10_name=#{inputParam10Name}, input_param10_identity=#{inputParam10Identity}, " +
            "formula_text=#{formulaText}, formula_image=#{formulaImage}, detection_limit=#{detectionLimit} " +
            "WHERE id = #{id}")
    void update(CalculationFormula calculationFormula);

    @Delete("DELETE FROM t_report_calculation_formulas WHERE id = #{id}")
    void deleteById(Integer id);

    @Select("<script>" +
            "SELECT * FROM t_report_calculation_formulas " +
            "<where>" +
            "${ew.sqlSegment}" +
            "</where>" +
            "ORDER BY id DESC" +
            "</script>")
    IPage<CalculationFormula> selectPage(Page<CalculationFormula> page, @Param("ew") QueryWrapper<CalculationFormula> queryWrapper);

    // 新增：根据standard_identity查询（关键）
    @Select("SELECT * FROM t_report_calculation_formulas WHERE standard_identity = #{standardIdentity} LIMIT 1")
    CalculationFormula findByStandardIdentity(String standardIdentity);
}