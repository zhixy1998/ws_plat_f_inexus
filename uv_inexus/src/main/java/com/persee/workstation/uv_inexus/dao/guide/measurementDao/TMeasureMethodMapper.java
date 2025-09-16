package com.persee.workstation.uv_inexus.dao.guide.measurementDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.dto.measurementDto.SelectMethodsByTypeResult;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


/**
 * 测量方法表(TMeasureMethod)表数据库访问层
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */

public interface TMeasureMethodMapper extends BaseMapper<TMeasureMethod> {
    @Select("SELECT measurement_type as methodtype, COUNT(*) as methodNumber " +
            "FROM t_measure_method " +
            "GROUP BY measurement_type " +
            "ORDER BY methodNumber DESC")
    List<SelectMethodsByTypeResult> selectMethodsGroupByType();

    @Select("select count(*) from t_measure_method where method_creator = #{methodCreator}")
    Integer getMethodsNum(@Param("methodCreator") String userKey);

    @Select("select * from t_measure_method where method_creator = #{userName}")
    List<TMeasureMethod> selectMethods(@Param("userName") String userName);
}

