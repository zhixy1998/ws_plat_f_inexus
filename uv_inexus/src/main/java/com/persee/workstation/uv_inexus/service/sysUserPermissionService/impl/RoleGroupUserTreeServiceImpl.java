package com.persee.workstation.uv_inexus.service.sysUserPermissionService.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysRoleMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.GroupTreeDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleTreeDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserTreeDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.RoleGroupUserTreeService;

@Service
public class RoleGroupUserTreeServiceImpl implements RoleGroupUserTreeService {

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysGroupMapper sysGroupMapper;

    @Autowired
    private SysUserGroupMapper sysUserGroupMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public Result<List<RoleTreeDTO>> getRoleGroupUserTree() {
        // 1. 查询所有启用的角色（状态为1）
        QueryWrapper<SysRolePO> roleQuery = new QueryWrapper<>();
        roleQuery.eq("status", 1);
        List<SysRolePO> roleList = sysRoleMapper.selectList(roleQuery);

        // 2. 组装角色树形结构
        List<RoleTreeDTO> roleTreeList = roleList.stream().map(role -> {
            RoleTreeDTO roleTree = new RoleTreeDTO();
            BeanUtils.copyProperties(role, roleTree);

            // 3. 查询该角色下的所有启用的组（使用MyBatis-Plus条件构造器，替代XML的selectByRoleId）
            QueryWrapper<SysGroupPO> groupQuery = new QueryWrapper<>();
            groupQuery.eq("role_id", role.getId())  // 关联角色ID
                      .eq("status", 1);             // 只查启用的组
            List<SysGroupPO> groupList = sysGroupMapper.selectList(groupQuery);

            // 4. 组装组树形结构
            List<GroupTreeDTO> groupTreeList = groupList.stream().map(group -> {
                GroupTreeDTO groupTree = new GroupTreeDTO();
                BeanUtils.copyProperties(group, groupTree);

                // 5. 查询该组下的所有用户ID（使用MyBatis-Plus条件构造器，替代XML的selectUserIdsByGroupId）
                QueryWrapper<SysUserGroupPO> userGroupQuery = new QueryWrapper<>();
                userGroupQuery.eq("group_id", group.getId());  // 关联组ID
                List<SysUserGroupPO> userGroupList = sysUserGroupMapper.selectList(userGroupQuery);
                // 提取用户ID列表
                List<String> userIds = userGroupList.stream()
                        .map(SysUserGroupPO::getUserId)
                        .collect(Collectors.toList());

                // 6. 根据用户ID查询启用的用户
                List<SysUserPO> userList = new ArrayList<>();
                if (!userIds.isEmpty()) {
                    QueryWrapper<SysUserPO> userQuery = new QueryWrapper<>();
                    userQuery.in("id", userIds)       // 批量查询用户
                             .eq("status", 1);        // 只查启用的用户
                    userList = sysUserMapper.selectList(userQuery);
                }

                // 7. 组装用户树形结构
                List<UserTreeDTO> userTreeList = userList.stream().map(user -> {
                    UserTreeDTO userTree = new UserTreeDTO();
                    BeanUtils.copyProperties(user, userTree);
                    return userTree;
                }).collect(Collectors.toList());

                groupTree.setUserList(userTreeList);
                return groupTree;
            }).collect(Collectors.toList());

            roleTree.setGroupList(groupTreeList);
            return roleTree;
        }).collect(Collectors.toList());

        return Result.ok(roleTreeList);
    }
}