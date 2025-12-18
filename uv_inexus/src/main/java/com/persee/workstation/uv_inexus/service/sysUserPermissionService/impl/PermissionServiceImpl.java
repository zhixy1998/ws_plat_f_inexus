package com.persee.workstation.uv_inexus.service.sysUserPermissionService.impl;

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
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionTreeDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysPermissionPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.PermissionService;

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

