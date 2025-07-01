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

    // 检查是否存在指定 standardIdentity 的记录
    @Select("SELECT COUNT(*) FROM t_report_standard_pretreatment WHERE standard_identity = #{standardIdentity}")
    boolean existsByStandardIdentity(@Param("standardIdentity") String standardIdentity);

    // 根据标准标识更新记录
    @Update("UPDATE t_report_standard_pretreatment " +
        "SET pretreatment_description = #{pretreatmentDescription} " + // 第一个字段
        "WHERE standard_identity = #{standardIdentity}")
    void updateByStandardIdentity(StandardPretreatment standardPretreatment);

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

