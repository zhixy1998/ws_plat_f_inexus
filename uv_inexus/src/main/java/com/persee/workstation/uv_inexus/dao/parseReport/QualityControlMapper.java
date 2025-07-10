package com.persee.workstation.uv_inexus.dao.parseReport;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.persee.workstation.uv_inexus.po.parseReport.QualityControl;

@Mapper
public interface QualityControlMapper {

    // 检查指定 detectionId 是否已存在
    @Select("SELECT COUNT(*) FROM t_report_quality_control WHERE detection_id = #{detectionId}")
    boolean existsBydetectionId(@Param("detectionId") String detectionId);

    // 使用 INSERT ... ON DUPLICATE KEY UPDATE 实现 Upsert
    @Insert("INSERT INTO t_report_quality_control (detection_id, control_type, control_description, control_formula) " +
            "VALUES (#{detectionId}, #{controlType}, #{controlDescription}, #{controlFormula}) " +
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
            "WHERE detection_id = #{detectionId}")
    void updateByDetectionId(QualityControl qualityControl);

    // 新增：联表查询，转换 detection_id 为文字，直接返回Excel模型类
    @Select("SELECT " +
        "q.id, " +
        "d.standard_identity AS standardIdentity, " +
        "q.control_type AS controlType, " +
        "q.control_description AS controlDescription, " +
        "q.control_formula AS controlFormula " +  
        "FROM t_report_quality_control q " +
        "LEFT JOIN t_report_standard_detection d " +
        "ON q.detection_id = d.id")
        @Results({
        @Result(column = "standardIdentity", property = "detectionId"), // 将standard_identity映射到detectionId字段
        })
        List<QualityControl> selectAllForExport();

        // 新增：查询某主表ID下的所有质控记录
    @Select("SELECT * FROM t_report_quality_control WHERE detection_id = #{detectionId}")
    List<QualityControl> selectByDetectionId(String detectionId);

    // 新增：根据ID更新
    @Update("UPDATE t_report_quality_control SET " +
            "control_type = #{controlType}, " +
            "control_description = #{controlDescription}, " +
            "control_formula = #{controlFormula} " +
            "WHERE id = #{id}")
    void updateById(QualityControl qualityControl);
}