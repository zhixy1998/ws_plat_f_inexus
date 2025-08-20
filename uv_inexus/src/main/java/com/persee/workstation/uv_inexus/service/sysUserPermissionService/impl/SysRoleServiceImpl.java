package com.persee.workstation.uv_inexus.service.sysUserPermissionService.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysRoleMapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleSaveDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysRoleService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRolePO> implements SysRoleService {

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
}