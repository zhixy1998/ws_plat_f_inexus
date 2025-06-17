package com.persee.workstation.uv_inexus.dao.parseReport;

import com.persee.workstation.uv_inexus.po.parseReport.Detection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import java.util.List;

@Mapper // MyBatis注解，标记这是一个MyBatis的Mapper接口
public interface DetectionDao {

    // 查询所有
    @Select("SELECT * FROM t_report_standard_detection")
    List<Detection> findAll();

    // 根据ID查询
    @Select("SELECT * FROM t_report_standard_detection WHERE id = #{id}")
    Detection findById(Integer id);

    // 插入新记录
    @Insert("INSERT INTO t_report_standard_detection (standard_identity, industry, detection_type, detection_instrument, standard_number, standard_name, detection_item, method_number, remark, detection_parameters) " +
            "VALUES (#{standardIdentity}, #{industry}, #{detectionType}, #{detectionInstrument}, #{standardNumber}, #{standardName}, #{detectionItem}, #{methodNumber}, #{remark}, #{detectionParameters})")
    void insert(Detection detection);

    // 更新记录
    @Update("UPDATE t_report_standard_detection SET standard_identity=#{standardIdentity}, industry=#{industry}, detection_type=#{detectionType}, " +
            "detection_instrument=#{detectionInstrument}, standard_number=#{standardNumber}, standard_name=#{standardName}, " +
            "detection_item=#{detectionItem}, method_number=#{methodNumber}, remark=#{remark}, detection_parameters=#{detectionParameters} " +
            
            "WHERE id = #{id}")
    void update(Detection detection);

    // 根据ID删除记录
    @Delete("DELETE FROM t_report_standard_detection WHERE id = #{id}")
    void deleteById(Integer id);
}
