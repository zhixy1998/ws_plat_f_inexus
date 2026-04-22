package com.persee.workstation.uv_inexus.service.sysUserPermissionService.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupQuickConfigMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysQuickConfigMapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigGrantPermDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigSaveDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigTreeDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupQuickConfigPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysQuickConfigPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysQuickConfigService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SysQuickConfigServiceImpl extends ServiceImpl<SysQuickConfigMapper, SysQuickConfigPO> implements SysQuickConfigService {

    @Autowired
    private SysGroupQuickConfigMapper sysGroupQuickConfigMapper;

    @Autowired
    private SysGroupPermissionMapper sysGroupPermissionMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    // ====================== 分页/新增/修改/删除 完全保持你原来逻辑不动 ======================
    @Override
    public Result<PageView<SysQuickConfigPO>> getQuickConfigPage(Page<SysQuickConfigPO> page, QuickConfigQueryDTO queryDTO) {
        LambdaQueryWrapper<SysQuickConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getName() != null && !queryDTO.getName().isEmpty()) {
            queryWrapper.like(SysQuickConfigPO::getName, queryDTO.getName());
        }
        if (queryDTO.getPermIds() != null && !queryDTO.getPermIds().isEmpty()) {
            queryWrapper.like(SysQuickConfigPO::getPermIds, queryDTO.getPermIds());
        }
        if (queryDTO.getParentId() != null) {
            queryWrapper.eq(SysQuickConfigPO::getParentId, queryDTO.getParentId());
        }
        IPage<SysQuickConfigPO> iPage = baseMapper.selectPage(page, queryWrapper);
        return Result.ok(new PageView<>(iPage));
    }

    @Override
    public Result<List<QuickConfigTreeDTO>> getQuickConfigList(QuickConfigQueryDTO queryDTO) {
        QueryWrapper<SysQuickConfigPO> queryWrapper = new QueryWrapper<>();
        
        // 构建查询条件
        if (queryDTO != null) {
            if (queryDTO.getName() != null && !queryDTO.getName().isEmpty()) {
                queryWrapper.like("name", queryDTO.getName());
            }
            if (queryDTO.getPermIds() != null && !queryDTO.getPermIds().isEmpty()) {
                queryWrapper.like("perm_ids", queryDTO.getPermIds());
            }
            if (queryDTO.getParentId() != null) {
                queryWrapper.eq("parent_id", queryDTO.getParentId());
            }
            if (queryDTO.getQuickConfigType() != null) {
                queryWrapper.eq("quick_config_type", queryDTO.getQuickConfigType());
            }
        }
        
        // 查询所有快捷配置
        List<SysQuickConfigPO> configs = baseMapper.selectList(queryWrapper);
        List<QuickConfigTreeDTO> tree = buildQuickConfigTree(configs);
        return Result.ok(tree);
    }

    /**
     * 构建快捷配置树形结构
     */
    private List<QuickConfigTreeDTO> buildQuickConfigTree(List<SysQuickConfigPO> configs) {
        List<QuickConfigTreeDTO> dtoList = configs.stream().map(po -> {
            QuickConfigTreeDTO dto = new QuickConfigTreeDTO();
            BeanUtils.copyProperties(po, dto);
            dto.setChildren(new ArrayList<>());
            return dto;
        }).collect(Collectors.toList());
        
        // 构建树形结构
        List<QuickConfigTreeDTO> rootList = new ArrayList<>();
        for (QuickConfigTreeDTO dto : dtoList) {
            Long parentId = dto.getParentId();
            if (parentId == null || parentId == 0) {
                rootList.add(dto);
            } else {
                for (QuickConfigTreeDTO parent : dtoList) {
                    if (parentId.equals(Long.parseLong(parent.getId()))) {
                        parent.getChildren().add(dto);
                        break;
                    }
                }
            }
        }
        
        return rootList;
    }

    @Override
    @Transactional
    public Result<String> addQuickConfig(QuickConfigSaveDTO saveDTO) {
        if (saveDTO.getParentId() != 0) {
            SysQuickConfigPO parent = baseMapper.selectById(saveDTO.getParentId());
            if (parent == null) return Result.fail("父级配置不存在");
        }
        SysQuickConfigPO po = new SysQuickConfigPO();
        BeanUtils.copyProperties(saveDTO, po);
        baseMapper.insert(po);
        return Result.ok("新增成功");
    }

    @Override
    @Transactional
    public Result<String> updateQuickConfig(QuickConfigSaveDTO saveDTO) {
        SysQuickConfigPO po = baseMapper.selectById(saveDTO.getId());
        if (po == null) return Result.fail("配置不存在");
        if (saveDTO.getParentId() != 0) {
            SysQuickConfigPO parent = baseMapper.selectById(saveDTO.getParentId());
            if (parent == null) return Result.fail("父级配置不存在");
        }
        BeanUtils.copyProperties(saveDTO, po);
        baseMapper.updateById(po);
        return Result.ok("修改成功");
    }

    @Override
    @Transactional
    public Result<String> deleteById(Long id) {
        SysQuickConfigPO po = baseMapper.selectById(id);
        if (po == null) return Result.fail("配置不存在");
        long count = baseMapper.selectCount(new LambdaQueryWrapper<SysQuickConfigPO>().eq(SysQuickConfigPO::getParentId, id));
        if (count > 0) return Result.fail("存在子级，无法删除");
        baseMapper.deleteById(id);
        return Result.ok("删除成功");
    }

    @Override
    @Transactional
    public Result<String> deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Result.fail("请选择数据");
        for (Long id : ids) {
            long count = baseMapper.selectCount(new LambdaQueryWrapper<SysQuickConfigPO>().eq(SysQuickConfigPO::getParentId, id));
            if (count > 0) return Result.fail("ID:" + id + " 存在子级");
        }
        baseMapper.deleteBatchIds(ids);
        return Result.ok("批量删除成功");
    }

    @Override
    public Result<SysQuickConfigPO> getQuickConfigById(Long id) {
        SysQuickConfigPO po = baseMapper.selectById(id);
        return po == null ? Result.fail("不存在") : Result.ok(po);
    }

    // ====================== 树形回显（修复：读取当前组自己的 permIdsHave） ======================
    @Override
    public Result<List<Map<String, Object>>> getQuickConfigTree(String groupId, Integer quickConfigType) {
        try {
            final Set<String> groupOwnPermIds = new HashSet<>();
            if (StringUtils.hasText(groupId)) {
                SysGroupQuickConfigPO groupConfig = sysGroupQuickConfigMapper.selectOne(
                        new QueryWrapper<SysGroupQuickConfigPO>()
                                .eq("group_id", groupId)
                                .eq("deleted", 0)
                );
                if (groupConfig != null && StringUtils.hasText(groupConfig.getPermIdsHave())) {
                    groupOwnPermIds.addAll(parsePermIdsToSet(groupConfig.getPermIdsHave()));
                }
            }

            List<SysQuickConfigPO> allConfig = baseMapper.selectList(
                    new QueryWrapper<SysQuickConfigPO>().lambda()
                            .eq(quickConfigType != null, SysQuickConfigPO::getQuickConfigType, quickConfigType)
            );

            // 【修复】全部使用 new ArrayList<>()，确保可 add
            List<Map<String, Object>> configMapList = new ArrayList<>();
            for (SysQuickConfigPO po : allConfig) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", po.getId());
                map.put("name", po.getName());
                map.put("permIds", po.getPermIds());
                map.put("parentId", po.getParentId());
                map.put("quickConfigType", po.getQuickConfigType());

                Set<String> configPermSet = parsePermIdsToSet(po.getPermIds());
                Set<String> currentConfigOwn = new HashSet<>();
                for (String pid : groupOwnPermIds) {
                    if (configPermSet.contains(pid)) {
                        currentConfigOwn.add(pid);
                    }
                }

                // 【关键修复】children 必须是 new ArrayList，否则不能 add 子节点
                List<Map<String, Object>> children = getPermissionListByPermIds(po.getPermIds(), currentConfigOwn);
                List<Map<String, Object>> mutableChildren = new ArrayList<>(children);
                map.put("children", mutableChildren);

                configMapList.add(map);
            }

            // 构建树形（正常拼接父子）
            List<Map<String, Object>> root = new ArrayList<>();
            for (Map<String, Object> m : configMapList) {
                Long pid = (Long) m.get("parentId");
                if (pid == null || pid == 0) {
                    root.add(m);
                } else {
                    for (Map<String, Object> p : configMapList) {
                        String pIdStr = (String) p.get("id");
                        if (pIdStr == null) continue;

                        long parentIdOfNode;
                        try {
                            parentIdOfNode = Long.parseLong(pIdStr);
                        } catch (Exception e) {
                            continue;
                        }

                        if (pid == parentIdOfNode) {
                            List<Map<String, Object>> parentChildren = (List<Map<String, Object>>) p.get("children");
                            parentChildren.add(m); // 现在不会报错了
                            break;
                        }
                    }
                }
            }

            return Result.ok(root);
        } catch (Exception e) {
            log.error("树形查询失败", e);
            return Result.fail("查询失败");
        }
    }

    /**
     * 查询当前权限【自己 + 所有上级父菜单】，直到 parent_id = 0
     * 完全匹配你的实体类：id=String, parentId=Integer
     */
    private Set<String> findAllParentIds(String permIdStr) {
        Set<String> allIds = new LinkedHashSet<>();
        allIds.add(permIdStr);

        try {
            String currentId = permIdStr;
            while (true) {
                SysPermissionPO po = sysPermissionMapper.selectById(currentId);
                if (po == null) break;

                Integer parentId = po.getParentId();
                if (parentId == null || parentId == 0) break;

                String parentIdStr = String.valueOf(parentId);
                if (allIds.contains(parentIdStr)) break;

                allIds.add(parentIdStr);
                currentId = parentIdStr;
            }
        } catch (Exception e) {
            log.error("查询父权限失败", e);
        }
        return allIds;
    }

    // ====================== 核心授权（已修复：写入 perm_ids_have 到组关联表） ======================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> grantQuickConfigToGroup(QuickConfigGrantPermDTO grantDTO) {
        try {
            String groupId = grantDTO.getGroupId();
            if (!StringUtils.hasText(groupId)) return Result.fail("小组ID不能为空");

            Map<String, List<Long>> permMap = grantDTO.getQuickConfigIds();
            if (permMap == null) permMap = new HashMap<>();

            // 1. 查询当前组已有绑定
            SysGroupQuickConfigPO exist = sysGroupQuickConfigMapper.selectOne(
                    new QueryWrapper<SysGroupQuickConfigPO>()
                            .eq("group_id", groupId)
                            .eq("deleted", 0)
            );

            // 2. 空值清空
            if (permMap.isEmpty()) {
                if (exist != null) {
                    exist.setQuickConfigIds("");
                    exist.setPermIdsHave("");
                    sysGroupQuickConfigMapper.updateById(exist);
                }
                return Result.ok(true);
            }

            // 3. 收集本次选择的所有权限 + 自动带上所有父级菜单
            Set<String> allSelected = new HashSet<>();
            Set<String> configIds = permMap.keySet();
            for (String cid : configIds) {
                List<Long> pids = permMap.get(cid);
                if (pids != null) {
                    for (Long pid : pids) {
                        String permIdStr = String.valueOf(pid);
                        Set<String> idsWithParent = findAllParentIds(permIdStr);
                        allSelected.addAll(idsWithParent);
                    }
                }
            }

            // 4. 拼接要保存的 ownPermIds
            String ownPermIdsStr = allSelected.stream()
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            // 5. 拼接快捷配置ID
            String quickConfigIdsStr = String.join(",", configIds);

            // 6. 保存或更新
            if (exist != null) {
                exist.setQuickConfigIds(quickConfigIdsStr);
                exist.setPermIdsHave(ownPermIdsStr);
                sysGroupQuickConfigMapper.updateById(exist);
            } else {
                SysGroupQuickConfigPO newPO = new SysGroupQuickConfigPO();
                newPO.setGroupId(groupId);
                newPO.setQuickConfigIds(quickConfigIdsStr);
                newPO.setPermIdsHave(ownPermIdsStr);
                sysGroupQuickConfigMapper.insert(newPO);
            }

            // 7. 同步更新组权限表（你原有逻辑完全保留）
            syncGroupPermission(groupId, allSelected);

            return Result.ok(true);
        } catch (Exception e) {
            log.error("授权失败", e);
            return Result.fail("授权失败：" + e.getMessage());
        }
    }

    // ====================== 同步组权限（保留你原来逻辑，抽成方法） ======================
    private void syncGroupPermission(String groupId, Set<String> selectedIds) {
        // 1. 删除该组原有权限
        sysGroupPermissionMapper.delete(
                new LambdaQueryWrapper<SysGroupPermissionPO>()
                        .eq(SysGroupPermissionPO::getGroupId, groupId)
        );

        // 2. 批量插入（包含子权限 + 所有父级）
        if (selectedIds.isEmpty()) return;
        List<SysGroupPermissionPO> list = new ArrayList<>();
        for (String pid : selectedIds) {
            SysGroupPermissionPO po = new SysGroupPermissionPO();
            po.setGroupId(groupId);
            po.setPermId(pid);
            list.add(po);
        }
        sysGroupPermissionMapper.insertBatch(list);
    }

    // ====================== 工具方法 ======================
    private Set<String> parsePermIdsToSet(String str) {
        Set<String> set = new HashSet<>();
        if (!StringUtils.hasText(str)) return set;
        String[] arr = str.split(",");
        for (String s : arr) {
            if (StringUtils.hasText(s)) set.add(s.trim());
        }
        return set;
    }

    private List<Map<String, Object>> getPermissionListByPermIds(String permIds, Set<String> ownSet) {
        if (!StringUtils.hasText(permIds)) return Collections.emptyList();
        List<String> idList = Arrays.stream(permIds.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());

        List<SysPermissionPO> list = sysPermissionMapper.selectList(
                new QueryWrapper<SysPermissionPO>().lambda()
                        .in(SysPermissionPO::getId, idList)
                        .eq(SysPermissionPO::getStatus, 1)
        );

        return list.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getTitle());
            m.put("type", "permission");
            m.put("isHave", ownSet.contains(p.getId()));
            return m;
        }).collect(Collectors.toList());
    }
}