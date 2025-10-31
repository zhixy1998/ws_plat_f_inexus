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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysRoleMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.GroupTreeDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleSaveDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleTreeDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserTreeDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysRoleService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRolePO> implements SysRoleService {

    @Autowired
    private SysUserGroupMapper sysUserGroupMapper;
    
    @Autowired
    private SysGroupMapper sysGroupMapper;
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    // 分页查询：返回封装后的 Result<PageView>
    @Override
    public Result<PageView<SysRolePO>> getRolePage(Page<SysRolePO> page, RoleQueryDTO queryDTO) {
        QueryWrapper<SysRolePO> queryWrapper = new QueryWrapper<>();
        
        if (queryDTO.getRoleName() != null && !queryDTO.getRoleName().isEmpty()) {
            queryWrapper.like("role_name", queryDTO.getRoleName());
        }
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq("status", queryDTO.getStatus());
        }
        
        queryWrapper.orderByDesc("create_time");
        
        // 执行分页查询
        IPage<SysRolePO> iPage = baseMapper.selectPage(page, queryWrapper);
        // 用 PageView 封装分页数据
        PageView<SysRolePO> pageView = new PageView<>(iPage);
        // 返回统一 Result 格式
        return Result.ok(pageView);
    }

    // 其他方法保持不变（修正删除方法的描述错误）
    @Override
    @Transactional
    public Result<String> addRole(RoleSaveDTO saveDTO) {
        // 检查角色编码是否已存在
        QueryWrapper<SysRolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_code", saveDTO.getRoleCode());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            return Result.fail("角色编码已存在");
        }
        
        SysRolePO rolePO = new SysRolePO();
        BeanUtils.copyProperties(saveDTO, rolePO);
        // rolePO.setCreateTime(LocalDateTime.now());
        // rolePO.setUpdateTime(LocalDateTime.now());
        rolePO.setCreateBy("amdin");
        rolePO.setCreated(LocalDateTime.now());
        baseMapper.insert(rolePO);
        return Result.ok("角色创建成功");
    }

    @Override
    @Transactional
    public Result<String> updateRole(RoleSaveDTO saveDTO) {
        SysRolePO rolePO = baseMapper.selectById(saveDTO.getId());
        if (rolePO == null) {
            return Result.fail("角色不存在");
        }
        
        // 检查角色编码是否重复（排除当前角色）
        QueryWrapper<SysRolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_code", saveDTO.getRoleCode())
                   .ne("id", saveDTO.getId());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            return Result.fail("角色编码已存在");
        }
        
        BeanUtils.copyProperties(saveDTO, rolePO);
        // rolePO.setUpdateTime(LocalDateTime.now());
        rolePO.setUpdateBy("amdin");
        rolePO.setUpdated(LocalDateTime.now());
        baseMapper.updateById(rolePO);
        return Result.ok("角色更新成功");
    }

    @Override
    @Transactional
    public Result<String> deleteBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择需要删除的角色");
        }
        
        baseMapper.deleteBatchIds(ids);
        return Result.ok("角色删除成功");
    }

    @Override
    public Result<SysRolePO> getRoleById(String id) {
        SysRolePO rolePO = baseMapper.selectById(id);
        if (rolePO == null) {
            return Result.fail("角色不存在");
        }
        return Result.ok(rolePO);
    }

    @Override
    @Transactional
    public Result<String> deleteById(String id) {
        // 1. 校验角色是否存在（修正描述错误：将“用户”改为“角色”）
        SysRolePO rolePO = baseMapper.selectById(id);
        if (rolePO == null) {
            return Result.fail("角色不存在，删除失败");
        }
        
        // 2. 执行删除
        baseMapper.deleteById(id);
        return Result.ok("角色删除成功");  // 修正描述错误
    }

    @Override
    public Result<RoleTreeDTO> getUserRoleGroupTree(String userId) {
        try {
            // 1. 根据用户ID查询所属的用户组
            LambdaQueryWrapper<SysUserGroupPO> userGroupWrapper = new LambdaQueryWrapper<>();
            userGroupWrapper.eq(SysUserGroupPO::getUserId, userId);
            List<SysUserGroupPO> userGroups = sysUserGroupMapper.selectList(userGroupWrapper);
            
            if (userGroups == null || userGroups.isEmpty()) {
                return Result.fail("该用户未分配任何组");
            }
            
            // 2. 从用户组中获取第一个组的角色ID（假设用户只属于同一角色的组）
            String firstGroupId = userGroups.get(0).getGroupId();
            SysGroupPO firstGroup = sysGroupMapper.selectById(firstGroupId);
            if (firstGroup == null) {
                return Result.fail("用户所属组不存在");
            }
            
            String roleId = firstGroup.getRoleId();
            if (roleId == null) {
                return Result.fail("用户所属组未分配角色");
            }
            
            // 3. 查询角色信息
            SysRolePO role = baseMapper.selectById(roleId);
            if (role == null) {
                return Result.fail("角色不存在");
            }
            
            // 4. 转换为RoleTreeDTO
            RoleTreeDTO roleTreeDTO = new RoleTreeDTO();
            roleTreeDTO.setId(role.getId());
            roleTreeDTO.setRoleName(role.getRoleName());
            roleTreeDTO.setRoleCode(role.getRoleCode());
            roleTreeDTO.setStatus(role.getStatus());
            
            // 5. 查询该角色下的所有组
            LambdaQueryWrapper<SysGroupPO> groupWrapper = new LambdaQueryWrapper<>();
            groupWrapper.eq(SysGroupPO::getRoleId, roleId);
            groupWrapper.eq(SysGroupPO::getStatus, 1); // 只查询启用的组
            List<SysGroupPO> groups = sysGroupMapper.selectList(groupWrapper);
            
            // 6. 转换组信息并查询每个组下的用户
            List<GroupTreeDTO> groupTreeDTOs = groups.stream().map(group -> {
                GroupTreeDTO groupDTO = new GroupTreeDTO();
                groupDTO.setId(group.getId());
                groupDTO.setGroupName(group.getGroupName());
                groupDTO.setDescription(group.getDescription());
                groupDTO.setStatus(group.getStatus());
                
                // 查询该组下的所有用户
                LambdaQueryWrapper<SysUserGroupPO> ugWrapper = new LambdaQueryWrapper<>();
                ugWrapper.eq(SysUserGroupPO::getGroupId, group.getId());
                List<SysUserGroupPO> userGroupRelations = sysUserGroupMapper.selectList(ugWrapper);
                
                // 获取用户ID列表
                List<String> userIds = userGroupRelations.stream()
                        .map(SysUserGroupPO::getUserId)
                        .collect(Collectors.toList());
                
                // 查询用户信息
                if (!userIds.isEmpty()) {
                    LambdaQueryWrapper<SysUserPO> userWrapper = new LambdaQueryWrapper<>();
                    userWrapper.in(SysUserPO::getId, userIds);
                    userWrapper.eq(SysUserPO::getStatus, 1); // 只查询启用的用户
                    List<SysUserPO> users = sysUserMapper.selectList(userWrapper);
                    
                    // 转换为UserTreeDTO
                    List<UserTreeDTO> userDTOs = users.stream().map(user -> {
                        UserTreeDTO userDTO = new UserTreeDTO();
                        userDTO.setId(user.getId());
                        userDTO.setUsername(user.getUsername());
                        userDTO.setRealName(user.getRealName());
                        userDTO.setPhone(user.getPhone());
                        userDTO.setStatus(user.getStatus());
                        return userDTO;
                    }).collect(Collectors.toList());
                    
                    groupDTO.setUserList(userDTOs);
                }
                
                return groupDTO;
            }).collect(Collectors.toList());
            
            roleTreeDTO.setGroupList(groupTreeDTOs);
            
            return Result.ok(roleTreeDTO);
        } catch (Exception e) {
            log.error("查询用户角色组树形结构失败", e);
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 新增实现：根据角色ID查询下属所有组信息
     */
    @Override
    public Result<List<SysGroupPO>> getGroupsByRoleId(String roleId) {
        try {
            // 1. 校验角色是否存在
            SysRolePO rolePO = baseMapper.selectById(roleId);
            if (rolePO == null) {
                return Result.fail("角色不存在");
            }
            
            // 2. 校验角色状态（只查询启用的角色）
            if (rolePO.getStatus() != 1) {
                return Result.fail("该角色已禁用，无法查询下属组");
            }

            // 3. 使用MyBatis-Plus的LambdaQueryWrapper查询
            LambdaQueryWrapper<SysGroupPO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysGroupPO::getRoleId, roleId)  // 匹配角色ID
                       //.eq(SysGroupPO::getStatus, 1)       // 只查询启用状态的组
                       .orderByAsc(SysGroupPO::getGroupName); // 按组名称升序排列
            
            // 执行查询
            List<SysGroupPO> groupList = sysGroupMapper.selectList(queryWrapper);
            
            return Result.ok(groupList);
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }
}