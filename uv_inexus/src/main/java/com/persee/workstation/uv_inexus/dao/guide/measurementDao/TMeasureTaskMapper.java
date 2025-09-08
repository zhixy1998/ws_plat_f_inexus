package com.persee.workstation.uv_inexus.dao.guide.measurementDao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.TaskBaseVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 测量任务表(TMeasureTask)表数据库访问层
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
public interface TMeasureTaskMapper extends BaseMapper<TMeasureTask> {

    List<TaskBaseVO> getAllTask(@Param("projectId") String projectId, @Param("measureType") String measureType);

}

