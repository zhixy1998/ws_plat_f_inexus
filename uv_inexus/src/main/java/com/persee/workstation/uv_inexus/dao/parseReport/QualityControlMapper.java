package com.persee.workstation.uv_inexus.dao.parseReport;

import java.util.List;

// import org.apache.ibatis.annotations.Mapper;
// import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.persee.workstation.uv_inexus.po.parseReport.QualityControl;

@Mapper
public interface QualityControlMapper {

    // 检查指定 standardIdentity 是否已存在
    @Select("SELECT COUNT(*) FROM t_report_quality_control WHERE standard_identity = #{standardIdentity}")
    boolean existsByStandardIdentity(@Param("standardIdentity") String standardIdentity);

    // 使用 INSERT ... ON DUPLICATE KEY UPDATE 实现 Upsert
    @Insert("INSERT INTO t_report_quality_control (standard_identity, control_type, control_description, control_formula) " +
            "VALUES (#{standardIdentity}, #{controlType}, #{controlDescription}, #{controlFormula}) " +
            "ON DUPLICATE KEY UPDATE " +
            "control_type = #{controlType}, " +
            "control_description = #{controlDescription}, " +
            "control_formula = #{controlFormula}")
    void insert(QualityControl qualityControl); // 命名更清晰地表示其功能

    // 如果需要批量插入/更新，可以添加以下方法
    // void batchSave(@Param("list") List<QualityControl> list); // 需要额外实现

    // 手动实现 selectList 方法
    @Select("SELECT * FROM t_report_quality_control")
    List<QualityControl> selectAll();

    // 根据标准标识更新记录 (如果使用分开的 insert 和 update)
    @Update("UPDATE t_report_quality_control " +
            "SET control_type = #{controlType}, " +
            "control_description = #{controlDescription}, " +
            "control_formula = #{controlFormula} " +
            "WHERE standard_identity = #{standardIdentity}")
    void updateByStandardIdentity(QualityControl qualityControl);
}
