/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.sysUserPermissionService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysGroupQuickConfigMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysPermissionMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysQuickConfigMapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigGrantPermDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigSaveDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigTreeDTO;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupQuickConfigPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysPermissionPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysQuickConfigPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysQuickConfigService;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.UserPermissionService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 快捷配置Service实现类
 */
@Service
public class SysQuickConfigServiceImpl extends ServiceImpl<SysQuickConfigMapper, SysQuickConfigPO> implements SysQuickConfigService {
    @Autowired
    private SysGroupQuickConfigMapper sysGroupQuickConfigMapper;
    @Autowired
    private SysGroupMapper sysGroupMapper;

    @Autowired
    private YamlMessageSource yamlMessageSource;

    @Autowired
    private UserPermissionService userPermissionService;

    @Override
    public Result<PageView<SysQuickConfigPO>> getQuickConfigPage(Page<SysQuickConfigPO> page, QuickConfigQueryDTO queryDTO) {
        LambdaQueryWrapper<SysQuickConfigPO> queryWrapper = new LambdaQueryWrapper<>();

        // 条件查询：配置名称模糊匹配
        if (queryDTO.getName() != null && !queryDTO.getName().isEmpty()) {
            queryWrapper.like(SysQuickConfigPO::getName, queryDTO.getName());
        }

        if (queryDTO.getPermIds() != null && !queryDTO.getPermIds().isEmpty()) {
            queryWrapper.like(SysQuickConfigPO::getPermIds, queryDTO.getPermIds());
        }
        // 条件查询：父级ID精确匹配
        if (queryDTO.getParentId() != null) {
            queryWrapper.eq(SysQuickConfigPO::getParentId, queryDTO.getParentId());
        }
        // 按创建时间降序排序
//        queryWrapper.orderByDesc(SysQuickConfigPO::getCreateTime);

        IPage<SysQuickConfigPO> iPage = baseMapper.selectPage(page, queryWrapper);
        PageView<SysQuickConfigPO> pageView = new PageView<>(iPage);
        return Result.ok(pageView);
    }

    @Override
    @Transactional
    public Result<String> addQuickConfig(QuickConfigSaveDTO saveDTO) {
        // 1. 校验父级配置是否存在（parentId≠0时）
        if (saveDTO.getParentId() != 0) {
            SysQuickConfigPO parentConfig = baseMapper.selectById(saveDTO.getParentId());
            if (parentConfig == null) {
                return Result.fail("父级快捷配置不存在，请检查父级ID");
            }
        }

        // 2. 封装配置信息并保存
        SysQuickConfigPO configPO = new SysQuickConfigPO();
        BeanUtils.copyProperties(saveDTO, configPO);
        baseMapper.insert(configPO);

        return Result.ok("快捷配置新增成功");
    }

    @Override
    @Transactional
    public Result<String> updateQuickConfig(QuickConfigSaveDTO saveDTO) {
        // 1. 校验配置是否存在
        SysQuickConfigPO configPO = baseMapper.selectById(saveDTO.getId());
        if (configPO == null) {
            return Result.fail("快捷配置不存在，修改失败");
        }

        // 2. 校验父级配置是否存在（parentId≠0时）
        if (saveDTO.getParentId() != 0) {
            SysQuickConfigPO parentConfig = baseMapper.selectById(saveDTO.getParentId());
            if (parentConfig == null) {
                return Result.fail("父级快捷配置不存在，请检查父级ID");
            }
        }

        // 3. 更新配置信息
        BeanUtils.copyProperties(saveDTO, configPO);
        baseMapper.updateById(configPO);

        return Result.ok("快捷配置修改成功");
    }

    @Override
    @Transactional
    public Result<String> deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择需要删除的快捷配置");
        }

        // 校验是否有子级配置（有子级则不允许删除）
        for (Long id : ids) {
            LambdaQueryWrapper<SysQuickConfigPO> childQuery = new LambdaQueryWrapper<>();
            childQuery.eq(SysQuickConfigPO::getParentId, id);
            long childCount = baseMapper.selectCount(childQuery);
            if (childCount > 0) {
                return Result.fail("配置ID：" + id + " 存在子级配置，无法删除");
            }
        }

        // 批量删除配置
        baseMapper.deleteBatchIds(ids);
        return Result.ok("快捷配置批量删除成功");
    }

    @Override
    public Result<SysQuickConfigPO> getQuickConfigById(Long id) {
        SysQuickConfigPO configPO = baseMapper.selectById(id);
        if (configPO == null) {
            return Result.fail("快捷配置不存在");
        }
        return Result.ok(configPO);
    }

    @Override
    @Transactional
    public Result<String> deleteById(Long id) {
        // 1. 校验配置是否存在
        SysQuickConfigPO configPO = baseMapper.selectById(id);
        if (configPO == null) {
            return Result.fail("快捷配置不存在，删除失败");
        }

        // 2. 校验是否有子级配置
        LambdaQueryWrapper<SysQuickConfigPO> childQuery = new LambdaQueryWrapper<>();
        childQuery.eq(SysQuickConfigPO::getParentId, id);
        long childCount = baseMapper.selectCount(childQuery);
        if (childCount > 0) {
            return Result.fail("该配置存在子级配置，无法删除");
        }

        // 3. 删除配置
        baseMapper.deleteById(id);
        return Result.ok("快捷配置删除成功");
    }

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Override
    public Result<List<Map<String, Object>>> getQuickConfigTree(String groupId) {
        try {
            // ========== 步骤1：获取该小组绑定的快捷配置ID集合 ==========
            Set<String> haveConfigIds = new HashSet<>();
            if (StringUtils.hasText(groupId)) {
                QueryWrapper<SysGroupQuickConfigPO> queryWrapper = new QueryWrapper<>();
                queryWrapper.select("quick_config_ids")
                        .eq("group_id", groupId);
                SysGroupQuickConfigPO groupConfig = sysGroupQuickConfigMapper.selectOne(queryWrapper);

                if (groupConfig != null && StringUtils.hasText(groupConfig.getQuickConfigIds())) {
                    String[] ids = groupConfig.getQuickConfigIds().split(",");
                    for (String id : ids) {
                        if (StringUtils.hasText(id)) {
                            haveConfigIds.add(id.trim());
                        }
                    }
                }
            }

            // ========== 步骤2：查询所有快捷配置，构建基础Map列表（补充perm_ids_have解析） ==========
            List<SysQuickConfigPO> allConfig = baseMapper.selectList(null);
            List<Map<String, Object>> allConfigMap = allConfig.stream().map(po -> {
                Map<String, Object> configMap = new HashMap<>();
                // 映射原有所有字段
                configMap.put("id", po.getId());
                configMap.put("createBy", po.getCreateBy() == null ? "" : po.getCreateBy());
                configMap.put("updateBy", po.getUpdateBy() == null ? "" : po.getUpdateBy());
                configMap.put("name", po.getName());
                configMap.put("permIds", po.getPermIds());
                configMap.put("permIdsHave", po.getPermIdsHave()); // 保留perm_ids_have字段
                configMap.put("parentId", po.getParentId());
                configMap.put("quickConfigType", po.getQuickConfigType());

                // 1：添加是否绑定标识
//                if (haveConfigIds.contains(po.getId())) {
//                    configMap.put("isHave", true);
//                }

                // 2：解析perm_ids_have为Set集合（用于权限节点的isHave判断）
                Set<String> permIdsHaveSet = parsePermIdsToSet(po.getPermIdsHave());

                // 3：解析permIds，查询权限列表并放入children（传入permIdsHaveSet）
                List<Map<String, Object>> children = new ArrayList<>();
                if (StringUtils.hasText(po.getPermIds())) {
                    List<Map<String, Object>> permList = getPermissionListByPermIds(po.getPermIds(), permIdsHaveSet);
                    children.addAll(permList); // 权限列表放入children
                }
                configMap.put("children", children); // 初始化children

                return configMap;
            }).collect(Collectors.toList());

            // ========== 步骤3：构建树形结构（原有快捷配置子节点逻辑） ==========
            List<Map<String, Object>> rootList = new ArrayList<>();
            for (Map<String, Object> dtoMap : allConfigMap) {
                Long parentId = (Long) dtoMap.get("parentId");
                if (parentId == 0) {
                    // 一级节点：加入根列表
                    rootList.add(dtoMap);
                } else {
                    // 二级快捷配置节点：找到父节点并添加到children（追加到权限列表之后）
                    for (Map<String, Object> parentMap : allConfigMap) {
                        String parentMapId = (String) parentMap.get("id");
                        if (parentId.equals(Long.parseLong(parentMapId))) {
                            List<Map<String, Object>> parentChildren = (List<Map<String, Object>>) parentMap.get("children");
                            parentChildren.add(dtoMap); // 快捷配置子节点追加到children
                            break;
                        }
                    }
                }
            }

            return Result.ok(rootList);
        } catch (Exception e) {
            log.error("查询快捷配置树形结构失败", e);
            return Result.fail("查询快捷配置树形结构失败：" + e.getMessage());
        }
    }

    /**
     * 工具方法：解析permIds字符串（如"1,2,3"）为Set集合，方便快速判断
     * @param permIdsStr 逗号分隔的ID字符串
     * @return 去重后的ID集合（空字符串/空值返回空集合）
     */
    private Set<String> parsePermIdsToSet(String permIdsStr) {
        Set<String> permIdSet = new HashSet<>();
        if (!StringUtils.hasText(permIdsStr)) {
            return permIdSet;
        }
        // 拆分、去重、过滤空值
        String[] ids = permIdsStr.split(",");
        for (String id : ids) {
            if (StringUtils.hasText(id)) {
                permIdSet.add(id.trim());
            }
        }
        return permIdSet;
    }

    /**
     * 工具方法：根据permIds字符串查询对应的权限列表，并添加isHave字段（基于permIdsHaveSet判断）
     * @param permIds 逗号分隔的权限ID字符串（如"118,119,120"）
     * @param permIdsHaveSet 当前快捷配置节点的perm_ids_have解析后的Set集合
     * @return 权限列表（每个元素含id、title、type、isHave）
     */
    private List<Map<String, Object>> getPermissionListByPermIds(String permIds, Set<String> permIdsHaveSet) {
        if (!StringUtils.hasText(permIds)) {
            return Collections.emptyList();
        }

        // 拆分并过滤无效ID
        List<String> permIdList = Arrays.stream(permIds.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());

        if (permIdList.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 查询权限表中对应ID的记录（仅查id和title）
            QueryWrapper<SysPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("id", "title")
                    .in("id", permIdList)
                    .eq("status", 1); // 仅查询启用状态的权限

            List<SysPermissionPO> permissionPOList = sysPermissionMapper.selectList(queryWrapper);

            // 转换为Map，添加type和isHave字段
            return permissionPOList.stream().map(perm -> {
                Map<String, Object> permMap = new HashMap<>(4); // 扩容为4，容纳新增的isHave字段
                permMap.put("id", perm.getId());
                permMap.put("name", perm.getTitle() == null ? "" : perm.getTitle());
                permMap.put("type", "permission"); // 权限节点标识
                // 核心：判断当前权限ID是否在permIdsHaveSet中，设置isHave
                permMap.put("isHave", permIdsHaveSet.contains(perm.getId()));
                return permMap;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("根据permIds查询权限列表失败，permIds={}");
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> grantQuickConfigToGroup(QuickConfigGrantPermDTO grantDTO) {
        try {
            // ========== 步骤1：基础校验 ==========
            if (!StringUtils.hasText(grantDTO.getGroupId())) {
                return Result.fail("小组ID不能为空");
            }
            // 兼容「可空」需求：如果Map为空（size=0），则跳过权限更新逻辑
            Map<String, List<Long>> quickConfigPermMap = grantDTO.getQuickConfigIds();
            if (quickConfigPermMap.isEmpty()) {
                log.warn("快捷配置权限映射为空，仅更新小组快捷配置关联关系，不更新权限");

                // 若Map为空，快捷配置ID列表也为空，直接更新t_sys_group_quick_config为空
                String quickConfigIdsStr = "";
                QueryWrapper<SysGroupQuickConfigPO> queryWrapper = new QueryWrapper<>();
                queryWrapper.select("id", "group_id", "quick_config_ids")
                        .eq("group_id", grantDTO.getGroupId());
                SysGroupQuickConfigPO existPO = sysGroupQuickConfigMapper.selectOne(queryWrapper);

                if (existPO != null) {
                    existPO.setQuickConfigIds(quickConfigIdsStr);
                    sysGroupQuickConfigMapper.updateById(existPO);
                } else {
                    SysGroupQuickConfigPO newPO = new SysGroupQuickConfigPO();
                    newPO.setGroupId(grantDTO.getGroupId());
                    newPO.setQuickConfigIds(quickConfigIdsStr);
                    sysGroupQuickConfigMapper.insert(newPO);
                }
                return Result.ok(true).message("快捷配置权限映射为空，仅更新小组关联关系成功");
            }

            // ========== 步骤2：提取所有快捷配置ID，校验并更新perm_ids_have字段 ==========
            Set<String> quickConfigIds = quickConfigPermMap.keySet();
            List<SysQuickConfigPO> configList = this.list(
                    new LambdaQueryWrapper<SysQuickConfigPO>()
                            .in(SysQuickConfigPO::getId, quickConfigIds)
            );
            if (configList.size() != quickConfigIds.size()) {
                Set<String> existConfigIds = configList.stream()
                        .map(SysQuickConfigPO::getId)
                        .collect(Collectors.toSet());
                Set<String> notExistIds = quickConfigIds.stream()
                        .filter(id -> !existConfigIds.contains(id))
                        .collect(Collectors.toSet());
                return Result.fail("快捷配置ID不存在：" + String.join(",", notExistIds));
            }

            // 遍历更新perm_ids_have
            for (SysQuickConfigPO configPO : configList) {
                String configId = configPO.getId().toString(); // 修复：Long转String，避免类型不匹配
                List<Long> permIdList = quickConfigPermMap.get(configId);
                // 兼容空权限列表：置空perm_ids_have
                String permIdsHaveStr = permIdList == null || permIdList.isEmpty()
                        ? ""
                        : permIdList.stream().map(String::valueOf).collect(Collectors.joining(","));
                configPO.setPermIdsHave(permIdsHaveStr);
                baseMapper.updateById(configPO);
            }

            // ========== 步骤3：汇总所有权限ID，更新小组权限 ==========
            // ========== 【注释掉更新SysGroupPermissionPO的逻辑】==========
        /*
        Set<String> allPermIds = new HashSet<>();
        quickConfigPermMap.values().forEach(permIdList -> {
            if (permIdList != null && !permIdList.isEmpty()) {
                allPermIds.addAll(permIdList.stream().map(String::valueOf).collect(Collectors.toSet()));
            }
        });

        if (!allPermIds.isEmpty()) {
            Result<Boolean> result = userPermissionService.updateGroupPermissions(
                    grantDTO.getGroupId(),
                    new ArrayList<>(allPermIds)
            );
            if (!ResultCodeEnum.SUCCESS.getCode().equals(result.getCode()) || !Boolean.TRUE.equals(result.getData())) {
                return Result.fail("更新小组权限失败：" + result.getMessage());
            }
        }
        */

            // ========== 步骤4：更新t_sys_group_quick_config表 ==========
            String quickConfigIdsStr = String.join(",", quickConfigIds);
            QueryWrapper<SysGroupQuickConfigPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("id", "group_id", "quick_config_ids")
                    .eq("group_id", grantDTO.getGroupId());
            SysGroupQuickConfigPO existPO = sysGroupQuickConfigMapper.selectOne(queryWrapper);

            if (existPO != null) {
                existPO.setQuickConfigIds(quickConfigIdsStr);
                sysGroupQuickConfigMapper.updateById(existPO);
            } else {
                SysGroupQuickConfigPO newPO = new SysGroupQuickConfigPO();
                newPO.setGroupId(grantDTO.getGroupId());
                newPO.setQuickConfigIds(quickConfigIdsStr);
                sysGroupQuickConfigMapper.insert(newPO);
            }

            return Result.ok(true).message("快捷配置权限授权成功，已同步更新perm_ids_have字段");

        } catch (Exception e) {
            log.error("快捷配置权限给小组失败", e);
            return Result.fail("快捷配置权限给小组失败：" + e.getMessage());
        }
    }
}