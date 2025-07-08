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
    @Update("UPDATE t_report_standard_pretreatment SET pretreatment_description = #{pretreatmentDescription}, ... " +
            "WHERE standard_identity = #{standardIdentity}")
    void updateByDetectionId(StandardPretreatment pretreatment);

}

// package com.persee.workstation.uv_inexus.dao.parseReport;

// import com.persee.workstation.uv_inexus.po.parseReport.StandardPretreatment;
// import org.apache.ibatis.annotations.*;

// import java.util.List;

// @Mapper
// public interface StandardPretreatmentMapper {

//     // 插入
//     @Insert("INSERT INTO treportstandardpretreatment (standardidentity, pretreatmentdescription) " +
//             "VALUES (#{standardIdentity}, #{pretreatmentDescription})")
//     void insert(StandardPretreatment standardPretreatment);

//     // 查询所有
//     @Select("SELECT * FROM treportstandardpretreatment")
//     List<StandardPretreatment> findAll();
    
//     // 根据标准身份标识查询
//     @Select("SELECT * FROM treportstandardpretreatment WHERE standardidentity = #{standardIdentity}")
//     StandardPretreatment findById(@Param("standardIdentity") String standardIdentity);

//     // 更新
//     @Update("UPDATE treportstandardpretreatment " +
//             "SET pretreatmentdescription = #{pretreatmentDescription} " +
//             "WHERE standardidentity = #{standardIdentity}")
//     void update(StandardPretreatment standardPretreatment);

//     // 根据标准身份标识删除
//     @Delete("DELETE FROM treportstandardpretreatment WHERE standardidentity = #{standardIdentity}")
//     void deleteById(@Param("standardIdentity") String standardIdentity);

//     // 手动实现 selectList 方法
//     @Select("SELECT * FROM treportstandardpretreatment")
//     List<StandardPretreatment> selectAll();
// }

