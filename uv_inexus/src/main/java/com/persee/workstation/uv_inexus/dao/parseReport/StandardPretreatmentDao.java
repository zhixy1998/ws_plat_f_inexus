package com.persee.workstation.uv_inexus.dao.parseReport;

import com.persee.workstation.uv_inexus.po.parseReport.StandardPretreatment;
import org.apache.ibatis.annotations.*;
import java.util.List;

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
}
