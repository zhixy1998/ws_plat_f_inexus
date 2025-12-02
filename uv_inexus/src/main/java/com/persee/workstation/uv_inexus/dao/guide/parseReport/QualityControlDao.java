/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dao.guide.parseReport;

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
import com.persee.workstation.uv_inexus.po.guide.parseReport.QualityControl;

@Mapper
public interface QualityControlDao {

    // 原有方法保持不变...
    @Select("SELECT * FROM t_report_quality_control")
    List<QualityControl> findAll();

    @Select("SELECT * FROM t_report_quality_control WHERE id = #{id}")
    QualityControl findById(Integer id);

    @Insert("INSERT INTO t_report_quality_control " +
            "(detection_id, control_type, control_description, control_formula, create_datetime, update_datetime) " +
            "VALUES " +
            "(#{detectionId}, #{controlType}, #{controlDescription}, #{controlFormula}, #{createDatetime}, #{updateDatetime})")
    void insert(QualityControl qualityControl);

    @Update("UPDATE t_report_quality_control SET " +
            "detection_id = #{detectionId}, " +
            "control_type = #{controlType}, " +
            "control_description = #{controlDescription}, " +
            "control_formula = #{controlFormula}, " +
            "update_datetime = #{updateDatetime} " +
            "WHERE id = #{id}")
    void update(QualityControl qualityControl);

    @Delete("DELETE FROM t_report_quality_control WHERE id = #{id}")
    void deleteById(Integer id);

    @Select("<script>" +
            "SELECT * FROM t_report_quality_control " +
            "<where>" +
            "${ew.sqlSegment}" +
            "</where>" +
            "ORDER BY id DESC" +
            "</script>")
    IPage<QualityControl> selectPage(Page<QualityControl> page, @Param("ew") QueryWrapper<QualityControl> queryWrapper);

    // 新增：根据detection_id查询（关键）
    @Select("SELECT * FROM t_report_quality_control WHERE detection_id = #{detectionId}")
    List<QualityControl> findByDetectionId(@Param("detectionId") String detectionId);

    @Select("SELECT * FROM t_report_quality_control WHERE detection_id = #{detectionId}")
    List<QualityControl> findByDetectionIdList(@Param("detectionId") String detectionId);
}