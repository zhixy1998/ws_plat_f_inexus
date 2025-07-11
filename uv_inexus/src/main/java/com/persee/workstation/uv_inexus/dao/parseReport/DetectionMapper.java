package com.persee.workstation.uv_inexus.dao.parseReport;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.persee.workstation.uv_inexus.po.parseReport.Detection;

@Mapper
public interface DetectionMapper {

    @Insert("INSERT INTO t_report_standard_detection (standard_identity, industry, detection_type, detection_instrument, " +
            "standard_number, standard_name, detection_item, method_number, remark, detection_parameters,release_date,expiration_date,effective_date,release_status,industry_type) " +
            "VALUES (#{standardIdentity}, #{industry}, #{detectionType}, #{detectionInstrument}, " +
            "#{standardNumber}, #{standardName}, #{detectionItem}, #{methodNumber}, #{remark}, #{detectionParameters}, #{releaseDate}, #{expirationDate}, #{effectiveDate}, #{releaseStatus}, #{industryType})")
    void insert(Detection detection);

    // 如果需要批量插入，可以添加以下方法
    void batchInsert(@Param("list") List<Detection> list);

    // 手动实现 selectList 方法
    @Select("SELECT * FROM t_report_standard_detection")
    List<Detection> selectAll();
}
