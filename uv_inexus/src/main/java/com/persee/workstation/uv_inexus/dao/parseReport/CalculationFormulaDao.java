package com.persee.workstation.uv_inexus.dao.parseReport;

import com.persee.workstation.uv_inexus.po.parseReport.CalculationFormula;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import java.util.List;

@Mapper // MyBatis注解，标记这是一个MyBatis的Mapper接口
public interface CalculationFormulaDao {

    // 查询所有
    @Select("SELECT * FROM t_report_calculation_formulas")
    List<CalculationFormula> findAll();

    // 根据ID查询
    @Select("SELECT * FROM t_report_calculation_formulas WHERE id = #{id}")
    CalculationFormula findById(Integer id);

    // 插入新记录
    @Insert("INSERT INTO t_report_calculation_formulas (standard_identity, result_identity, result_name, unit, input_param1_name, input_param1_identity, input_param2_name, input_param2_identity, input_param3_name, input_param3_identity, input_param4_name, input_param4_identity, input_param5_name, input_param5_identity, input_param6_name, input_param6_identity, input_param7_name, input_param7_identity, input_param8_name, input_param8_identity, input_param9_name, input_param9_identity, input_param10_name, input_param10_identity, formula_text, formula_image) " +
            "VALUES (#{standardIdentity}, #{resultIdentity}, #{resultName}, #{unit}, #{inputParam1Name}, #{inputParam1Identity}, #{inputParam2Name}, #{inputParam2Identity}, #{inputParam3Name}, #{inputParam3Identity}, #{inputParam4Name}, #{inputParam4Identity}, #{inputParam5Name}, #{inputParam5Identity}, #{inputParam6Name}, #{inputParam6Identity}, #{inputParam7Name}, #{inputParam7Identity}, #{inputParam8Name}, #{inputParam8Identity}, #{inputParam9Name}, #{inputParam9Identity}, #{inputParam10Name}, #{inputParam10Identity}, #{formulaText}, #{formulaImage})")
    void insert(CalculationFormula calculationFormula);

    // 更新记录
    @Update("UPDATE t_report_calculation_formulas SET " +
            "standard_identity=#{standardIdentity}, " +
            "result_identity=#{resultIdentity}, " +
            "result_name=#{resultName}, " +
            "unit=#{unit}, " +
            "input_param1_name=#{inputParam1Name}, " +
            "input_param1_identity=#{inputParam1Identity}, " +
            "input_param2_name=#{inputParam2Name}, " +
            "input_param2_identity=#{inputParam2Identity}, " +
            "input_param3_name=#{inputParam3Name}, " +
            "input_param3_identity=#{inputParam3Identity}, " +
            "input_param4_name=#{inputParam4Name}, " +
            "input_param4_identity=#{inputParam4Identity}, " +
            "input_param5_name=#{inputParam5Name}, " +
            "input_param5_identity=#{inputParam5Identity}, " +
            "input_param6_name=#{inputParam6Name}, " +
            "input_param6_identity=#{inputParam6Identity}, " +
            "input_param7_name=#{inputParam7Name}, " +
            "input_param7_identity=#{inputParam7Identity}, " +
            "input_param8_name=#{inputParam8Name}, " +
            "input_param8_identity=#{inputParam8Identity}, " +
            "input_param9_name=#{inputParam9Name}, " +
            "input_param9_identity=#{inputParam9Identity}, " +
            "input_param10_name=#{inputParam10Name}, " +
            "input_param10_identity=#{inputParam10Identity}, " +
            "formula_text=#{formulaText}, " +
            "formula_image=#{formulaImage} " +
            "WHERE id = #{id}")
    void update(CalculationFormula calculationFormula);

    // 根据ID删除记录
    @Delete("DELETE FROM t_report_calculation_formulas WHERE id = #{id}")
    void deleteById(Integer id);
}
