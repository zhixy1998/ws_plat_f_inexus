package com.persee.workstation.uv_inexus.dao.parseReport;

import com.persee.workstation.uv_inexus.po.parseReport.RoundingRules; // 修改了导入的PO类
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import java.util.List;

@Mapper // MyBatis注解，标记这是一个MyBatis的Mapper接口
public interface RoundingRulesDao { // 修改了接口名

    // 查询所有
    @Select("SELECT * FROM t_report_rounding_rules") // 修改了表名
    List<RoundingRules> findAll(); // 修改了返回类型

    // 根据ID查询
    @Select("SELECT * FROM t_report_rounding_rules WHERE id = #{id}") // 修改了表名
    RoundingRules findById(Integer id); // 修改了返回类型

    // 插入新记录
    @Insert("INSERT INTO t_report_rounding_rules (standard_identity, result_identity, rounding_type, rounding_condition, decimal_places) " + // 修改了表名和列名
            "VALUES (#{standardIdentity}, #{resultIdentity}, #{roundingType}, #{roundingCondition}, #{decimalPlaces})") // 修改了参数名
    void insert(RoundingRules roundingRules); // 修改了参数类型和变量名

    // 更新记录
    @Update("UPDATE t_report_rounding_rules SET standard_identity=#{standardIdentity}, result_identity=#{resultIdentity}, " + // 修改了表名和列名
            "rounding_type=#{roundingType}, rounding_condition=#{roundingCondition}, decimal_places=#{decimalPlaces} " + // 修改了列名和参数名
            "WHERE id = #{id}") // 修改了表名
    void update(RoundingRules roundingRules); // 修改了参数类型和变量名

    // 根据ID删除记录
    @Delete("DELETE FROM t_report_rounding_rules WHERE id = #{id}") // 修改了表名
    void deleteById(Integer id);
}
