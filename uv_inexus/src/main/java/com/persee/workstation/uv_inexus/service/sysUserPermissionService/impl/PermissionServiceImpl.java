package com.persee.workstation.uv_inexus.service.sysUserPermissionService.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysRoleMapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionDeleteDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionSaveDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionTreeDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionUpdateDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysPermissionPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.PermissionService;
import com.persee.workstation.uv_inexus.utils.OperateLog.UserContext;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private SysPermissionMapper sysPermissionMapper;
    
    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public Result<List<PermissionTreeDTO>> getAllPermissionTree() {
        QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("sort");
        List<SysPermissionPO> permissions = sysPermissionMapper.selectList(queryWrapper);
        if (permissions == null || permissions.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }
        List<PermissionTreeDTO> tree = buildPermissionTree(permissions);
        return Result.ok(tree);
    }

    @Override
    public Result<String> addPermission(PermissionSaveDTO saveDTO) {
        // 1. 检查权限编码是否已存在
        if (saveDTO.getPermCode() != null && !saveDTO.getPermCode().isEmpty()) {
            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("perm_code", saveDTO.getPermCode());
            if (sysPermissionMapper.selectCount(queryWrapper) > 0) {
                return Result.fail("00D02004");
            }
        }

        // 2. 检查权限名称是否已存在
        if (saveDTO.getPermName() != null && !saveDTO.getPermName().isEmpty()) {
            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("perm_name", saveDTO.getPermName());
            if (sysPermissionMapper.selectCount(queryWrapper) > 0) {
                return Result.fail("00D02005");
            }
        }

        // 3. 检查路由路径是否已存在
        if (saveDTO.getPath() != null && !saveDTO.getPath().isEmpty()) {
            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("path", saveDTO.getPath());
            if (sysPermissionMapper.selectCount(queryWrapper) > 0) {
                return Result.fail("00D02006");
            }
        }

        // 4. 检查前端组件路径是否已存在
        if (saveDTO.getComponent() != null && !saveDTO.getComponent().isEmpty()) {
            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("component", saveDTO.getComponent());
            if (sysPermissionMapper.selectCount(queryWrapper) > 0) {
                return Result.fail("00D02007");
            }
        }

        // 5. 转换并处理默认值
        SysPermissionPO po = new SysPermissionPO();
        BeanUtils.copyProperties(saveDTO, po);

        // 核心逻辑：parentId 为空默认给 0
        if (po.getParentId() == null) {
            po.setParentId(0);
        }

        // 其他默认值
        if (po.getStatus() == null) {
            po.setStatus(1); // 默认启用
        }
        
        LoginUserInfoVO currentUser = UserContext.getCurrentUser();
        String currentUserName = (currentUser != null && currentUser.getUsername() != null) ? currentUser.getUsername() : Constants.SUPER_USER_NAME;
        po.setCreateBy(currentUserName);
        po.setCreated(LocalDateTime.now());

        // 6. 执行插入
        sysPermissionMapper.insert(po);

        // 7. 更新SUPER_ADMIN角色的权限列表
        updateSuperAdminPermission(po.getId());

        return Result.ok("00D02009");
    }

    private void updateSuperAdminPermission(String permissionId) {
        // 查询role_code=SUPER_ADMIN的角色
        QueryWrapper<com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO> roleQueryWrapper = new QueryWrapper<>();
        roleQueryWrapper.eq("role_code", "SUPER_ADMIN");
        com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO superAdminRole = sysRoleMapper.selectOne(roleQueryWrapper);
        
        if (superAdminRole != null) {
            String permIds = superAdminRole.getPermIds();
            if (permIds == null || permIds.isEmpty()) {
                // 如果perm_ids为空，直接设置为新权限ID
                permIds = permissionId;
            } else {
                // 如果perm_ids不为空，追加新权限ID
                permIds += "," + permissionId;
            }
            
            // 更新perm_ids字段
            superAdminRole.setPermIds(permIds);
            sysRoleMapper.updateById(superAdminRole);
        }
    }

    private void removePermissionFromSuperAdmin(String permissionId) {
        // 查询role_code=SUPER_ADMIN的角色
        QueryWrapper<com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO> roleQueryWrapper = new QueryWrapper<>();
        roleQueryWrapper.eq("role_code", "SUPER_ADMIN");
        com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO superAdminRole = sysRoleMapper.selectOne(roleQueryWrapper);
        
        if (superAdminRole != null) {
            String permIds = superAdminRole.getPermIds();
            if (permIds != null && !permIds.isEmpty()) {
                // 分割权限ID列表
                String[] permissionIds = permIds.split(",");
                StringBuilder newPermIds = new StringBuilder();
                
                // 重建权限ID列表，排除要删除的权限ID
                for (String id : permissionIds) {
                    if (!id.equals(permissionId)) {
                        if (newPermIds.length() > 0) {
                            newPermIds.append(",");
                        }
                        newPermIds.append(id);
                    }
                }
                
                // 更新perm_ids字段
                superAdminRole.setPermIds(newPermIds.toString());
                sysRoleMapper.updateById(superAdminRole);
            }
        }
    }

    @Override
    public Result<String> updatePermission(PermissionUpdateDTO updateDTO) {
        // 1. 检查记录是否存在
        SysPermissionPO existingPo = sysPermissionMapper.selectById(updateDTO.getId());
        if (existingPo == null) {
            return Result.fail("00D02008");
        }

        // 2. 检查权限编码是否已存在 (排除自身)
        if (updateDTO.getPermCode() != null && !updateDTO.getPermCode().isEmpty()) {
            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("perm_code", updateDTO.getPermCode())
                    .ne("id", updateDTO.getId());
            if (sysPermissionMapper.selectCount(queryWrapper) > 0) {
                return Result.fail("00D02004");
            }
        }

        // 3. 检查权限名称是否已存在 (排除自身)
        if (updateDTO.getPermName() != null && !updateDTO.getPermName().isEmpty()) {
            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("perm_name", updateDTO.getPermName())
                    .ne("id", updateDTO.getId());
            if (sysPermissionMapper.selectCount(queryWrapper) > 0) {
                return Result.fail("00D02005");
            }
        }

        // 4. 检查路由路径是否已存在 (排除自身)
        if (updateDTO.getPath() != null && !updateDTO.getPath().isEmpty()) {
            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("path", updateDTO.getPath())
                    .ne("id", updateDTO.getId());
            if (sysPermissionMapper.selectCount(queryWrapper) > 0) {
                return Result.fail("00D02006");
            }
        }

        // 5. 检查前端组件路径是否已存在 (排除自身)
        if (updateDTO.getComponent() != null && !updateDTO.getComponent().isEmpty()) {
            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("component", updateDTO.getComponent())
                    .ne("id", updateDTO.getId());
            if (sysPermissionMapper.selectCount(queryWrapper) > 0) {
                return Result.fail("00D02007");
            }
        }
        // 6. 转换并处理默认值
        SysPermissionPO po = new SysPermissionPO();
        BeanUtils.copyProperties(updateDTO, po);

        // 核心逻辑：parentId 为空默认给 0
        if (po.getParentId() == null) {
            po.setParentId(0);
        }

        LoginUserInfoVO currentUser = UserContext.getCurrentUser();
        String currentUserName = (currentUser != null && currentUser.getUsername() != null) ? currentUser.getUsername() : Constants.SUPER_USER_NAME;
        po.setUpdateBy(currentUserName);
        po.setUpdated(LocalDateTime.now());

        // 7. 执行更新
        sysPermissionMapper.updateById(po);

        return Result.ok("00D02010");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deletePermission(PermissionDeleteDTO deleteDTO) {
        // 1. 检查记录是否存在
        SysPermissionPO existingPo = sysPermissionMapper.selectById(deleteDTO.getId());
        if (existingPo == null) {
            return Result.fail("00D02008");
        }

        // 2. 递归删除当前节点及其所有子节点
        deletePermissionAndChildren(deleteDTO.getId());

        return Result.ok("00D02011");
    }

    private void deletePermissionAndChildren(Integer parentId) {
        // 查找所有子节点
        QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId);
        List<SysPermissionPO> children = sysPermissionMapper.selectList(queryWrapper);

        // 递归删除所有子节点
        if (children != null && !children.isEmpty()) {
            for (SysPermissionPO child : children) {
                deletePermissionAndChildren(Integer.parseInt(child.getId()));
            }
        }

        // 从SUPER_ADMIN角色的权限列表中移除当前权限
        removePermissionFromSuperAdmin(parentId.toString());
        
        // 删除当前节点
        sysPermissionMapper.deleteById(parentId);
    }

    @Override
    public Result<List<PermissionTreeDTO>> getPermissionList(PermissionQueryDTO queryDTO) {
        QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
        
        // 构建查询条件
        if (queryDTO != null) {
            if (queryDTO.getTitle() != null && !queryDTO.getTitle().isEmpty()) {
                queryWrapper.like("title", queryDTO.getTitle());
            }
            if (queryDTO.getPermName() != null && !queryDTO.getPermName().isEmpty()) {
                queryWrapper.like("perm_name", queryDTO.getPermName());
            }
            if (queryDTO.getPermCode() != null && !queryDTO.getPermCode().isEmpty()) {
                queryWrapper.like("perm_code", queryDTO.getPermCode());
            }
            if (queryDTO.getPermType() != null) {
                queryWrapper.eq("perm_type", queryDTO.getPermType());
            }
            if (queryDTO.getParentId() != null) {
                queryWrapper.eq("parent_id", queryDTO.getParentId());
            }
            if (queryDTO.getStatus() != null) {
                queryWrapper.eq("status", queryDTO.getStatus());
            }
            if (queryDTO.getHidden() != null) {
                queryWrapper.eq("hidden", queryDTO.getHidden());
            }
            if (queryDTO.getNoAuth() != null) {
                queryWrapper.eq("no_auth", queryDTO.getNoAuth());
            }
            if (queryDTO.getQuickConfigType() != null) {
                queryWrapper.eq("quick_config_type", queryDTO.getQuickConfigType());
            }
        }
        
        // 按排序号排序
        queryWrapper.orderByAsc("sort");
        
        List<SysPermissionPO> permissions = sysPermissionMapper.selectList(queryWrapper);
        List<PermissionTreeDTO> tree = buildPermissionTree(permissions);
        return Result.ok(tree);
    }


    private List<PermissionTreeDTO> buildPermissionTree(List<SysPermissionPO> permissions) {
        List<PermissionTreeDTO> dtoList = permissions.stream().map(po -> {
            PermissionTreeDTO dto = new PermissionTreeDTO();
            BeanUtils.copyProperties(po, dto);
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

        sortPermissionTree(rootNodes);
        return rootNodes;
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
}

