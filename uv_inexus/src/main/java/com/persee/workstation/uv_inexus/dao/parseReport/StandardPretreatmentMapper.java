package com.persee.workstation.uv_inexus.dao.parseReport;

import java.util.List;

// import org.apache.ibatis.annotations.Mapper;
// import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.persee.workstation.uv_inexus.po.parseReport.StandardPretreatment;

@Mapper
public interface StandardPretreatmentMapper {

    @Insert("INSERT INTO t_report_standard_pretreatment (standard_identity, pretreatment_description) " +
            "VALUES (#{standardIdentity}, #{pretreatmentDescription})")
    void insert(StandardPretreatment standardPretreatment);

    // 如果需要批量插入，可以添加以下方法
    void batchInsert(@Param("list") List<StandardPretreatment> list);
    // 手动实现 selectList 方法

    @Select("SELECT * FROM t_report_standard_pretreatment")
    List<StandardPretreatment> selectAll();

    // 修改exists方法，根据Detection ID而非标准标识名称
    @Select("SELECT COUNT(*) FROM t_report_standard_pretreatment WHERE standard_identity = #{detectionId}")
    boolean existsByDetectionId(Integer detectionId);
    
    // 修改update方法，根据Detection ID而非标准标识名称
    @Update("UPDATE t_report_standard_pretreatment SET pretreatment_description = #{pretreatmentDescription} " +
            "WHERE standard_identity = #{standardIdentity}")
    void updateByDetectionId(StandardPretreatment pretreatment);

    /**
     * 联表查询：将 standard_identity（ID）转换为对应的文字
     * 关联标准信息表（t_report_standard_detection），通过 ID 匹配原始文字
     */
    // 新增联表查询方法，使用别名映射到 Excel 模型类的字段
    @Select("SELECT " +
            "p.id, " +
            "d.standard_identity AS standard_identity," +
            "p.pretreatment_description " +
            "FROM t_report_standard_pretreatment p " +
            "LEFT JOIN t_report_standard_detection d " +
            "ON p.standard_identity = d.id")
    List<StandardPretreatment> selectAllForExport();  // 直接返回 Excel 模型类
}
