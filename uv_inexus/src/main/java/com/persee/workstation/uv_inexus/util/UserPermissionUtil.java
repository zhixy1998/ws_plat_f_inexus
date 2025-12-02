/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysUserService;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.UserPermissionService;
import com.persee.workstation.uv_inexus.utils.OperateLog.UserContext;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户权限工具类
 * 提供根据用户名获取权限信息的功能
 */
@Component
public class UserPermissionUtil {

    @Autowired
    private UserPermissionService userPermissionService;
    @Autowired
    private SysUserService sysUserService;

    private static UserPermissionUtil instance;
    private static UserPermissionService staticUserPermissionService;

    @PostConstruct
    public void init() {
        instance = this;
        staticUserPermissionService = userPermissionService;
    }

    /**
     * 根据用户ID获取用户权限列表
     *
     * @param userName 用户ID
     * @return 权限列表
     */
    public  List<PermissionDTO> getUserPermissions(String userName) {
        try {
            if (staticUserPermissionService == null) {
                return new ArrayList<>();
            }
            LambdaQueryWrapper<SysUserPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserPO::getUsername, userName);
            SysUserPO one = sysUserService.getOne(wrapper);
            Result<List<PermissionDTO>> result = staticUserPermissionService.getUserPermissions(one.getId());
            if (result != null && result.getCode()==200 && !CollectionUtils.isEmpty(result.getData())) {
                return result.getData();
            }
        } catch (Exception e) {
            // 异常处理，返回空列表而不是抛出异常
        }
        return new ArrayList<>();
    }

    /**
     * 根据当前登录用户获取权限列表
     *
     * @return 权限列表
     */
    public  List<PermissionDTO> getCurrentUserPermissions() {
        try {
            LoginUserInfoVO currentUser = UserContext.getCurrentUser();
            if (currentUser != null) {
                return getUserPermissions(currentUser.getUsername());
            }
        } catch (Exception e) {
            // 异常处理
        }
        return new ArrayList<>();
    }

    /**
     * 根据用户ID获取用户权限编码列表
     *
     * @param userId 用户ID
     * @return 权限编码列表
     */
    public  List<String> getUserPermissionCodes(String userId) {
        List<PermissionDTO> permissions = getUserPermissions(userId);
        return extractPermissionCodes(permissions);
    }

    /**
     * 根据当前登录用户获取权限编码列表
     *
     * @return 权限编码列表
     */
    public  List<String> getCurrentUserPermissionCodes() {
        List<PermissionDTO> permissions = getCurrentUserPermissions();
        return extractPermissionCodes(permissions);
    }

    /**
     * 从权限列表中提取权限编码
     *
     * @param permissions 权限列表
     * @return 权限编码列表
     */
    private List<String> extractPermissionCodes(List<PermissionDTO> permissions) {
        List<String> codes = new ArrayList<>();
        if (CollectionUtils.isEmpty(permissions)) {
            return codes;
        }

        // 递归提取所有权限码
        extractCodesRecursively(permissions, codes);
        return codes;
    }
    private void extractCodesRecursively(List<PermissionDTO> permissions, List<String> codes) {
        for (PermissionDTO permission : permissions) {
            // 添加当前权限的权限码
            if (permission.getPermCode() != null && !permission.getPermCode().trim().isEmpty()) {
                codes.add(permission.getPermCode());
            }

            // 递归处理子权限
            if (!CollectionUtils.isEmpty(permission.getChildren())) {
                extractCodesRecursively(permission.getChildren(), codes);
            }
        }
    }

    /**
     * 检查用户是否具有指定权限
     *
     * @param userId         用户ID
     * @param permissionCode 权限编码
     * @return 是否具有权限
     */
    public  boolean hasPermission(String userId, String permissionCode) {
        List<String> userPermissions = getUserPermissionCodes(userId);
        return userPermissions.contains(permissionCode);
    }

    /**
     * 检查当前登录用户是否具有指定权限
     *
     * @param permissionCode 权限编码
     * @return 是否具有权限
     */
    public  boolean hasCurrentPermission(String permissionCode) {
        List<String> userPermissions = getCurrentUserPermissionCodes();
        return userPermissions.contains(permissionCode);
    }

    /**
     * 获取当前工具类实例
     *
     * @return 工具类实例
     */
    public  UserPermissionUtil getInstance() {
        return instance;
    }
}
