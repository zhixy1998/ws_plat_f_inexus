package com.persee.workstation.uv_inexus.dao.parseReport;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.persee.workstation.uv_inexus.po.parseReport.StandardPretreatment;

@Mapper
public interface StandardPretreatmentMapper {

    @Insert("INSERT INTO t_report_standard_pretreatment (detection_id, pretreatment_description) " +
            "VALUES (#{detectionId}, #{pretreatmentDescription})")
    void insert(StandardPretreatment standardPretreatment);

    // 批量插入（保持原有定义）
    void batchInsert(@Param("list") List<StandardPretreatment> list);

    // 查询所有记录（保持原有逻辑）
    @Select("SELECT * FROM t_report_standard_pretreatment")
    List<StandardPretreatment> selectAll();

    // 检查记录是否存在（根据Detection ID，保持原有逻辑）
    @Select("SELECT COUNT(*) FROM t_report_standard_pretreatment WHERE detection_id = #{detectionId}")
    boolean existsByDetectionId(Integer detectionId);
    
    // 根据Detection ID更新记录（保持原有逻辑）
    @Update("UPDATE t_report_standard_pretreatment SET pretreatment_description = #{pretreatmentDescription}, " +
            "WHERE detection_id = #{detectionId}")
    void updateByDetectionId(StandardPretreatment pretreatment);

    // 联表查询（导出用，保持原有逻辑）
    @Select("SELECT " +
            "p.id, " +
            "d.standard_identity AS standardIdentity," +
            "p.pretreatment_description " +
            "FROM t_report_standard_pretreatment p " +
            "LEFT JOIN t_report_standard_detection d " +
            "ON p.detection_id = d.id")
    List<StandardPretreatment> selectAllForExport();

    // 新增：查询某主表ID下的所有前处理记录（用于哈希对比）
    @Select("SELECT * FROM t_report_standard_pretreatment WHERE detection_id = #{detectionId}")
    List<StandardPretreatment> selectByDetectionId(String detectionId);

    // 新增：根据ID精准更新单条记录（用于哈希对比后的更新）
    @Update("UPDATE t_report_standard_pretreatment SET " +
            "pretreatment_description = #{pretreatmentDescription}" +
            "WHERE id = #{id}")
    void updateById(StandardPretreatment pretreatment);
}