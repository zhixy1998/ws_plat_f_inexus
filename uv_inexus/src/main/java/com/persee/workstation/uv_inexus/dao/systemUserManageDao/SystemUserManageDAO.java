package com.persee.workstation.uv_inexus.dao.systemUserManageDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserManagePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SystemUserManageDAO extends BaseMapper<SystemUserManagePO> {

    String getUserPermissionList(@Param("userName") String userName);
}
