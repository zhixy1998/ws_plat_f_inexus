package com.persee.workstation.uv_inexus.dao.guide.systemManageDao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.factory_inspection.ReminderSwitchConfigPO;
import org.apache.ibatis.annotations.Mapper;

@DS("guide")
@Mapper
public interface ReminderSwitchConfigMapper extends BaseMapper<ReminderSwitchConfigPO> {
}
