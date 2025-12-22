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
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysPermissionMapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionSaveDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionTreeDTO;
import com.persee.workstation.uv_inexus.utils.OperateLog.UserContext;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysPermissionPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.PermissionService;

import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionUpdateDTO;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

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
                return Result.fail("权限编码已存在");
            }
        }

        // 2. 检查权限名称是否已存在
        if (saveDTO.getPermName() != null && !saveDTO.getPermName().isEmpty()) {
            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("perm_name", saveDTO.getPermName());
            if (sysPermissionMapper.selectCount(queryWrapper) > 0) {
                return Result.fail("权限名称已存在");
            }
        }

        // 3. 检查路由路径是否已存在
        if (saveDTO.getPath() != null && !saveDTO.getPath().isEmpty()) {
            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("path", saveDTO.getPath());
            if (sysPermissionMapper.selectCount(queryWrapper) > 0) {
                return Result.fail("路由路径已存在");
            }
        }

        // 4. 检查前端组件路径是否已存在
        if (saveDTO.getComponent() != null && !saveDTO.getComponent().isEmpty()) {
            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("component", saveDTO.getComponent());
            if (sysPermissionMapper.selectCount(queryWrapper) > 0) {
                return Result.fail("前端组件路径已存在");
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
        String currentUserName = (currentUser != null && currentUser.getUsername() != null) ? currentUser.getUsername() : "admin";
        po.setCreateBy(currentUserName);
        po.setCreated(LocalDateTime.now());

        // 6. 执行插入
        sysPermissionMapper.insert(po);

        return Result.ok("新增权限成功");
    }

    @Override
    public Result<String> updatePermission(PermissionUpdateDTO updateDTO) {
        // 1. 检查记录是否存在
        SysPermissionPO existingPo = sysPermissionMapper.selectById(updateDTO.getId());
        if (existingPo == null) {
            return Result.fail("权限记录不存在");
        }

        // 2. 检查权限编码是否已存在 (排除自身)
        if (updateDTO.getPermCode() != null && !updateDTO.getPermCode().isEmpty()) {
            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("perm_code", updateDTO.getPermCode())
                    .ne("id", updateDTO.getId());
            if (sysPermissionMapper.selectCount(queryWrapper) > 0) {
                return Result.fail("权限编码已存在");
            }
        }

        // 3. 检查权限名称是否已存在 (排除自身)
        if (updateDTO.getPermName() != null && !updateDTO.getPermName().isEmpty()) {
            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("perm_name", updateDTO.getPermName())
                    .ne("id", updateDTO.getId());
            if (sysPermissionMapper.selectCount(queryWrapper) > 0) {
                return Result.fail("权限名称已存在");
            }
        }

        // 4. 检查路由路径是否已存在 (排除自身)
        if (updateDTO.getPath() != null && !updateDTO.getPath().isEmpty()) {
            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("path", updateDTO.getPath())
                    .ne("id", updateDTO.getId());
            if (sysPermissionMapper.selectCount(queryWrapper) > 0) {
                return Result.fail("路由路径已存在");
            }
        }

        // 5. 检查前端组件路径是否已存在 (排除自身)
        if (updateDTO.getComponent() != null && !updateDTO.getComponent().isEmpty()) {
            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("component", updateDTO.getComponent())
                    .ne("id", updateDTO.getId());
            if (sysPermissionMapper.selectCount(queryWrapper) > 0) {
                return Result.fail("前端组件路径已存在");
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
        String currentUserName = (currentUser != null && currentUser.getUsername() != null) ? currentUser.getUsername() : "admin";
        po.setUpdateBy(currentUserName);
        po.setUpdated(LocalDateTime.now());

        // 7. 执行更新
        sysPermissionMapper.updateById(po);

        return Result.ok("更新权限成功");
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

