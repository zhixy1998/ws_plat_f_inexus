package com.persee.workstation.uv_inexus.service.sysUserPermissionService.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionTreeDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.UserPermissionService;

@Service
public class UserPermissionServiceImpl implements UserPermissionService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysUserGroupMapper sysUserGroupMapper;

    @Autowired
    private SysGroupPermissionMapper sysGroupPermissionMapper;

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Override
    public Result<List<PermissionDTO>> getUserPermissions(String userId) {
        // 1. 校验用户是否存在
        SysUserPO userPO = sysUserMapper.selectById(userId);
        if (userPO == null) {
            return Result.fail("用户不存在");
        }

        // 2. 查用户关联的小组ID
        QueryWrapper<SysUserGroupPO> userGroupQuery = new QueryWrapper<>();
        userGroupQuery.eq("user_id", userId);
        List<SysUserGroupPO> userGroupList = sysUserGroupMapper.selectList(userGroupQuery);
        if (userGroupList.isEmpty()) {
            return Result.fail("该用户未关联任何小组");
        }
        List<String> groupIds = userGroupList.stream()
                .map(SysUserGroupPO::getGroupId)
                .collect(Collectors.toList());

        // 3. 查小组关联的权限ID（去重）
        QueryWrapper<SysGroupPermissionPO> groupPermQuery = new QueryWrapper<>();
        groupPermQuery.in("group_id", groupIds);
        List<SysGroupPermissionPO> groupPermList = sysGroupPermissionMapper.selectList(groupPermQuery);
        if (groupPermList.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }
        List<String> permIds = groupPermList.stream()
                .map(SysGroupPermissionPO::getPermId)
                .distinct()
                .collect(Collectors.toList());

        // 4. 查权限详情并排除"首页"
        QueryWrapper<SysPermissionPO> permQuery = new QueryWrapper<>();
        permQuery.in("id", permIds)
                .ne("title", "首页")
                .orderByAsc("sort");
        List<SysPermissionPO> permissionList = sysPermissionMapper.selectList(permQuery);

        // 5. 转换为DTO并处理排序
        List<PermissionDTO> flatPermissionDTOs = permissionList.stream()
                .map(perm -> {
                    PermissionDTO dto = new PermissionDTO();
                    BeanUtils.copyProperties(perm, dto);
                    return dto;
                })
                .filter(dto -> dto.getTitle() == null || !"首页".equals(dto.getTitle()))
                .sorted((d1, d2) -> {
                    int sort1 = (d1.getSort() == null) ? 0 : d1.getSort();
                    int sort2 = (d2.getSort() == null) ? 0 : d2.getSort();
                    return Integer.compare(sort1, sort2);
                })
                .collect(Collectors.toList());

        // 6. 构建树形结构
        List<PermissionDTO> treePermissionDTOs = buildPermissionTree(flatPermissionDTOs);

        return Result.ok(treePermissionDTOs);
    }

    private List<PermissionDTO> buildPermissionTree(List<PermissionDTO> flatPermissions) {
        Map<String, PermissionDTO> permissionMap = flatPermissions.stream()
                .collect(Collectors.toMap(
                        PermissionDTO::getId,
                        dto -> dto,
                        (existing, replacement) -> existing
                ));

        List<PermissionDTO> rootPermissions = new ArrayList<>();
        for (PermissionDTO currentPerm : flatPermissions) {
            Integer parentId = currentPerm.getParentId();
            if (parentId == null || parentId == 0 || !permissionMap.containsKey(parentId.toString())) {
                rootPermissions.add(currentPerm);
            } else {
                PermissionDTO parentPerm = permissionMap.get(parentId.toString());
                if (parentPerm.getChildren() == null) {
                    parentPerm.setChildren(new ArrayList<>());
                }
                parentPerm.getChildren().add(currentPerm);
                parentPerm.setChildren(sortPermissionBySort(parentPerm.getChildren()));
            }
        }

        return sortPermissionBySort(rootPermissions);
    }

    private List<PermissionDTO> sortPermissionBySort(List<PermissionDTO> permissions) {
        return permissions.stream()
                .sorted((p1, p2) -> {
                    int sort1 = (p1.getSort() == null) ? 0 : p1.getSort();
                    int sort2 = (p2.getSort() == null) ? 0 : p2.getSort();
                    return Integer.compare(sort1, sort2);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Result<List<String>> getRootPermissionPaths(String userId) {
        SysUserPO userPO = sysUserMapper.selectById(userId);
        if (userPO == null) {
            return Result.fail("用户不存在");
        }

        QueryWrapper<SysUserGroupPO> userGroupQuery = new QueryWrapper<>();
        userGroupQuery.eq("user_id", userId);
        List<SysUserGroupPO> userGroupList = sysUserGroupMapper.selectList(userGroupQuery);
        if (userGroupList.isEmpty()) {
            return Result.fail("该用户未关联任何小组");
        }

        List<String> groupIds = userGroupList.stream()
                .map(SysUserGroupPO::getGroupId)
                .collect(Collectors.toList());

        QueryWrapper<SysGroupPermissionPO> groupPermQuery = new QueryWrapper<>();
        groupPermQuery.in("group_id", groupIds);
        List<SysGroupPermissionPO> groupPermList = sysGroupPermissionMapper.selectList(groupPermQuery);
        if (groupPermList.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }

        List<String> permIds = groupPermList.stream()
                .map(SysGroupPermissionPO::getPermId)
                .distinct()
                .collect(Collectors.toList());

        QueryWrapper<SysPermissionPO> permQuery = new QueryWrapper<>();
        permQuery.in("id", permIds)
                .eq("parent_id", 0)
                .ne("title", "首页")
                .orderByAsc("sort")
                .select("path");
        List<SysPermissionPO> permissionList = sysPermissionMapper.selectList(permQuery);

        List<String> rootPermissionPaths = permissionList.stream()
                .map(SysPermissionPO::getPath)
                .filter(path -> path != null && !path.trim().isEmpty())
                .collect(Collectors.toList());

        return Result.ok(rootPermissionPaths);
    }

    /**
     * 核心方法：按用户→小组→权限逻辑查询树形权限结构
     */
    @Override
    public Result<List<PermissionTreeDTO>> getPermissionTreeByTypeAndUser(List<Integer> permTypeList, String groupId) {
        try {
            // 1. 校验用户ID有效性
            // if (!StringUtils.hasText(userId)) {
            //     return Result.fail("用户ID不能为空");
            // }
            
            // // 2. 验证用户存在性
            // SysUserPO userPO = sysUserMapper.selectById(userId.trim());
            // if (userPO == null) {
            //     return Result.fail("用户不存在（userId：" + userId + "）");
            // }

            // // 3. 查询用户关联的所有小组ID
            // List<String> groupIds = getGroupIdsByUserId(userId.trim());
            // if (CollectionUtils.isEmpty(groupIds)) {
            //     return Result.fail("用户未关联任何小组（userId：" + userId + "）");
            // }
            // System.out.println(groupIds);
            // 4. 查询这些小组拥有的所有权限ID（去重）
            Set<String> userGroupPermIds = getPermIdsByGroupIds(groupId);
            System.out.println(groupId);
            // 5. 查询符合权限类型的所有权限（全量权限）
            List<SysPermissionPO> allPermissions = queryPermissionsByType(permTypeList);
            if (CollectionUtils.isEmpty(allPermissions)) {
                return Result.ok(new ArrayList<>());
            }

            // 6. 构建树形结构并标记权限拥有状态
            List<PermissionTreeDTO> permissionTree = buildPermissionTree(allPermissions, userGroupPermIds);

            return Result.ok(permissionTree);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("查询权限树形结构失败：" + e.getMessage());
        }
    }

    /**
     * 根据用户ID查询关联的所有小组ID
     */
    private List<String> getGroupIdsByUserId(String userId) {
        QueryWrapper<SysUserGroupPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                    .select("group_id");

        List<SysUserGroupPO> userGroupList = sysUserGroupMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(userGroupList)) {
            return new ArrayList<>();
        }

        return userGroupList.stream()
                .map(SysUserGroupPO::getGroupId)
                .filter(groupId -> StringUtils.hasText(groupId))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 根据小组ID列表查询所有关联的权限ID
     */
    private Set<String> getPermIdsByGroupIds(String groupId) { // 参数类型从List<String>改为String
        // 构建查询条件：用eq（精确匹配单个ID）替换原in（匹配列表）
        QueryWrapper<SysGroupPermissionPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("group_id", groupId) // 单个groupId精确查询
                    .select("perm_id"); // 只查权限ID，减少数据传输
                    // .isNull("deleted"); // 若有软删除字段，需加此条件（如deleted=0或deleted is null）

        // 执行查询（此时查询的是单个小组的权限关联记录）
        List<SysGroupPermissionPO> groupPermList = sysGroupPermissionMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(groupPermList)) {
            return new HashSet<>(); // 无权限时返回空集合，避免空指针
        }
System.out.println("groupId=" + groupId + "查询到的权限关联记录数：" + (groupPermList.size()));
        // 提取权限ID（去重、过滤空值，逻辑不变）
        return groupPermList.stream()
                .map(SysGroupPermissionPO::getPermId)
                .filter(permId -> StringUtils.hasText(permId)) // 过滤空的权限ID
                .distinct() // 去重（防止重复的小组-权限关联记录）
                .collect(Collectors.toSet());
    }

    /**
     * 根据权限类型查询权限列表
     */
    private List<SysPermissionPO> queryPermissionsByType(List<Integer> permTypeList) {
        QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
        // 按类型筛选（如无类型限制则查询所有）
        if (permTypeList != null && !permTypeList.isEmpty()) {
            queryWrapper.in("perm_type", permTypeList);
        }
        // 只查询启用状态的权限，排除"首页"
        queryWrapper.eq("status", 1)
                    .ne("title", "首页")
                    .orderByAsc("sort");
        
        return sysPermissionMapper.selectList(queryWrapper);
    }

    /**
     * 构建权限树形结构并标记是否拥有该权限
     */
    private List<PermissionTreeDTO> buildPermissionTree(List<SysPermissionPO> permissions, Set<String> userGroupPermIds) {
        // 步骤1：先筛选出“用户拥有的权限”（只保留permId在userGroupPermIds中的PO）
        List<SysPermissionPO> havePermissions = permissions.stream()
                .filter(po -> userGroupPermIds.contains(po.getId())) // 只保留拥有的权限
                .collect(Collectors.toList());
        System.out.println("实际拥有的权限PO数量：" + havePermissions.size()); // 日志：应与groupPermList数量一致（如groupId=8时为35）

        // 步骤2：转换为DTO并标记isHave（此时所有DTO的isHave都是true，因为已筛选）
        List<PermissionTreeDTO> haveDtoList = havePermissions.stream().map(po -> {
            PermissionTreeDTO dto = new PermissionTreeDTO();
            BeanUtils.copyProperties(po, dto);
            dto.setIsHave(true); // 已筛选，必然拥有
            dto.setChildren(new ArrayList<>());
            return dto;
        }).collect(Collectors.toList());

        // 步骤3：构建DTO的ID映射（便于快速查找父节点）
        Map<String, PermissionTreeDTO> dtoMap = haveDtoList.stream()
                .collect(Collectors.toMap(
                        PermissionTreeDTO::getId,
                        dto -> dto,
                        (existing, replacement) -> existing
                ));

        // 步骤4：构建树形结构（只处理拥有的权限，避免多余节点）
        List<PermissionTreeDTO> rootNodes = new ArrayList<>();
        for (PermissionTreeDTO currentDto : haveDtoList) {
            Integer parentId = currentDto.getParentId();
            String parentIdStr = parentId != null ? parentId.toString() : "0";

            // 根节点判断：父ID为0 或 父权限不在“拥有的权限”中（说明当前是根权限）
            if ("0".equals(parentIdStr) || !dtoMap.containsKey(parentIdStr)) {
                rootNodes.add(currentDto);
            } else {
                // 子节点：添加到父节点的children中（父节点必然在拥有的权限中）
                PermissionTreeDTO parentDto = dtoMap.get(parentIdStr);
                parentDto.getChildren().add(currentDto);
            }
        }

        // 步骤5：排序树形结构
        sortPermissionTree(rootNodes);
        System.out.println("最终返回的根权限数量：" + rootNodes.size()); // 日志：验证根节点数量是否合理

        return rootNodes;
    }

    /**
     * 递归排序树形结构的所有节点（按sort字段升序）
     */
    private void sortPermissionTree(List<PermissionTreeDTO> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        
        // 对当前层级节点按sort排序
        nodes.sort(Comparator.comparingInt(node -> {
            Integer sort = node.getSort();
            return sort != null ? sort : 0;
        }));
        
        // 递归排序子节点
        for (PermissionTreeDTO node : nodes) {
            sortPermissionTree(node.getChildren());
        }
    }


    @Override
    public Result<Boolean> updateGroupPermissions(String groupId, List<String> targetPermIds) {
        try {
            // 1. 校验参数
            if (!StringUtils.hasText(groupId)) {
                return Result.fail("小组ID不能为空");
            }
            if (CollectionUtils.isEmpty(targetPermIds)) {
                return Result.fail("权限ID列表不能为空");
            }

            // 2. 查询小组当前已有的权限
            QueryWrapper<SysGroupPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("group_id", groupId);
            List<SysGroupPermissionPO> existingPerms = sysGroupPermissionMapper.selectList(queryWrapper);

            // 3. 计算需要新增和删除的权限
            Set<String> existingPermIds = existingPerms.stream()
                    .map(SysGroupPermissionPO::getPermId)
                    .collect(Collectors.toSet());
            Set<String> targetPermIdSet = new HashSet<>(targetPermIds);

            // 4. 删除不需要的权限
            List<String> toDeletePermIds = existingPermIds.stream()
                    .filter(id -> !targetPermIdSet.contains(id))
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(toDeletePermIds)) {
                QueryWrapper<SysGroupPermissionPO> deleteWrapper = new QueryWrapper<>();
                deleteWrapper.eq("group_id", groupId)
                            .in("perm_id", toDeletePermIds);
                sysGroupPermissionMapper.delete(deleteWrapper);
            }

            // 5. 新增需要的权限
            List<String> toAddPermIds = targetPermIdSet.stream()
                    .filter(id -> !existingPermIds.contains(id))
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(toAddPermIds)) {
                List<SysGroupPermissionPO> newPermRelations = toAddPermIds.stream()
                        .map(permId -> {
                            SysGroupPermissionPO po = new SysGroupPermissionPO();
                            po.setGroupId(groupId);
                            po.setPermId(permId);
                            return po;
                        })
                        .collect(Collectors.toList());
                sysGroupPermissionMapper.insertBatch(newPermRelations); // 假设Mapper有批量插入方法
            }

            return Result.ok(Boolean.TRUE);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("更新小组权限失败：" + e.getMessage());
        }
    }
}
