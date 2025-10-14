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

package com.persee.workstation.uv_inexus.dao.guide.measurementDao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.TaskBaseVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

/**
 * 测量任务表(TMeasureTask)表数据库访问层
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
public interface TMeasureTaskMapper extends BaseMapper<TMeasureTask> {

    @Select("select count(*) from t_measure_task where user_id = #{userId}")
    Integer getTasksNum(@Param("userId") String userId);

    @Select("select count(*) from t_measure_task where method_key = #{methodId} and is_delete = 1")
    Integer getTaskByMethodId(String methodId);

    @Select("select count(*) from t_measure_task where user_id = #{userId} and is_delete = 1 ")
    Integer getFinishTaskNum(String userId);

    List<TaskBaseVO> getAllTask(@Param("projectId") String projectId, @Param("measureType") String measureType);

    List<HeadVO> getPreHead(@Param("measure_type") String measureType, @Param("sample_kind") String sampleKind);

    @Select("select method_key from t_measure_task where user_id = #{userId}")
    Set<String> selectMethodIds(String userId);
}

