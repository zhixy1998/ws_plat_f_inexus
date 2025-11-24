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

import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.alibaba.excel.util.StringUtils;
import com.persee.workstation.uv_inexus.utils.AESencryption.HmacMD5Util;
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
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PasswordUpdateDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserDetailDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserSaveDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysUserService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;
import com.persee.workstation.uv_inexus.utils.PasswordUtils.PasswordUtils;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUserPO> implements SysUserService {

    // @Autowired
    // private SysUserMapper sysUserMapper;

    @Autowired
    private SysUserGroupMapper sysUserGroupMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;
    // 注入小组Mapper用于校验小组是否存在
    @Autowired
    private SysGroupMapper sysGroupMapper;

    @Override
    public Result<PageView<SysUserPO>> getUserPage(Page<SysUserPO> page, UserQueryDTO queryDTO) {
        QueryWrapper<SysUserPO> queryWrapper = new QueryWrapper<>();

        // 条件查询
        if (queryDTO.getUsername() != null && !queryDTO.getUsername().isEmpty()) {
            queryWrapper.like("username", queryDTO.getUsername());
        }
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq("status", queryDTO.getStatus());
        }
        queryWrapper.orderByDesc("create_time");

        IPage<SysUserPO> iPage = baseMapper.selectPage(page, queryWrapper);
        PageView<SysUserPO> pageView = new PageView<>(iPage);
        return Result.ok(pageView);
    }


@Override
@Transactional
public Result<String> addUser(UserSaveDTO saveDTO) {
    // 1. 检查用户名是否已存在
    QueryWrapper<SysUserPO> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("username", saveDTO.getUsername());
    if (baseMapper.selectCount(queryWrapper) > 0) {
        //用户名已存在
        return Result.fail(YamlMessageSource.getMessageStatic("00601001"));
    }

    saveDTO.setPassword(HmacMD5Util.hmacMD5(saveDTO.getPassword()));
    // 2. 校验小组是否存在
    String groupId = saveDTO.getGroupId();
    if (groupId == null || groupId.isEmpty()) {
        //小组ID不能为空
        return Result.fail(YamlMessageSource.getMessageStatic("00500001"));
    }
    SysGroupPO groupPO = sysGroupMapper.selectById(groupId);
    if (groupPO == null) {
        //该小组不存在，请刷新重试！
        return Result.fail(YamlMessageSource.getMessageStatic("00601002"));
    }

    // 3. 封装用户信息并保存
    SysUserPO userPO = new SysUserPO();
    BeanUtils.copyProperties(saveDTO, userPO);

    // 单独处理 roleId 字段，将其从 Integer 转换为 String 后设置到 DTO 中
    if (saveDTO.getRoleId() != null) {
        userPO.setRoleId(Integer.valueOf(saveDTO.getRoleId()));
    }

    // 密码加密
//    userPO.setPassword(MD5Util.md5(saveDTO.getPassword()));
    userPO.setCreateBy("admin");
    userPO.setCreated(LocalDateTime.now());
    baseMapper.insert(userPO);  // 保存用户，生成用户ID

//    // 4.检验所属小组是否为禁用
//    if (groupPO.getStatus() != null && groupPO.getStatus() == 0) {
//        userPO.setStatus(0);  // 设置用户状态为禁用
//    }
    // 5. 添加用户-小组关联
    SysUserGroupPO userGroupPO = new SysUserGroupPO();
    userGroupPO.setUserId(userPO.getId());  // 绑定用户ID
    userGroupPO.setGroupId(groupId);  // 绑定小组ID
    sysUserGroupMapper.insert(userGroupPO);  // 保存关联关系
    //用户创建成功，已关联小组
    return Result.ok(YamlMessageSource.getMessageStatic("00601003"));
}


    @Override
    @Transactional
    public Result<String> updateUser(UserSaveDTO saveDTO) {
        // 1. 校验用户是否存在
        SysUserPO userPO = baseMapper.selectById(saveDTO.getId());
        if (userPO == null) {
            //用户不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00000001"));
        }

        // 2. 校验小组是否存在
        String groupId = saveDTO.getGroupId();
        if (groupId == null || groupId.isEmpty()) {
            //小组ID不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00500001"));
        }
        SysGroupPO groupPO = sysGroupMapper.selectById(groupId);
        if (groupPO == null) {
            //该小组不存在，请刷新重试！
            return Result.fail(YamlMessageSource.getMessageStatic("00601002"));
        }

        // 3. 更新用户基本信息
        BeanUtils.copyProperties(saveDTO, userPO, "password");  // 排除密码
        // 密码更新（若有新密码）
        if (saveDTO.getPassword() != null && !saveDTO.getPassword().isEmpty()) {
            userPO.setPassword(HmacMD5Util.hmacMD5(saveDTO.getPassword()));
        }
        userPO.setUpdateBy("admin");
        userPO.setUpdated(LocalDateTime.now());
        baseMapper.updateById(userPO);  // 更新用户信息

        // 4. 同步更新用户-小组关联（先删旧关联，再添新关联）
        String userId = saveDTO.getId();  // 用户ID

        // 4.1 删除旧关联
        QueryWrapper<SysUserGroupPO> groupQueryWrapper = new QueryWrapper<>();
        groupQueryWrapper.eq("user_id", userId);
        sysUserGroupMapper.delete(groupQueryWrapper);  // 删除该用户所有旧关联

        // 4.2 添加新关联
        SysUserGroupPO userGroupPO = new SysUserGroupPO();
        userGroupPO.setUserId(userId);
        userGroupPO.setGroupId(groupId);
        sysUserGroupMapper.insert(userGroupPO);  // 保存新关联
        //用户更新成功，已同步更新小组关联
        return Result.ok(YamlMessageSource.getMessageStatic("00601004"));
    }

    @Override
    @Transactional
    public Result<String> deleteBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            //请选择需要删除的用户
            return Result.fail(YamlMessageSource.getMessageStatic("00601005"));
        }

        // 1. 先删除用户的小组关联（避免冗余数据）
        for (String userId : ids) {
            QueryWrapper<SysUserGroupPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId);
            sysUserGroupMapper.delete(queryWrapper);
        }

        // 2. 再删除用户
        baseMapper.deleteBatchIds(ids);
        //用户删除成功
        return Result.ok(YamlMessageSource.getMessageStatic("00601006"));
    }

    @Override
    public Result<UserDetailDTO> getUserById(String id) {
        // 1. 查询用户基本信息
        SysUserPO userPO = baseMapper.selectById(id);
        if (userPO == null) {
            //用户不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00000001"));
        }

        // 2. 转换用户信息到DTO
        UserDetailDTO userDetail = new UserDetailDTO();
        BeanUtils.copyProperties(userPO, userDetail);

        // 3. 查询用户关联的小组（用户-小组关联表）
        List<SysUserGroupPO> userGroups = sysUserGroupMapper.selectList(
            new LambdaQueryWrapper<SysUserGroupPO>()
                .eq(SysUserGroupPO::getUserId, id)
        );

        // 4. 若有小组，查询小组信息
        if (!userGroups.isEmpty()) {
            // 取第一个关联的小组（若需多组，可循环处理）
            SysUserGroupPO firstUserGroup = userGroups.get(0);
            SysGroupPO groupPO = sysGroupMapper.selectById(firstUserGroup.getGroupId());
            
            if (groupPO != null) {
                // 复制小组信息到DTO
                userDetail.setGroupId(groupPO.getId());
                userDetail.setGroupName(groupPO.getGroupName());
                userDetail.setGroupDescription(groupPO.getDescription());

                // 5. 根据小组关联的角色ID查询角色信息
                SysRolePO rolePO = sysRoleMapper.selectById(groupPO.getRoleId());
                if (rolePO != null) {
                    // 复制角色信息到DTO
                    userDetail.setRoleId(rolePO.getId());
                    userDetail.setRoleName(rolePO.getRoleName());
                    userDetail.setRoleCode(rolePO.getRoleCode());
                    userDetail.setRoleDescription(rolePO.getDescription());
                }
            }
        }

        return Result.ok(userDetail);
    }

    @Override
    @Transactional
    public Result<String> deleteById(String id) {
        // 1. 校验用户是否存在
        SysUserPO userPO = baseMapper.selectById(id);
        if (userPO == null) {
            //用户不存在，删除失败
            return Result.fail(YamlMessageSource.getMessageStatic("00601007"));
        }

        // 2. 先删除该用户的小组关联
        QueryWrapper<SysUserGroupPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", id);
        sysUserGroupMapper.delete(queryWrapper);

        // 3. 再删除用户
        baseMapper.deleteById(id);
        //用户删除成功
        return Result.ok(YamlMessageSource.getMessageStatic("00601006"));
    }

    @Override
    @Transactional
    public Result<String> updatePassword(PasswordUpdateDTO updateDTO) {
        // 1. 校验用户是否存在
        SysUserPO userPO = baseMapper.selectById(updateDTO.getUserId());
        if (userPO == null) {
            //用户不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00000001"));
        }
        
        // 2. 验证旧密码是否正确
        if (!StringUtils.equals(HmacMD5Util.hmacMD5(updateDTO.getOldPassword()), userPO.getPassword())) {
          return Result.fail(YamlMessageSource.getMessageStatic("00601008"));
        }
        
        // 3. 检查新密码是否与旧密码相同
        if (StringUtils.equals(HmacMD5Util.hmacMD5(updateDTO.getNewPassword()), userPO.getPassword())) {
            //新密码不能与旧密码相同
            return Result.fail(YamlMessageSource.getMessageStatic("00601009"));
        }

        // 4. 校验确认密码与新密码是否一致
        if (!updateDTO.getNewPassword().equals(updateDTO.getConfirmNewPassword())) {
            //新密码与确认密码不一致，请重新输入
            return Result.fail(YamlMessageSource.getMessageStatic("0060100A"));
        }
        
        // 5. 更新密码
        userPO.setPassword(HmacMD5Util.hmacMD5(updateDTO.getNewPassword()));
        userPO.setUpdateBy("admin"); // 实际应用中应该是当前登录用户
        userPO.setUpdated(LocalDateTime.now());
        baseMapper.updateById(userPO);
        //密码修改成功
        return Result.ok(YamlMessageSource.getMessageStatic("0060100B"));
    }

    @Override
    @Transactional
    public Result<String> resetPassword(String userId) {
        // 1. 校验用户是否存在
        SysUserPO userPO = baseMapper.selectById(userId);
        if (userPO == null) {
            //用户不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00000001"));
        }
        
        // 2. 定义默认密码
        String defaultPassword = "123456";
        
        // 3. 检查默认密码是否与当前密码相同
        if (StringUtils.equals(HmacMD5Util.hmacMD5(defaultPassword), userPO.getPassword())) {
          //当前密码已是默认密码，无需重置
            return Result.fail(YamlMessageSource.getMessageStatic("0060100C"));
        }
        
        // 4. 更新密码（加密存储）
        userPO.setPassword(HmacMD5Util.hmacMD5(defaultPassword));
        userPO.setUpdateBy("admin"); // 实际应用中应该是当前登录用户
        userPO.setUpdated(LocalDateTime.now());
        baseMapper.updateById(userPO);
        //密码已重置为默认值123456，请提醒用户及时修改
        return Result.ok(YamlMessageSource.getMessageStatic("0060100D"));
    }
}