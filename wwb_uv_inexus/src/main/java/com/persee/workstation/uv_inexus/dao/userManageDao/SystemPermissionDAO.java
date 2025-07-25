package com.persee.workstation.uv_inexus.dao.userManageDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.userManagePo.SystemPermissionPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemPermissionDAO extends BaseMapper<SystemPermissionPO> {
}
