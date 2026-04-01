/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.sysUserPermissionService.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserGroupMapper;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserGroupPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.UserRoutePermissionService;

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

    @Override
    public Result<List<?>> getMenuRouteStructure() {
        // 查询所有perm_type=1的权限
        QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("perm_type", 1)
                .orderByAsc("sort");
        List<SysPermissionPO> permissions = sysPermissionMapper.selectList(queryWrapper);
        
        // 构建路由结构
        List<RouteNode> routeNodes = buildRouteStructure(permissions);
        
        return Result.ok(routeNodes);
    }
    
    // 构建路由结构
    private List<RouteNode> buildRouteStructure(List<SysPermissionPO> permissions) {
        // 先按parent_id分组
        Map<Integer, List<SysPermissionPO>> parentMap = new HashMap<>();
        // 同时创建ID到权限对象的映射
        Map<String, SysPermissionPO> permMap = new HashMap<>();
        
        for (SysPermissionPO perm : permissions) {
            int parentId = perm.getParentId() != null ? perm.getParentId() : 0;
            parentMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(perm);
            permMap.put(perm.getId(), perm);
        }
        
        // 构建根节点（parent_id=0或null）
        List<RouteNode> rootNodes = new ArrayList<>();
        List<SysPermissionPO> rootPerms = parentMap.getOrDefault(0, Collections.emptyList());
        
        for (SysPermissionPO perm : rootPerms) {
            RouteNode node = createRouteNode(perm);
            // 递归构建子节点
            buildChildren(node, parentMap, permMap, perm.getId());
            rootNodes.add(node);
        }
        
        return rootNodes;
    }
    
    // 递归构建子节点
    private void buildChildren(RouteNode parentNode, Map<Integer, List<SysPermissionPO>> parentMap, Map<String, SysPermissionPO> permMap, String parentPermId) {
        // 获取当前权限对象
        SysPermissionPO parentPerm = permMap.get(parentPermId);
        if (parentPerm == null) {
            return;
        }
        
        // 使用parentPerm的ID作为parentId来查找子节点
        int parentId = Integer.parseInt(parentPerm.getId());
        List<SysPermissionPO> childrenPerms = parentMap.getOrDefault(parentId, Collections.emptyList());
        
        if (!childrenPerms.isEmpty()) {
            List<RouteNode> children = new ArrayList<>();
            for (SysPermissionPO perm : childrenPerms) {
                RouteNode childNode = createRouteNode(perm);
                buildChildren(childNode, parentMap, permMap, perm.getId());
                children.add(childNode);
            }
            parentNode.setChildren(children);
        }
    }
    
    // 创建路由节点
    private RouteNode createRouteNode(SysPermissionPO perm) {
        RouteNode node = new RouteNode();
        node.setPath(perm.getPath());
        node.setName(perm.getPath()); // path和name都读取path字段
        
        // 设置meta
        RouteMeta meta = new RouteMeta();
        meta.setTitle(perm.getTitle());
        node.setMeta(meta);
        
        // 设置redirect
        if (perm.getRedirect() != null && !perm.getRedirect().isEmpty()) {
            node.setRedirect(perm.getRedirect());
        }
        
        // 设置component（仅子节点）
        if (perm.getComponent() != null && !perm.getComponent().isEmpty()) {
            node.setComponent(perm.getComponent());
        }
        
        return node;
    }
    
    // 路由节点类
    private static class RouteNode {
        private String path;
        private String name;
        private RouteMeta meta;
        private String redirect;
        private List<RouteNode> children;
        private String component;
        
        // getters and setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public RouteMeta getMeta() { return meta; }
        public void setMeta(RouteMeta meta) { this.meta = meta; }
        public String getRedirect() { return redirect; }
        public void setRedirect(String redirect) { this.redirect = redirect; }
        public List<RouteNode> getChildren() { return children; }
        public void setChildren(List<RouteNode> children) { this.children = children; }
        public String getComponent() { return component; }
        public void setComponent(String component) { this.component = component; }
    }
    
    // 路由元数据类
    private static class RouteMeta {
        private String title;
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
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