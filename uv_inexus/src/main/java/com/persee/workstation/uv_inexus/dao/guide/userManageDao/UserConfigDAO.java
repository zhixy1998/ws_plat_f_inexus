package com.persee.workstation.uv_inexus.dao.guide.userManageDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.guide.userManagePo.UserConfigPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserConfigDAO extends BaseMapper<UserConfigPO> {

    String getPassword(@Param("user_name") String userName);

    String getToken(@Param("user_name") String userName);

    void updateToken(@Param("user_name") String userName, @Param("token") String token);
}
