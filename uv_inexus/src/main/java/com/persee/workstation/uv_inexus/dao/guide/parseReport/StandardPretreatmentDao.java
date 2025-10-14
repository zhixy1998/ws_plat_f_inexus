/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
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
import com.persee.workstation.uv_inexus.po.guide.parseReport.StandardPretreatment;

@Mapper
public interface StandardPretreatmentDao {

    // 原有方法保持不变...
    @Select("SELECT * FROM t_report_standard_pretreatment")
    List<StandardPretreatment> findAll();

    @Select("SELECT * FROM t_report_standard_pretreatment WHERE id = #{id}")
    StandardPretreatment findById(Integer id);

    @Insert("INSERT INTO t_report_standard_pretreatment (detection_id, pretreatment_description) " +
            "VALUES (#{detectionId}, #{pretreatmentDescription})")
    void insert(StandardPretreatment standardPretreatment);

    @Update("UPDATE t_report_standard_pretreatment SET detection_id=#{detectionId}, pretreatment_description=#{pretreatmentDescription} WHERE id = #{id}")
    void update(StandardPretreatment standardPretreatment);

    @Delete("DELETE FROM t_report_standard_pretreatment WHERE id = #{id}")
    void deleteById(Integer id);

    @Select("<script>" +
            "SELECT * FROM t_report_standard_pretreatment " +
            "<where>" +
            "${ew.sqlSegment}" +
            "</where>" +
            "ORDER BY id DESC" +
            "</script>")
    IPage<StandardPretreatment> selectPage(Page<StandardPretreatment> page, @Param("ew") QueryWrapper<StandardPretreatment> queryWrapper);

    // 新增：根据detection_id查询（关键）
    @Select("SELECT * FROM t_report_standard_pretreatment WHERE detection_id = #{detectionId}")
    List<StandardPretreatment> findByDetectionId(@Param("detectionId") String detectionId);

    @Select("SELECT * FROM t_report_standard_pretreatment WHERE detection_id = #{detectionId}")
    List<StandardPretreatment> findByDetectionIdList(@Param("detectionId") String detectionId);
}