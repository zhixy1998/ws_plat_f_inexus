package com.persee.workstation.uv_inexus.dao.systemUserManageDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserManagePO;
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
