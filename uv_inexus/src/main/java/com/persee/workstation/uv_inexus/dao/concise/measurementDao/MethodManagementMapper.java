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

package com.persee.workstation.uv_inexus.dao.concise.measurementDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.persee.workstation.uv_inexus.dto.measurementDto.SelectMethodsByTypeResult;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MethodManagementMapper extends BaseMapper<TMeasureMethod> {


    @Select("SELECT measurement_type as methodtype, COUNT(*) as methodNumber " +
            "FROM t_measure_method " +
            "GROUP BY measurement_type " +
            "ORDER BY methodNumber DESC")
    List<SelectMethodsByTypeResult> selectMethodsGroupByType();

    @Select("select count(*) from t_measure_method where method_creator = #{methodCreator}")
    Integer getMethodsNum(@Param("methodCreator") String userKey);

}
