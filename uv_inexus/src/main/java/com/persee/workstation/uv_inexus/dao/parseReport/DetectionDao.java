package com.persee.workstation.uv_inexus.dao.parseReport;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionListDTO;
import com.persee.workstation.uv_inexus.po.parseReport.Detection;

@Mapper
public interface DetectionDao {

    @Select("SELECT * FROM t_report_standard_detection")
    List<Detection> findAll();

    @Select("SELECT * FROM t_report_standard_detection WHERE id = #{id}")
    Detection findById(Integer id);

    @Insert("INSERT INTO t_report_standard_detection (" +
            "standard_identity, industry, detection_type, detection_instrument, standard_number, " +
            "standard_name, detection_item, method_number, remark, detection_parameters, " +
            "release_date, expiration_date, effective_date, release_status, industry_type" +
            ") VALUES (" +
            "#{standardIdentity}, #{industry}, #{detectionType}, #{detectionInstrument}, #{standardNumber}, " +
            "#{standardName}, #{detectionItem}, #{methodNumber}, #{remark}, #{detectionParameters}, " +
            "#{releaseDate}, #{expirationDate}, #{effectiveDate}, #{releaseStatus}, #{industryType}" +
            ")")
    void insert(Detection detection);

    @Update("UPDATE t_report_standard_detection SET " +
            "standard_identity=#{standardIdentity}, industry=#{industry}, detection_type=#{detectionType}, " +
            "detection_instrument=#{detectionInstrument}, standard_number=#{standardNumber}, " +
            "standard_name=#{standardName}, detection_item=#{detectionItem}, method_number=#{methodNumber}, " +
            "remark=#{remark}, detection_parameters=#{detectionParameters}, " +
            "release_date=#{releaseDate}, expiration_date=#{expirationDate}, effective_date=#{effectiveDate}, " +
            "release_status=#{releaseStatus}, industry_type=#{industryType} " +
            "WHERE id = #{id}")
    void update(Detection detection);

    @Delete("DELETE FROM t_report_standard_detection WHERE id = #{id}")
    void deleteById(Integer id);

    @Select("<script>" +
            "SELECT * FROM t_report_standard_detection " +
            "<where>" +
            "${ew.sqlSegment}" +
            "</where>" +
            "ORDER BY id DESC" +
            "</script>")
    IPage<Detection> selectPage(Page<Detection> page, @Param("ew") QueryWrapper<Detection> queryWrapper);

    @Select("SELECT industry_type AS name, COUNT(*) AS num FROM t_report_standard_detection GROUP BY industry_type")
    List<Map<String, Object>> countIndustryType();

    @Select("SELECT industry AS name, COUNT(*) AS num FROM t_report_standard_detection GROUP BY industry")
    List<Map<String, Object>> countIndustry();

    @Delete("<script>" +
            "DELETE FROM t_report_standard_detection WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int deleteBatchIds(@Param("ids") List<Integer> ids);

    // 新增：去重分页查询
    @Select("<script>" +
        "SELECT " +
        "sub.industry, " +
        "sub.standardName, " +
        "sub.standardNumber, " +
        "sub.releaseDate, " +
        "sub.expirationDate, " +
        "sub.effectiveDate, " +
        "sub.industryType, " +
        "sub.count " +
        "FROM ( " +
        "    SELECT " +
        "    MAX(id) AS id, " +  // 取每组最大ID（最新记录）
        "    MAX(industry) AS industry, " +  // 用聚合函数包裹非分组字段
        "    MAX(standard_name) AS standardName, " +
        "    standard_number AS standardNumber, " +  // 分组字段
        "    MAX(release_date) AS releaseDate, " +
        "    MAX(expiration_date) AS expirationDate, " +
        "    MAX(effective_date) AS effectiveDate, " +
        "    MAX(industry_type) AS industryType, " +
        "    COUNT(*) AS count " +  // 统计出现次数
        "    FROM t_report_standard_detection " +
        "    <where> ${ew.sqlSegment} </where> " +
        "    GROUP BY standard_number " +  // 仅按标准号去重
        ") sub " +
        "ORDER BY sub.id DESC" +  // 按最新记录排序
        "</script>")
IPage<DetectionListDTO> selectDistinctPage(
        Page<DetectionListDTO> page,
        @Param("ew") QueryWrapper<Detection> queryWrapper
);
}