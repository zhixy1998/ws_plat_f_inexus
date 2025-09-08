package com.persee.workstation.uv_inexus.dao.concise.measurementDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureAnalysisImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分析图片表(FMeasureAnalysisImage)表数据库访问层
 */
@Mapper
public interface FMeasureAnalysisImageMapper extends BaseMapper<FMeasureAnalysisImage> {
}