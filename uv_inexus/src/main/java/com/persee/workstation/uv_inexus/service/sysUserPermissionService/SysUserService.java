/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.sysUserPermissionService;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PasswordUpdateDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserDetailDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserSaveDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

import java.util.List;
import java.util.Map;

public interface SysUserService extends IService<SysUserPO> {

    /**
     * 分页查询用户
     */
    Result<PageView<SysUserPO>> getUserPage(Page<SysUserPO> page, UserQueryDTO queryDTO);

    /**
     * 新增用户
     */
    Result<String> addUser(UserSaveDTO saveDTO);

    /**
     * 修改用户
     */
    Result<String> updateUser(UserSaveDTO saveDTO);

    /**
     * 批量删除用户
     */
    Result<String> deleteBatch(List<String> ids);

    /**
     * 根据ID查询用户
     */
    Result<UserDetailDTO> getUserById(String id);

    /**
     * 根据ID删除单个用户
     * @param id 要删除的用户ID
     * @param updateById 操作人ID
     */
    Result<String> deleteById(String id, String updateById);

    /**
     * 修改密码
     */
    Result<String> updatePassword(PasswordUpdateDTO updateDTO);

    /**
     * 重置用户密码为默认值123456
     * @param userId 用户ID
     * @return 操作结果
     */
    Result<String> resetPassword(String userId);

    boolean updateUserStatusByGroupStatus(List<String> userIds, Integer status);

    boolean addQuickNavigation(Map<String, Object> quickNavigation);

    Result<JSONArray> getQuickNavigation(String userName);

}