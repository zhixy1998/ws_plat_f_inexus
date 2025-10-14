/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.GroupVO;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.QueryUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemUserManageDAO extends BaseMapper<SystemUserManagePO> {


    String getPassword(@Param("user_name") String userName);

    String getToken(@Param("user_name") String userName);

    void updateToken(@Param("user_name") String userName, @Param("token") String token);

    List<QueryUserVO> queryRoleInfo(@Param("system_role_num") String systemRoleNum);

    List<GroupVO> queryGroupInfo(@Param("system_identity_id") String systemIdentityId, @Param("groupNo") String groupNO);
}
