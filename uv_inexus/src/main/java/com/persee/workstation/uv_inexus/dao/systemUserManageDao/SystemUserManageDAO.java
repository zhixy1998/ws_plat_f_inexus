package com.persee.workstation.uv_inexus.dao.systemUserManageDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserManagePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SystemUserManageDAO extends BaseMapper<SystemUserManagePO> {


    String getPassword(@Param("user_name") String userName);

    String getToken(@Param("user_name") String userName);

    void updateToken(@Param("user_name") String userName, @Param("token") String token);
}
