package com.persee.workstation.uv_inexus.dao.parseReport;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.persee.workstation.uv_inexus.po.parseReport.Detection;

/**
 * 检测表DAO层，处理检测数据的CRUD
 */
@Mapper
public interface DetectionMapper {

    /**
     * 插入单条检测数据（标准标识字段存储的是ID）
     * @param detection 检测实体
     */
    @Insert("INSERT INTO t_report_standard_detection (" +
            "standard_identity, industry, detection_type, detection_instrument, " +
            "standard_number, standard_name, detection_item, method_number, remark, " +
            "detection_parameters, release_date, expiration_date, effective_date, " +
            "release_status, industry_type" +
            ") VALUES (" +
            "#{standardIdentity}, #{industry}, #{detectionType}, #{detectionInstrument}, " +
            "#{standardNumber}, #{standardName}, #{detectionItem}, #{methodNumber}, #{remark}, " +
            "#{detectionParameters}, #{releaseDate}, #{expirationDate}, #{effectiveDate}, " +
            "#{releaseStatus}, #{industryType}" +
            ")")
    void insert(Detection detection);

    /**
     * 批量插入检测数据（可选，用于优化大量数据导入）
     * @param list 检测数据列表
     */
    @Insert("<script>" +
            "INSERT INTO t_report_standard_detection (" +
            "standard_identity, industry, detection_type, detection_instrument, " +
            "standard_number, standard_name, detection_item, method_number, remark, " +
            "detection_parameters, release_date, expiration_date, effective_date, " +
            "release_status, industry_type" +
            ") VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(" +
            "#{item.standardIdentity}, #{item.industry}, #{item.detectionType}, #{item.detectionInstrument}, " +
            "#{item.standardNumber}, #{item.standardName}, #{item.detectionItem}, #{item.methodNumber}, #{item.remark}, " +
            "#{item.detectionParameters}, #{item.releaseDate}, #{item.expirationDate}, #{item.effectiveDate}, " +
            "#{item.releaseStatus}, #{item.industryType}" +
            ")" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("list") List<Detection> list);

    /**
     * 查询所有检测数据（包含标准标识ID）
     * @return 检测数据列表
     */
    @Select("SELECT * FROM t_report_standard_detection")
    List<Detection> selectAll();

    /**
     * 根据标准标识ID查询检测数据（新增：用于关联查询）
     * @param standardIdentity 标准标识ID
     * @return 对应的检测数据列表
     */
    @Select("SELECT * FROM t_report_standard_detection WHERE standard_identity = #{standardIdentity}")
    List<Detection> selectByStandardIdentity(Integer standardIdentity);

    @Select("SELECT id FROM t_report_standard_detection WHERE standard_identity = #{standardIdentityName}")
    Integer findIdByStandardIdentityName(String standardIdentityName);

    @Select("SELECT COUNT(*) > 0 FROM t_report_standard_detection WHERE standard_identity = #{standardIdentity}")
    boolean existsByStandardIdentity(@Param("standardIdentity") String standardIdentity);

    /**
     * 修正：根据标准标识ID更新检测表记录
     * 表名改为检测表 t_report_standard_detection，参数改为Detection实体
     */
    @Update("UPDATE t_report_standard_detection SET " +
            "industry = #{industry}, " +
            "detection_type = #{detectionType}, " +
            "detection_instrument = #{detectionInstrument}, " +
            "standard_number = #{standardNumber}, " +
            "standard_name = #{standardName}, " +
            "detection_item = #{detectionItem}, " +
            "method_number = #{methodNumber}, " +
            "remark = #{remark}, " +
            "detection_parameters = #{detectionParameters}, " +
            "release_date = #{releaseDate}, " +
            "expiration_date = #{expirationDate}, " +
            "effective_date = #{effectiveDate}, " +
            "release_status = #{releaseStatus}, " +
            "industry_type = #{industryType} " +
            "WHERE standard_identity = #{standardIdentity}")
    void updateByStandardIdentity(Detection detection);
}