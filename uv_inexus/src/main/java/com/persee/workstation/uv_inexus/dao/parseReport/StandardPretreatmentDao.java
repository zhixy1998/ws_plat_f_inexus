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
import com.persee.workstation.uv_inexus.po.parseReport.StandardPretreatment;

/**
 * 标准前处理表 DAO 接口
 */
@Mapper // MyBatis注解，标记这是一个MyBatis的Mapper接口
public interface StandardPretreatmentDao {

    // 查询所有
    @Select("SELECT * FROM t_report_standard_pretreatment")
    List<StandardPretreatment> findAll();

    // 根据ID查询
    @Select("SELECT * FROM t_report_standard_pretreatment WHERE id = #{id}")
    StandardPretreatment findById(Integer id);

    // 插入新记录
    @Insert("INSERT INTO t_report_standard_pretreatment (standard_identity, pretreatment_description) " +
            "VALUES (#{standardIdentity}, #{pretreatmentDescription})")
    void insert(StandardPretreatment standardPretreatment);

    // 更新记录
    @Update("UPDATE t_report_standard_pretreatment SET standard_identity=#{standardIdentity}, pretreatment_description=#{pretreatmentDescription} WHERE id = #{id}")

    void update(StandardPretreatment standardPretreatment);

    // 根据ID删除记录
    @Delete("DELETE FROM t_report_standard_pretreatment WHERE id = #{id}")
    void deleteById(Integer id);

    // 分页查询
    @Select("<script>" +
        "SELECT * FROM t_report_standard_pretreatment " +
        "<where>" +
        "${ew.sqlSegment}" +
        "</where>" +
        "ORDER BY id DESC" +  // 固定倒序
        "</script>")
    IPage<StandardPretreatment> selectPage(Page<StandardPretreatment> page, @Param("ew") QueryWrapper<StandardPretreatment> queryWrapper);
}
