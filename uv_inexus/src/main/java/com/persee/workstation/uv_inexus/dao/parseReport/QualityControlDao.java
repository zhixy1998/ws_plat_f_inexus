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
import com.persee.workstation.uv_inexus.po.parseReport.QualityControl; // 替换为你的 QualityControl 实体类路径

@Mapper // MyBatis注解，标记这是一个MyBatis的Mapper接口
public interface QualityControlDao {

    // 查询所有
    @Select("SELECT * FROM t_report_quality_control") // 使用实体类上的 @TableName 指定的表名
    List<QualityControl> findAll();

    // 根据ID查询
    @Select("SELECT * FROM t_report_quality_control WHERE id = #{id}") // 使用 #{id} 占位符，MyBatis 会处理类型转换
    QualityControl findById(Integer id);

    // 插入记录
    @Insert("INSERT INTO t_report_quality_control " +
            "(standard_identity, control_type, control_description, control_formula, create_datetime, update_datetime) " +
            "VALUES " +
            "(#{standardIdentity}, #{controlType}, #{controlDescription}, #{controlFormula}, #{createDatetime}, #{updateDatetime})")
    // 确保列名与 @TableField 指定的值一致，变量名与实体类中的字段名一致
    void insert(QualityControl qualityControl); // 参数类型改为 QualityControl

    // 更新记录
    @Update("UPDATE t_report_quality_control SET " +
            "standard_identity = #{standardIdentity}, " +
            "control_type = #{controlType}, " +
            "control_description = #{controlDescription}, " +
            "control_formula = #{controlFormula}, " +
            "update_datetime = #{updateDatetime} " + // 假设 updateDatetime 需要更新
            "WHERE id = #{id}")
    // 确保列名与 @TableField 指定的值一致，变量名与实体类中的字段名一致
    void update(QualityControl qualityControl); // 参数类型改为 QualityControl

    // 根据ID删除记录
    @Delete("DELETE FROM t_report_quality_control WHERE id = #{id}") // 使用 #{id} 占位符
    void deleteById(Integer id);

    // 分页查询
    @Select("<script>" +
        "SELECT * FROM t_report_quality_control " +
        "<where>" +
        "${ew.sqlSegment}" +
        "</where>" +
        "ORDER BY id DESC" +  // 固定倒序
        "</script>")
    IPage<QualityControl> selectPage(Page<QualityControl> page, @Param("ew") QueryWrapper<QualityControl> queryWrapper);
}
