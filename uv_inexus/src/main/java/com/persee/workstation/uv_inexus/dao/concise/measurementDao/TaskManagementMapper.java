package com.persee.workstation.uv_inexus.dao.concise.measurementDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskManagementMapper extends BaseMapper<FMeasureTask> {

    @Select("select count(*) from t_measure_task where user_id = #{userId}")
    Integer getTasksNum(@Param("userId") String userId);

}
