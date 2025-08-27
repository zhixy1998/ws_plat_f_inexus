package com.persee.workstation.uv_inexus.dao.concise.measurementDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureSampleHead;
import org.apache.ibatis.annotations.Mapper;

/**
 * 样本头表(FMeasureSampleHead)表数据库访问层
 */
@Mapper
public interface FMeasureSampleHeadMapper extends BaseMapper<FMeasureSampleHead> {
}
