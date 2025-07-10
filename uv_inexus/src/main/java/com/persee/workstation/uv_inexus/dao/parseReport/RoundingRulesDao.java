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
import com.persee.workstation.uv_inexus.po.parseReport.RoundingRules; // 修改了导入的PO类

@Mapper // MyBatis注解，标记这是一个MyBatis的Mapper接口
public interface RoundingRulesDao { // 修改了接口名

    // 查询所有
    @Select("SELECT * FROM t_report_rounding_rules") // 修改了表名
    List<RoundingRules> findAll(); // 修改了返回类型

    // 根据ID查询
    @Select("SELECT * FROM t_report_rounding_rules WHERE id = #{id}") // 修改了表名
    RoundingRules findById(Integer id); // 修改了返回类型

    // 插入新记录
    @Insert("INSERT INTO t_report_rounding_rules (detection_id, result_identity, rounding_type, rounding_condition, decimal_places) " + // 修改了表名和列名
            "VALUES (#{detectionId}, #{resultIdentity}, #{roundingType}, #{roundingCondition}, #{decimalPlaces})") // 修改了参数名
    void insert(RoundingRules roundingRules); // 修改了参数类型和变量名

    // 更新记录
    @Update("UPDATE t_report_rounding_rules SET detection_id=#{detectionId}, result_identity=#{resultIdentity}, " + // 修改了表名和列名
            "rounding_type=#{roundingType}, rounding_condition=#{roundingCondition}, decimal_places=#{decimalPlaces} " + // 修改了列名和参数名
            "WHERE id = #{id}") // 修改了表名
    void update(RoundingRules roundingRules); // 修改了参数类型和变量名

    // 根据ID删除记录
    @Delete("DELETE FROM t_report_rounding_rules WHERE id = #{id}") // 修改了表名
    void deleteById(Integer id);

    
    // 分页查询
    @Select("<script>" +
        "SELECT * FROM t_report_quality_control " +
        "<where>" +
        "${ew.sqlSegment}" +
        "</where>" +
        "ORDER BY id DESC" +  // 固定倒序
        "</script>")
    IPage<RoundingRules> selectPage(Page<RoundingRules> page, @Param("ew") QueryWrapper<RoundingRules> queryWrapper);
}
