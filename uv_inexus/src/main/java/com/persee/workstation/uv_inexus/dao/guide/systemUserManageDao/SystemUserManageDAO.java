/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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
