package com.persee.workstation.uv_inexus.dao.parseReport;

import java.util.List;
import java.util.Set;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.persee.workstation.uv_inexus.po.parseReport.Detection;

/**
 * 检测表DAO层，处理检测数据的CRUD（适配哈希对比逻辑）
 */
@Mapper
public interface DetectionMapper {

    /**
     * 插入单条检测数据
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
     * 批量插入检测数据
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
     * 查询所有检测数据
     */
    @Select("SELECT * FROM t_report_standard_detection")
    List<Detection> selectAll();

    /**
     * 根据标准标识查询单条检测数据（适配哈希对比：返回单条记录）
     * 修正参数类型为String（因为standard_identity存储的是标识字符串）
     */
    @Select("SELECT * FROM t_report_standard_detection WHERE standard_identity = #{standardIdentity}")
    Detection selectByStandardIdentity(String standardIdentity);

    /**
     * 根据标准标识名称查询记录ID（用于子表关联主表）
     */
    @Select("SELECT id FROM t_report_standard_detection WHERE standard_identity = #{standardIdentityName}")
    Integer findIdByStandardIdentityName(String standardIdentityName);

    /**
     * 检查记录是否存在（用于主表新增/更新判断）
     */
    @Select("SELECT COUNT(*) > 0 FROM t_report_standard_detection WHERE standard_identity = #{standardIdentity}")
    boolean existsByStandardIdentity(@Param("standardIdentity") String standardIdentity);

    /**
     * 根据标准标识更新检测表记录（全字段更新，适配哈希对比后的数据更新）
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

    // 新增：根据ID列表查询
    @Select("<script>" +  // 关键：添加 <script> 标签包裹动态SQL
            "SELECT * FROM t_report_standard_detection " +  // 表名正确
            "WHERE id IN " +  // 按id查询
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +  // 临时变量id，与item='id'对应
            "</foreach>" +
            "</script>")
    List<Detection> selectByIds(@Param("ids") List<String> ids);

    // 关键：添加 selectByStandardNumbers 方法的 SQL 注解
    @Select("<script>" +  // 必须用 <script> 包裹动态 SQL
            "SELECT * FROM t_report_standard_detection " +  // 替换为你的 Detection 表实际表名
            "WHERE standard_number IN " +  // 匹配 Detection 表中的 standard_number 字段
            "<foreach collection='standardNumbers' item='num' open='(' separator=',' close=')'>" +
            "#{num}" +  // 临时变量 num 对应 item='num'
            "</foreach>" +
            "</script>")
    List<Detection> selectByStandardNumbers(@Param("standardNumbers") Set<String> standardNumbers);

    /**
     * 根据记录ID查询标准标识（用于日志输出）
     */
    @Select("SELECT standard_identity FROM t_report_standard_detection WHERE id = #{id}")
    String findStandardIdentityById(Integer id);

}