/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.sysUserPermissionService.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysRoleMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.PermissionTreeDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysRolePO;
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
    
    @Autowired
    private SysGroupMapper sysGroupMapper;
    
    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public Result<List<PermissionDTO>> getUserPermissions(String userId) {
        // 1. 校验用户是否存在
        SysUserPO userPO = sysUserMapper.selectById(userId);
        if (userPO == null) {
            //用户不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00000001"));
        }

        // 2. 查用户关联的小组ID
        QueryWrapper<SysUserGroupPO> userGroupQuery = new QueryWrapper<>();
        userGroupQuery.eq("user_id", userId);
        List<SysUserGroupPO> userGroupList = sysUserGroupMapper.selectList(userGroupQuery);
        if (userGroupList.isEmpty()) {
            //该用户未关联任何小组
            return Result.fail(YamlMessageSource.getMessageStatic("00605001"));
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
                .filter(dto -> {
                    // 过滤掉hidden=1的权限（注意处理null的情况，避免空指针）
                    Integer hidden = dto.getHidden();
                    return hidden == null || hidden != 1;
                })
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

    /**
     * 单参数版：构建PermissionDTO树形结构（入参：PermissionDTO列表）
     * 完全保留你的原有逻辑，未做任何修改，调用处（getUserPermissions）正常使用
     */
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

    /**
     * 双参数版：构建PermissionTreeDTO树形结构（入参：SysPermissionPO列表+小组已拥有权限ID集合）
     * 仅修改此方法，不影响单参数版，调用处（getPermissionTreeByTypeAndUser）逻辑不变
     */
    private List<PermissionTreeDTO> buildPermissionTree(List<SysPermissionPO> permissions, Set<String> groupPermIds) {
        // 1. PO转DTO，初始化叶子节点isHave（父节点后续递归更新）
        List<PermissionTreeDTO> dtoList = permissions.stream().map(po -> {
            PermissionTreeDTO dto = new PermissionTreeDTO();
            BeanUtils.copyProperties(po, dto);
            dto.setIsHave(groupPermIds.contains(po.getId())); // 叶子节点初始值
            dto.setChildren(new ArrayList<>());
            return dto;
        }).collect(Collectors.toList());

        // 2. 构建DTO ID映射，快速找父节点
        Map<String, PermissionTreeDTO> dtoMap = dtoList.stream()
                .collect(Collectors.toMap(
                        PermissionTreeDTO::getId,
                        dto -> dto,
                        (existing, replacement) -> existing
                ));

        // 3. 组装父子关系（原有逻辑不变）
        List<PermissionTreeDTO> rootNodes = new ArrayList<>();
        for (PermissionTreeDTO currentDto : dtoList) {
            Integer parentId = currentDto.getParentId();
            String parentIdStr = parentId != null ? parentId.toString() : "0";
            if ("0".equals(parentIdStr) || !dtoMap.containsKey(parentIdStr)) {
                rootNodes.add(currentDto);
            } else {
                PermissionTreeDTO parentDto = dtoMap.get(parentIdStr);
                parentDto.getChildren().add(currentDto);
            }
        }

        // 4. 新增：递归计算父节点isHave状态
        for (PermissionTreeDTO root : rootNodes) {
            calculateParentIsHave(root);
        }

        // 5. 原有排序逻辑不变
        sortPermissionTree(rootNodes);
        return rootNodes;
    }

    /**
     * 递归辅助方法：仅用于双参数版buildPermissionTree，计算父节点的isHave状态
     * 规则：父节点isHave = 所有子节点isHave均为true；叶子节点isHave = 小组是否直接拥有
     */
    private boolean calculateParentIsHave(PermissionTreeDTO node) {
        // 叶子节点（无children）：直接返回初始isHave值（小组是否直接拥有该权限）
        if (CollectionUtils.isEmpty(node.getChildren())) {
            return node.getIsHave();
        }

        // 非叶子节点：遍历所有子节点，只要有一个子节点isHave为false，父节点则为false
        boolean allChildrenHave = true;
        for (PermissionTreeDTO child : node.getChildren()) {
            if (!calculateParentIsHave(child)) {
                allChildrenHave = false;
            }
        }

        // 更新当前父节点的isHave状态
        node.setIsHave(allChildrenHave);
        return allChildrenHave;
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
            //用户不存在
            return Result.fail(YamlMessageSource.getMessageStatic("00000001"));
        }

        QueryWrapper<SysUserGroupPO> userGroupQuery = new QueryWrapper<>();
        userGroupQuery.eq("user_id", userId);
        List<SysUserGroupPO> userGroupList = sysUserGroupMapper.selectList(userGroupQuery);
        if (userGroupList.isEmpty()) {
            //该用户未关联任何小组
            return Result.fail(YamlMessageSource.getMessageStatic("00605001"));
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
     * 核心方法：按小组→角色→权限逻辑查询树形权限结构，标记小组是否拥有该权限
     */
    @Override
    public Result<List<PermissionTreeDTO>> getPermissionTreeByTypeAndUser(List<Integer> permTypeList, String groupId) {
        try {
            // 1. 校验小组ID有效性
            if (!StringUtils.hasText(groupId)) {
                //小组ID不能为空
                return Result.fail(YamlMessageSource.getMessageStatic("00500001"));
            }
            
            // 2. 查询小组信息，获取关联的角色ID
            SysGroupPO groupPO = sysGroupMapper.selectById(groupId);
            System.out.println("groupPO:"+groupPO);
            if (groupPO == null) {
                //"小组不存在（groupId：" + groupId + "）"
                return Result.fail(YamlMessageSource.getMessageStatic("00605002",groupId));
            }
            
            String roleId = groupPO.getRoleId();
            System.out.println("roleId:"+roleId);
            if (!StringUtils.hasText(roleId)) {
                //"小组未关联任何角色（groupId：" + groupId + "）"
                return Result.fail(YamlMessageSource.getMessageStatic("00605003",groupId));
            }
            
            // 3. 查询角色信息，获取该角色的所有权限ID（总权限）
            SysRolePO rolePO = sysRoleMapper.selectById(roleId);
            System.out.println("rolePO:"+rolePO);
            if (rolePO == null) {
                //"角色不存在（roleId：" + roleId + "）"
                return Result.fail(YamlMessageSource.getMessageStatic("00605004",roleId));
            }
            
            String permIdsStr = rolePO.getPermIds();
            System.out.println("permIdsStr:"+permIdsStr);
            if (!StringUtils.hasText(permIdsStr)) {
                return Result.ok(new ArrayList<>());
            }
            
            // 解析角色的权限ID列表
            List<String> rolePermIds = Arrays.stream(permIdsStr.split(","))
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
            
            if (CollectionUtils.isEmpty(rolePermIds)) {
                return Result.ok(new ArrayList<>());
            }
            
            // 4. 查询该小组已拥有的权限ID
            Set<String> groupPermIds = getPermIdsByGroupId(groupId);
            
            // 5. 查询角色拥有的所有权限详情（按权限类型筛选）
            List<SysPermissionPO> rolePermissions = queryPermissionsByTypeAndIds(permTypeList, rolePermIds);
            if (CollectionUtils.isEmpty(rolePermissions)) {
                return Result.ok(new ArrayList<>());
            }

            // 6. 构建树形结构并标记权限拥有状态
            List<PermissionTreeDTO> permissionTree = buildPermissionTree(rolePermissions, groupPermIds);

            return Result.ok(permissionTree);

        } catch (Exception e) {
            e.printStackTrace();
            //"查询权限树形结构失败：" + e.getMessage()
            return Result.fail(YamlMessageSource.getMessageStatic("00605005",e.getMessage()));
        }
    }

    /**
     * 根据小组ID查询关联的权限ID
     */
    private Set<String> getPermIdsByGroupId(String groupId) {
        QueryWrapper<SysGroupPermissionPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("group_id", groupId)
                    .select("perm_id");

        List<SysGroupPermissionPO> groupPermList = sysGroupPermissionMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(groupPermList)) {
            return new HashSet<>();
        }
        
        return groupPermList.stream()
                .map(SysGroupPermissionPO::getPermId)
                .filter(permId -> StringUtils.hasText(permId))
                .distinct()
                .collect(Collectors.toSet());
    }

    /**
     * 根据权限类型和权限ID列表查询权限详情
     */
    private List<SysPermissionPO> queryPermissionsByTypeAndIds(List<Integer> permTypeList, List<String> permIds) {
        QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", permIds)
                    .eq("status", 1)
                    .ne("title", "首页")
                    .orderByAsc("sort");
        
        // 按类型筛选（如无类型限制则查询所有）
        if (permTypeList != null && !permTypeList.isEmpty()) {
            queryWrapper.in("perm_type", permTypeList);
        }
        
        return sysPermissionMapper.selectList(queryWrapper);
    }

    // /**
    //  * 构建权限树形结构并标记小组是否拥有该权限
    //  */
    // private List<PermissionTreeDTO> buildPermissionTree(List<SysPermissionPO> permissions, Set<String> groupPermIds) {
    //     // 转换为DTO并标记isHave（根据小组是否拥有该权限）
    //     List<PermissionTreeDTO> dtoList = permissions.stream().map(po -> {
    //         PermissionTreeDTO dto = new PermissionTreeDTO();
    //         BeanUtils.copyProperties(po, dto);
    //         // 核心逻辑：判断小组是否拥有该权限
    //         dto.setIsHave(groupPermIds.contains(po.getId()));
    //         dto.setChildren(new ArrayList<>());
    //         return dto;
    //     }).collect(Collectors.toList());

    //     // 构建DTO的ID映射（便于快速查找父节点）
    //     Map<String, PermissionTreeDTO> dtoMap = dtoList.stream()
    //             .collect(Collectors.toMap(
    //                     PermissionTreeDTO::getId,
    //                     dto -> dto,
    //                     (existing, replacement) -> existing
    //             ));

    //     // 构建树形结构
    //     List<PermissionTreeDTO> rootNodes = new ArrayList<>();
    //     for (PermissionTreeDTO currentDto : dtoList) {
    //         Integer parentId = currentDto.getParentId();
    //         String parentIdStr = parentId != null ? parentId.toString() : "0";

    //         // 根节点判断：父ID为0 或 父权限不在当前权限列表中
    //         if ("0".equals(parentIdStr) || !dtoMap.containsKey(parentIdStr)) {
    //             rootNodes.add(currentDto);
    //         } else {
    //             // 子节点：添加到父节点的children中
    //             PermissionTreeDTO parentDto = dtoMap.get(parentIdStr);
    //             parentDto.getChildren().add(currentDto);
    //         }
    //     }

    //     // 排序树形结构
    //     sortPermissionTree(rootNodes);
        
    //     return rootNodes;
    // }

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
            // 1. 校验参数（原有逻辑不变）
            if (!StringUtils.hasText(groupId)) {
                //小组ID不能为空
                return Result.fail(YamlMessageSource.getMessageStatic("00500001"));
            }
            if (CollectionUtils.isEmpty(targetPermIds)) {
                //权限ID列表不能为空
                return Result.fail(YamlMessageSource.getMessageStatic("00605006"));
            }

            // 关键修复：将不可修改的列表转换为可修改的ArrayList
            targetPermIds = new ArrayList<>(targetPermIds);

            // ===================== 新增逻辑：自动关联一级菜单的首页子权限 =====================
            // 1.1 筛选目标权限中的一级菜单ID
            List<String> firstLevelPermIds = filterFirstLevelPermIds(targetPermIds);
            // 1.2 查询一级菜单下的首页子权限ID
            List<String> homePermIds = getHomePermIdsByFirstLevelPerms(firstLevelPermIds);
            // 1.3 将首页子权限ID添加到目标权限列表（去重，避免重复）
            if (!CollectionUtils.isEmpty(homePermIds)) {
                targetPermIds.addAll(homePermIds);
                // 去重：防止同一首页权限被多次添加
                targetPermIds = targetPermIds.stream().distinct().collect(Collectors.toList());
            }
            // ==========================================================================

            // 后续原有逻辑不变...
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
                sysGroupPermissionMapper.insertBatch(newPermRelations);
            }

            return Result.ok(Boolean.TRUE);

        } catch (Exception e) {
            e.printStackTrace();
            //"更新小组权限失败：" + e.getMessage()
            return Result.fail(YamlMessageSource.getMessageStatic("00605007",e.getMessage()));
        }
    }

    /**
     * 新增方法：查询一级菜单（parent_id=0）下 title=首页 的子权限ID
     * @param firstLevelPermIds 一级菜单权限ID列表
     * @return 首页子权限ID列表（无结果则返回空列表）
     */
    private List<String> getHomePermIdsByFirstLevelPerms(List<String> firstLevelPermIds) {
        if (CollectionUtils.isEmpty(firstLevelPermIds)) {
            return new ArrayList<>();
        }
        // 查询一级菜单下 title=首页 的子权限
        QueryWrapper<SysPermissionPO> homePermQuery = new QueryWrapper<>();
        homePermQuery.in("parent_id", firstLevelPermIds) // 父ID为一级菜单ID
                    .eq("title", "首页") // 子权限标题为"首页"
                    .select("id"); // 仅查询权限ID，提升效率
        List<SysPermissionPO> homePerms = sysPermissionMapper.selectList(homePermQuery);
        // 提取首页子权限ID并返回
        return homePerms.stream()
                .map(SysPermissionPO::getId)
                .collect(Collectors.toList());
    }

    /**
     * 新增方法：筛选目标权限中的一级菜单（parent_id=0）ID
     * @param targetPermIds 目标权限ID列表
     * @return 一级菜单权限ID列表（无结果则返回空列表）
     */
    private List<String> filterFirstLevelPermIds(List<String> targetPermIds) {
        if (CollectionUtils.isEmpty(targetPermIds)) {
            return new ArrayList<>();
        }
        // 筛选目标权限中的一级菜单（parent_id=0）
        QueryWrapper<SysPermissionPO> firstLevelQuery = new QueryWrapper<>();
        firstLevelQuery.in("id", targetPermIds)
                    .eq("parent_id", 0) // 一级菜单标识
                    .select("id"); // 仅查询权限ID
        List<SysPermissionPO> firstLevelPerms = sysPermissionMapper.selectList(firstLevelQuery);
        // 提取一级菜单ID并返回
        return firstLevelPerms.stream()
                .map(SysPermissionPO::getId)
                .collect(Collectors.toList());
    }
}
