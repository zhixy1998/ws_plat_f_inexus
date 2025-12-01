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

package com.persee.workstation.uv_inexus.service.sysUserPermissionService.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Collectors;

import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserGroupMapper;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserGroupPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.UserRoutePermissionService;

import javax.servlet.http.HttpServletRequest;

@Service
public class UserRoutePermissionServiceImpl implements UserRoutePermissionService {

    @Autowired
    private SysUserGroupMapper sysUserGroupMapper;

    @Autowired
    private SysGroupPermissionMapper sysGroupPermissionMapper;

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Value("${remote-access.enabled}")
    private Boolean enableRemoteAccess;
    @Override
    public Result<List<String>> getUserRoutePermissions(String userId, HttpServletRequest request) {
        // IP访问控制检查 - 新增逻辑
        if (!isAllowedIP(request)) {
            return Result.fail("访问被拒绝：IP未授权");
        }
        // 1. 查询用户所属的所有小组ID
        QueryWrapper<SysUserGroupPO> userGroupQuery = new QueryWrapper<>();
        userGroupQuery.eq("user_id", userId);
        List<String> groupIds = sysUserGroupMapper.selectList(userGroupQuery).stream()
                .map(SysUserGroupPO::getGroupId)
                .collect(Collectors.toList());
        if (groupIds.isEmpty()) {
            //用户未关联任何小组，无访问权限
            return Result.fail(YamlMessageSource.getMessageStatic("00606001"));
        }

        // 2. 查询小组关联的所有权限ID
        QueryWrapper<SysGroupPermissionPO> groupPermQuery = new QueryWrapper<>();
        groupPermQuery.in("group_id", groupIds);
        List<String> permIds = sysGroupPermissionMapper.selectList(groupPermQuery).stream()
                .map(SysGroupPermissionPO::getPermId)
                .distinct()
                .collect(Collectors.toList());
        if (permIds.isEmpty()) {
            //用户所在小组未配置权限
            return Result.fail(YamlMessageSource.getMessageStatic("00606002"));
        }

        // 3. 查询权限详情（只查菜单类型的权限），并提取 name 字段
        QueryWrapper<SysPermissionPO> permQuery = new QueryWrapper<>();
        permQuery.in("id", permIds)
                .eq("perm_type", 1); // 只筛选菜单类型的权限
        List<String> routeNames = sysPermissionMapper.selectList(permQuery).stream()
                .map(SysPermissionPO::getPermName) // 提取 name 字段
                .filter(name -> name != null && !name.isEmpty()) // 过滤空值
                .distinct() // 去重
                .collect(Collectors.toList());

        return Result.ok(routeNames);
    }

    /**
     * 检查当前请求IP是否被允许访问
     * 示例：获取当前请求IP并与允许的IP列表进行比较
     */
    private boolean isAllowedIP(HttpServletRequest request) {
        if (enableRemoteAccess){
            return true;
        }
        String clientIP = getClientIP(request);

        try {
            // 获取本机所有IP地址
            InetAddress[] localAddresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());

            // 检查客户端IP是否匹配本机任一IP
            for (InetAddress addr : localAddresses) {
                if (clientIP.equals(addr.getHostAddress())) {
                    return true;
                }
            }

            // 检查常见本地访问地址
            return clientIP.equals("127.0.0.1") || clientIP.equals("localhost");
        } catch (UnknownHostException e) {
            // 发生异常时，可以考虑宽松策略或严格策略
            // 严格策略：抛出异常
            // 宽松策略：允许本地访问
            return clientIP.equals("127.0.0.1") || clientIP.equals("localhost");
        }
    }


    /**
     * 获取客户端真实IP地址
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}