package com.persee.workstation.uv_inexus.dao.guide.measurementDao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.TaskBaseVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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

}

