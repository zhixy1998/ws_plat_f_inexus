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
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskManagementMapper extends BaseMapper<TMeasureTask> {

    @Select("select count(*) from t_measure_task where user_id = #{userId}")
    Integer getTasksNum(@Param("userId") String userId);

    @Select("select count(*) from t_measure_task where method_key = #{methodId} and is_delete = 1")
    Integer getTaskByMethodId(String methodId);

    @Select("select count(*) from t_measure_task where user_id = #{userId} and is_delete = 1 ")
    Integer getFinishTaskNum(String userId);
}
