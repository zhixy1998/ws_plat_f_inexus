package com.persee.workstation.uv_inexus.service.sysUserPermissionService.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysRoleMapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.GroupQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.GroupSaveDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO;
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
        // 1. 校验角色是否存在
        String roleId = saveDTO.getRoleId();
        if (roleId == null || roleId.isEmpty()) {
            return Result.fail("角色ID不能为空");
        }
        SysRolePO rolePO = sysRoleMapper.selectById(roleId);
        if (rolePO == null) {
            return Result.fail("该角色不存在，请刷新重试！");
        }

        // 2. 封装小组信息并保存
        SysGroupPO groupPO = new SysGroupPO();
        BeanUtils.copyProperties(saveDTO, groupPO);
        groupPO.setCreateBy("admin");  // 修正拼写错误（原"amdin"）
        groupPO.setCreated(LocalDateTime.now());
        baseMapper.insert(groupPO);

        return Result.ok("小组创建成功，已关联角色");
    }

    // 修改小组：校验角色存在性并更新关联
    @Override
    @Transactional
    public Result<String> updateGroup(GroupSaveDTO saveDTO) {
        // 1. 校验小组是否存在
        SysGroupPO groupPO = baseMapper.selectById(saveDTO.getId());
        if (groupPO == null) {
            return Result.fail("小组不存在");
        }

        // 2. 校验角色是否存在
        String roleId = saveDTO.getRoleId();
        if (roleId == null || roleId.isEmpty()) {
            return Result.fail("角色ID不能为空");
        }
        SysRolePO rolePO = sysRoleMapper.selectById(roleId);
        if (rolePO == null) {
            return Result.fail("该角色不存在，请刷新重试！");
        }

        // 3. 更新小组信息（包含角色ID）
        BeanUtils.copyProperties(saveDTO, groupPO);
        groupPO.setUpdateBy("admin");  // 修正拼写错误
        groupPO.setUpdated(LocalDateTime.now());
        baseMapper.updateById(groupPO);

        return Result.ok("小组更新成功，已同步更新角色关联");
    }

    // 批量删除小组
    @Override
    @Transactional
    public Result<String> deleteBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择需要删除的小组");
        }
        
        baseMapper.deleteBatchIds(ids);
        return Result.ok("小组删除成功");
    }

    // 根据ID查询小组
    @Override
    public Result<SysGroupPO> getGroupById(String id) {
        SysGroupPO groupPO = baseMapper.selectById(id);
        if (groupPO == null) {
            return Result.fail("小组不存在");
        }
        return Result.ok(groupPO);
    }

    // 根据ID删除小组
    @Override
    @Transactional
    public Result<String> deleteById(String id) {
        SysGroupPO groupPO = baseMapper.selectById(id);
        if (groupPO == null) {
            return Result.fail("小组不存在，删除失败"); 
        }
        
        baseMapper.deleteById(id);
        return Result.ok("小组删除成功"); 
    }
}