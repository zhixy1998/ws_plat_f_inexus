package com.persee.workstation.uv_inexus.dao.instrumentDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.instrumentPo.InitRecordPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InitRecordDAO extends BaseMapper<InitRecordPO> {
}
