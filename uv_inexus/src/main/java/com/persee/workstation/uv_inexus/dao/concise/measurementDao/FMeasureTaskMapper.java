package com.persee.workstation.uv_inexus.dao.concise.measurementDao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FTaskBaseVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 测量任务表(TMeasureTask)表数据库访问层
 */

public interface FMeasureTaskMapper extends BaseMapper<FMeasureTask> {

    List<FTaskBaseVO> getAllTask(@Param("projectId") String projectId, @Param("measureType") String measureType);

    List<FHeadVO> getPreHead(@Param("measure_type") String measureType, @Param("sample_kind") String sampleKind);


}

