/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.sysUserPermissionService.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
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
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysRoleMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserGroupMapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.GroupQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.GroupSaveDTO;
import com.persee.workstation.uv_inexus.i18n.I18nMessageSource;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserGroupPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysGroupService;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysUserService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

@Service
public class SysGroupServiceImpl extends ServiceImpl<SysGroupMapper, SysGroupPO> implements SysGroupService {

    // @Autowired
    // private SysGroupMapper sysGroupMapper;

    // 注入角色Mapper用于校验角色是否存在
    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private SysUserGroupMapper  sysUserGroupMapper;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysGroupPermissionMapper sysGroupPermissionMapper;


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
        nameCheckWrapper.eq("group_name", saveDTO.getGroupName())
                .eq("role_id", saveDTO.getRoleId());Long count = baseMapper.selectCount(nameCheckWrapper);
        if (count > 0) {
            //小组名已存在，请更改
            return Result.fail("00D02001");
        }

        // 2. 校验角色是否存在
        String roleId = saveDTO.getRoleId();
        if (roleId == null || roleId.isEmpty()) {
            //角色ID不能为空
            return Result.fail("00D02002");
        }
        SysRolePO rolePO = sysRoleMapper.selectById(roleId);
        if (rolePO == null) {
            //该角色不存在，请刷新重试！
            return Result.fail("00D02003");
        }

        // 3. 封装小组信息并保存
        SysGroupPO groupPO = new SysGroupPO();
        BeanUtils.copyProperties(saveDTO, groupPO);
        groupPO.setCreateBy("admin");
        groupPO.setCreated(LocalDateTime.now());
        baseMapper.insert(groupPO);

        // ========== 新增逻辑开始 ==========
        // 4. 从角色表获取perm_ids（格式：1,2,3,4）
        String permIds = rolePO.getPermIds();
        if (permIds != null && !permIds.isEmpty()) {
            // 4.1 拆分perm_ids为单个权限ID
            String[] permIdArray = permIds.split(",");
            // 4.2 循环创建小组-权限关联数据
            for (String permId : permIdArray) {
                // 过滤空值和空格
                permId = permId.trim();
                if (permId.isEmpty()) {
                    continue;
                }
                // 4.3 封装关联数据
                SysGroupPermissionPO groupPermissionPO = new SysGroupPermissionPO();
                groupPermissionPO.setGroupId(groupPO.getId()); // 新建小组ID
                groupPermissionPO.setPermId(permId);          // 单个权限ID
                groupPermissionPO.setCreateBy("admin");
                groupPermissionPO.setCreated(LocalDateTime.now());
                // 4.4 插入关联表（需注入SysGroupPermissionMapper）
                sysGroupPermissionMapper.insert(groupPermissionPO);
            }
        }
        // ========== 新增逻辑结束 ==========

        //小组创建成功，已关联角色
        return Result.ok("00D02004");
    }

    // 修改小组：校验角色存在性并更新关联
    @Override
    @Transactional
    public Result<String> updateGroup(GroupSaveDTO saveDTO) {
        // 1. 校验小组是否存在
        SysGroupPO groupPO = baseMapper.selectById(saveDTO.getId());
        if (groupPO == null) {
            //小组不存在
            return Result.fail("00D02005");
        }

        // 2. 校验角色是否存在
        String roleId = saveDTO.getRoleId();
        if (roleId == null || roleId.isEmpty()) {
            //角色ID不能为空
            return Result.fail("00D02002");
        }
        SysRolePO rolePO = sysRoleMapper.selectById(roleId);
        if (rolePO == null) {
            //该角色不存在，请刷新重试！
            return Result.fail("00D02003");
        }

        // 3. 更新小组信息（包含角色ID）
        BeanUtils.copyProperties(saveDTO, groupPO);
        groupPO.setUpdateBy("admin");  // 修正拼写错误
        groupPO.setUpdated(LocalDateTime.now());
        baseMapper.updateById(groupPO);
        //禁用小组后，禁用小组下的用户
        List<String> userIds = sysUserGroupMapper.selectUserIdsByGroupId(saveDTO.getId());

        if (CollectionUtils.isNotEmpty(userIds)){
            boolean b=sysUserService.updateUserStatusByGroupStatus(userIds, saveDTO.getStatus());
        }
        //小组更新成功，已同步更新角色关联
        return Result.ok("00D02006");
    }

    // 批量删除小组
    @Override
    @Transactional
    public Result<String> deleteBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            //请选择需要删除的小组
            return Result.fail("00D02007");
        }

        // 1. 查询这些组下的所有用户ID
        LambdaQueryWrapper<SysUserGroupPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SysUserGroupPO::getGroupId, ids);
        List<SysUserGroupPO> userGroupList = sysUserGroupMapper.selectList(queryWrapper);

        if (userGroupList != null && !userGroupList.isEmpty()) {
            List<String> userIds = userGroupList.stream()
                    .map(SysUserGroupPO::getUserId)
                    .distinct()
                    .collect(Collectors.toList());

            // 2. 批量删除这些用户
            sysUserService.deleteBatch(userIds);
        }
        
        baseMapper.deleteBatchIds(ids);
        //小组删除成功
        return Result.ok("00D02008");
    }

    // 根据ID查询小组
    @Override
    public Result<SysGroupPO> getGroupById(String id) {
        SysGroupPO groupPO = baseMapper.selectById(id);
        if (groupPO == null) {
            //小组不存在
            return Result.fail("00D02005");
        }
        return Result.ok(groupPO);
    }

    // 根据ID删除小组
    @Override
    @Transactional
    public Result<String> deleteById(String id) {
        SysGroupPO groupPO = baseMapper.selectById(id);
        if (groupPO == null) {
            return Result.fail("00D02009");
        }

        // 1. 查询该组下的所有用户ID
        LambdaQueryWrapper<SysUserGroupPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUserGroupPO::getGroupId, id);
        List<SysUserGroupPO> userGroupList = sysUserGroupMapper.selectList(queryWrapper);

        if (userGroupList != null && !userGroupList.isEmpty()) {
            List<String> userIds = userGroupList.stream()
                    .map(SysUserGroupPO::getUserId)
                    .distinct()
                    .collect(Collectors.toList());

            // 2. 批量删除这些用户
            sysUserService.deleteBatch(userIds);
        }
        
        baseMapper.deleteById(id);
        //小组删除成功
        return Result.ok("00D02008");
    }
}