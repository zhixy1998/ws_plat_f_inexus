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

import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysRoleMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.GroupQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.GroupSaveDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysGroupService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

@Service
public class SysGroupServiceImpl extends ServiceImpl<SysGroupMapper, SysGroupPO> implements SysGroupService {

    // @Autowired
    // private SysGroupMapper sysGroupMapper;

    // 注入角色Mapper用于校验角色是否存在
    @Autowired
    private SysRoleMapper sysRoleMapper;




    // 分页查询
    @Override
    public Result<PageView<SysGroupPO>> getGroupPage(Page<SysGroupPO> page, GroupQueryDTO queryDTO) {
        QueryWrapper<SysGroupPO> queryWrapper = new QueryWrapper<>();
        
        if (queryDTO.getGroupName() != null && !queryDTO.getGroupName().isEmpty()) {
            queryWrapper.like("group_name", queryDTO.getGroupName());
        }
        if (queryDTO.getRoleId() != null && !queryDTO.getRoleId().isEmpty()) {
            queryWrapper.eq("role_id", queryDTO.getRoleId());
        }
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq("status", queryDTO.getStatus());
        }
        
        queryWrapper.orderByDesc("create_time");
        
        IPage<SysGroupPO> iPage = baseMapper.selectPage(page, queryWrapper);
        PageView<SysGroupPO> pageView = new PageView<>(iPage);
        return Result.ok(pageView);
    }

    // 新增小组：校验角色存在性并关联
    @Override
    @Transactional
    public Result<String> addGroup(GroupSaveDTO saveDTO) {
        // 1. 校验组名是否已存在
        QueryWrapper<SysGroupPO> nameCheckWrapper = new QueryWrapper<>();
        nameCheckWrapper.eq("group_name", saveDTO.getGroupName());
        Long count = baseMapper.selectCount(nameCheckWrapper);
        if (count > 0) {
            //小组名已存在，请更改
            return Result.fail(YamlMessageSource.getMessageStatic("00602001"));
        }

        // 2. 校验角色是否存在
        String roleId = saveDTO.getRoleId();
        if (roleId == null || roleId.isEmpty()) {
            //角色ID不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00602002"));
        }
        SysRolePO rolePO = sysRoleMapper.selectById(roleId);
        if (rolePO == null) {
            //该角色不存在，请刷新重试！
            return Result.fail(YamlMessageSource.getMessageStatic("00602003"));
        }

        // 3. 封装小组信息并保存
        SysGroupPO groupPO = new SysGroupPO();
        BeanUtils.copyProperties(saveDTO, groupPO);
        groupPO.setCreateBy("admin");  // 修正拼写错误（原"amdin"）
        groupPO.setCreated(LocalDateTime.now());
        baseMapper.insert(groupPO);
        //小组创建成功，已关联角色
        return Result.ok(YamlMessageSource.getMessageStatic("00602004"));
    }

    // 修改小组：校验角色存在性并更新关联
    @Override
    @Transactional
    public Result<String> updateGroup(GroupSaveDTO saveDTO) {
        // 1. 校验小组是否存在
        SysGroupPO groupPO = baseMapper.selectById(saveDTO.getId());
        if (groupPO == null) {
            //小组不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00602005"));
        }

        // 2. 校验角色是否存在
        String roleId = saveDTO.getRoleId();
        if (roleId == null || roleId.isEmpty()) {
            //角色ID不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00602002"));
        }
        SysRolePO rolePO = sysRoleMapper.selectById(roleId);
        if (rolePO == null) {
            //该角色不存在，请刷新重试！
            return Result.fail(YamlMessageSource.getMessageStatic("00602003"));
        }

        // 3. 更新小组信息（包含角色ID）
        BeanUtils.copyProperties(saveDTO, groupPO);
        groupPO.setUpdateBy("admin");  // 修正拼写错误
        groupPO.setUpdated(LocalDateTime.now());
        baseMapper.updateById(groupPO);
        //小组更新成功，已同步更新角色关联
        return Result.ok(YamlMessageSource.getMessageStatic("00602006"));
    }

    // 批量删除小组
    @Override
    @Transactional
    public Result<String> deleteBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            //请选择需要删除的小组
            return Result.fail(YamlMessageSource.getMessageStatic("00602007"));
        }
        
        baseMapper.deleteBatchIds(ids);
        //小组删除成功
        return Result.ok(YamlMessageSource.getMessageStatic("00602008"));
    }

    // 根据ID查询小组
    @Override
    public Result<SysGroupPO> getGroupById(String id) {
        SysGroupPO groupPO = baseMapper.selectById(id);
        if (groupPO == null) {
            //小组不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00602005"));
        }
        return Result.ok(groupPO);
    }

    // 根据ID删除小组
    @Override
    @Transactional
    public Result<String> deleteById(String id) {
        SysGroupPO groupPO = baseMapper.selectById(id);
        if (groupPO == null) {
            return Result.fail(YamlMessageSource.getMessageStatic("00602009"));
        }
        
        baseMapper.deleteById(id);
        //小组删除成功
        return Result.ok(YamlMessageSource.getMessageStatic("00602008"));
    }
}