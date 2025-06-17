package com.persee.workstation.uv_inexus.dao.parseReport;

import com.persee.workstation.uv_inexus.po.parseReport.QualityControl;
import org.apache.ibatis.annotations.*;
// import org.apache.ibatis.annotations.Mapper;
// import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QualityControlMapper {

    @Insert("INSERT INTO t_report_quality_control (standard_identity, control_type, control_description, control_formula) " +
            "VALUES (#{standardIdentity}, #{controlType}, #{controlDescription}, #{controlFormula})")
    void insert(QualityControl qualityControl);

    // 如果需要批量插入，可以添加以下方法
    void batchInsert(@Param("list") List<QualityControl> list);

    // 手动实现 selectList 方法
    @Select("SELECT * FROM t_report_quality_control")
    List<QualityControl> selectAll();
}
