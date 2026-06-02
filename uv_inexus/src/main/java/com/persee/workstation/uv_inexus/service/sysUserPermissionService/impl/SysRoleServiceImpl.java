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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysRoleMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.GroupTreeDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionTreeDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleSaveDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.RoleTreeDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserTreeDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysGroupService;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysRoleService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRolePO> implements SysRoleService {

    @Autowired
    private SysUserGroupMapper sysUserGroupMapper;
    
    @Autowired
    private SysGroupMapper sysGroupMapper;
    
    @Autowired
    private SystemUserManageDAO systemUserManageDAO;
    
    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Autowired
    private SysGroupService sysGroupService;
    
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
            //角色编码已存在
            return Result.fail("00D03001");
        }
        
        SysRolePO rolePO = new SysRolePO();
        BeanUtils.copyProperties(saveDTO, rolePO);
        // rolePO.setCreateTime(LocalDateTime.now());
        // rolePO.setUpdateTime(LocalDateTime.now());
        rolePO.setCreateBy("amdin");
        rolePO.setCreated(LocalDateTime.now());
        baseMapper.insert(rolePO);
        //角色创建成功
        return Result.ok("00D03002");
    }

    @Override
    @Transactional
    public Result<String> updateRole(RoleSaveDTO saveDTO) {
        SysRolePO rolePO = baseMapper.selectById(saveDTO.getId());
        if (rolePO == null) {
            //角色不存在
            return Result.fail("00D03003");
        }
        
        // 检查角色编码是否重复（排除当前角色）
        QueryWrapper<SysRolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_code", saveDTO.getRoleCode())
                   .ne("id", saveDTO.getId());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            //角色编码已存在
            return Result.fail("00D03001");
        }
        
        BeanUtils.copyProperties(saveDTO, rolePO);
        // rolePO.setUpdateTime(LocalDateTime.now());
        rolePO.setUpdateBy("amdin");
        rolePO.setUpdated(LocalDateTime.now());
        baseMapper.updateById(rolePO);
        //角色更新成功
        return Result.ok("00D03004");
    }

    @Override
    @Transactional
    public Result<String> deleteBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            //请选择需要删除的角色
            return Result.fail("00D03005");
        }

        // 1. 查询这些角色下的所有组ID
        LambdaQueryWrapper<SysGroupPO> groupQueryWrapper = new LambdaQueryWrapper<>();
        groupQueryWrapper.in(SysGroupPO::getRoleId, ids);
        List<SysGroupPO> groups = sysGroupMapper.selectList(groupQueryWrapper);

        if (groups != null && !groups.isEmpty()) {
            List<String> groupIds = groups.stream()
                    .map(SysGroupPO::getId)
                    .distinct()
                    .collect(Collectors.toList());

            // 2. 批量删除这些组（SysGroupService中已实现级联删除组下用户）
            sysGroupService.deleteBatch(groupIds);
        }
        
        baseMapper.deleteBatchIds(ids);
        //角色删除成功
        return Result.ok("00D03006");
    }

    @Override
    public Result<SysRolePO> getRoleById(String id) {
        SysRolePO rolePO = baseMapper.selectById(id);
        if (rolePO == null) {
            //角色不存在
            return Result.fail("00D03003");
        }
        return Result.ok(rolePO);
    }

    @Override
    @Transactional
    public Result<String> deleteById(String id) {
        // 1. 校验角色是否存在（修正描述错误：将“用户”改为“角色”）
        SysRolePO rolePO = baseMapper.selectById(id);
        if (rolePO == null) {
            //角色不存在，删除失败
            return Result.fail("00D03007");
        }

        // 2. 查询该角色下的所有组ID
        LambdaQueryWrapper<SysGroupPO> groupQueryWrapper = new LambdaQueryWrapper<>();
        groupQueryWrapper.eq(SysGroupPO::getRoleId, id);
        List<SysGroupPO> groups = sysGroupMapper.selectList(groupQueryWrapper);

        if (groups != null && !groups.isEmpty()) {
            List<String> groupIds = groups.stream()
                    .map(SysGroupPO::getId)
                    .distinct()
                    .collect(Collectors.toList());

            // 3. 批量删除这些组（级联删除组下用户）
            sysGroupService.deleteBatch(groupIds);
        }
        
        // 4. 执行删除
        baseMapper.deleteById(id);
        //角色删除成功
        return Result.ok("00D03006");  // 修正描述错误
    }

    @Override
    public Result<RoleTreeDTO> getUserRoleGroupTree(String userId) {
        try {
            // 1. 根据用户ID查询所属的用户组
            LambdaQueryWrapper<SysUserGroupPO> userGroupWrapper = new LambdaQueryWrapper<>();
            userGroupWrapper.eq(SysUserGroupPO::getUserId, userId);
            List<SysUserGroupPO> userGroups = sysUserGroupMapper.selectList(userGroupWrapper);
            
            if (userGroups == null || userGroups.isEmpty()) {
                //该用户未分配任何组
                return Result.fail("00D03008");
            }
            
            // 2. 从用户组中获取第一个组的角色ID（假设用户只属于同一角色的组）
            String firstGroupId = userGroups.get(0).getGroupId();
            SysGroupPO firstGroup = sysGroupMapper.selectById(firstGroupId);
            if (firstGroup == null) {
                //用户所属组不存在
                return Result.fail("00D03009");
            }
            
            String roleId = firstGroup.getRoleId();
            if (roleId == null) {
                //用户所属组未分配角色
                return Result.fail("00D0300A");
            }
            
            // 3. 查询角色信息
            SysRolePO role = baseMapper.selectById(roleId);
            if (role == null) {
                //角色不存在
                return Result.fail("00D03003");
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
                    List<SysUserPO> users = systemUserManageDAO.selectList(userWrapper);
                    
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
            //"查询失败：" + e.getMessage()
            return Result.fail("00D0300B",e.getMessage());
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
                //角色不存在
                return Result.fail("00D03003");
            }
            
            // 2. 校验角色状态（只查询启用的角色）
            if (rolePO.getStatus() != 1) {
                //该角色已禁用，无法查询下属组
                return Result.fail("00D0300C");
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
            //"查询失败：" + e.getMessage()
            return Result.fail("00D0300B",e.getMessage());
        }
    }

    @Override
    public Result<List<PermissionTreeDTO>> getPermissionTree(List<Integer> permTypeList, String roleId, Integer quickConfigType) {
        try {
            SysRolePO rolePO = baseMapper.selectById(roleId);
            if (rolePO == null) {
                return Result.fail("00D03003");
            }

            String permIdsStr = rolePO.getPermIds();
            Set<String> rolePermIds = new HashSet<>();
            if (permIdsStr != null && !permIdsStr.trim().isEmpty()) {
                rolePermIds = Arrays.stream(permIdsStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toSet());
            }

            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByAsc("sort");
            if (permTypeList != null && !permTypeList.isEmpty()) {
                queryWrapper.in("perm_type", permTypeList);
            }
            if (quickConfigType != null) {
                queryWrapper.eq("quick_config_type", quickConfigType);
            }
            List<SysPermissionPO> permissions = sysPermissionMapper.selectList(queryWrapper);
            if (permissions == null || permissions.isEmpty()) {
                return Result.ok(new ArrayList<>());
            }
            List<PermissionTreeDTO> tree = buildPermissionTree(permissions, rolePermIds);
//            System.out.println("Permission Tree: " + JSON.toJSONString(tree));
            return Result.ok(tree);
        } catch (Exception e) {
            return Result.fail("00605005",e.getMessage());
        }
    }

    private List<PermissionTreeDTO> buildPermissionTree(List<SysPermissionPO> permissions, Set<String> rolePermIds) {
        List<PermissionTreeDTO> dtoList = permissions.stream().map(po -> {
            PermissionTreeDTO dto = new PermissionTreeDTO();
            BeanUtils.copyProperties(po, dto);
            // 如果是首页权限，isHave始终返回true
            dto.setIsHave("首页".equals(po.getTitle()) || rolePermIds.contains(po.getId()));
            dto.setChildren(new ArrayList<>());
            return dto;
        }).collect(Collectors.toList());

        Map<String, PermissionTreeDTO> dtoMap = dtoList.stream()
                .collect(Collectors.toMap(PermissionTreeDTO::getId, dto -> dto, (existing, replacement) -> existing));

        List<PermissionTreeDTO> rootNodes = new ArrayList<>();
        for (PermissionTreeDTO currentDto : dtoList) {
            Integer parentId = currentDto.getParentId();
            String parentIdStr = parentId != null ? parentId.toString() : "0";
            if ("0".equals(parentIdStr) || !dtoMap.containsKey(parentIdStr)) {
                rootNodes.add(currentDto);
            } else {
                PermissionTreeDTO parentDto = dtoMap.get(parentIdStr);
                if (parentDto.getChildren() == null) {
                    parentDto.setChildren(new ArrayList<>());
                }
                parentDto.getChildren().add(currentDto);
            }
        }

        for (PermissionTreeDTO root : rootNodes) {
            calculateParentIsHave(root);
        }

        sortPermissionTree(rootNodes);
        return rootNodes;
    }

    private boolean calculateParentIsHave(PermissionTreeDTO node) {
        if (CollectionUtils.isEmpty(node.getChildren())) {
            return node.getIsHave();
        }
        boolean allChildrenHave = true;
        for (PermissionTreeDTO child : node.getChildren()) {
            if (!calculateParentIsHave(child)) {
                allChildrenHave = false;
            }
        }
        node.setIsHave(allChildrenHave);
        return allChildrenHave;
    }

    private void sortPermissionTree(List<PermissionTreeDTO> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        nodes.sort(Comparator.comparingInt(node -> {
            Integer sort = node.getSort();
            return sort != null ? sort : 0;
        }));
        for (PermissionTreeDTO node : nodes) {
            sortPermissionTree(node.getChildren());
        }
    }

    @Override
    @Transactional
    public Result<String> updateRolePermissions(String roleId, List<String> permIds) {
        // 1. 校验角色是否存在
        SysRolePO rolePO = baseMapper.selectById(roleId);
        if (rolePO == null) {
            //角色不存在
            return Result.fail("00D03003");
        }

        // 2. 更新权限字段（直接存储逗号分隔的字符串，去除空格）
        String permIdsStr = "";
        if (permIds != null && !permIds.isEmpty()) {
            // 去除每个元素的空格，并用逗号连接
            permIdsStr = permIds.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
        }
        rolePO.setPermIds(permIdsStr);
        
        rolePO.setUpdateBy("admin");
        rolePO.setUpdated(LocalDateTime.now());
        
        baseMapper.updateById(rolePO);
        
        // 角色更新成功
        return Result.ok("00D03004");
    }
}
