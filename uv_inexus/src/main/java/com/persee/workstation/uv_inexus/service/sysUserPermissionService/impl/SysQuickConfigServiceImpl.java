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
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.*;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigGrantPermDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigSaveDTO;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.*;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysQuickConfigService;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.UserPermissionService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

    @Autowired
    private SysGroupPermissionMapper sysGroupPermissionMapper;

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
    public Result<List<Map<String, Object>>> getQuickConfigTree(String groupId, Integer quickConfigType) {
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

            // ========== 步骤2：查询快捷配置，构建基础Map列表（补充perm_ids_have解析） ==========
            QueryWrapper<SysQuickConfigPO> configQueryWrapper = new QueryWrapper<>();
            if (quickConfigType != null) {
                configQueryWrapper.eq("quick_config_type", quickConfigType);
            }
            List<SysQuickConfigPO> allConfig = baseMapper.selectList(configQueryWrapper);
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
            if (!StringUtils.hasText(grantDTO.getGroupId())) {
                return Result.fail("小组ID不能为空");
            }
            String groupId = grantDTO.getGroupId();
            Map<String, List<Long>> quickConfigPermMap = grantDTO.getQuickConfigIds();

            // ========== 新增核心步骤：查询该小组已绑定的所有快捷配置ID ==========
            Set<String> boundQuickConfigIds = new HashSet<>();
            QueryWrapper<SysGroupQuickConfigPO> groupConfigQuery = new QueryWrapper<>();
            groupConfigQuery.select("quick_config_ids")
                    .eq("group_id", groupId)
                    .eq("deleted", 0); // 排除逻辑删除的记录
            SysGroupQuickConfigPO groupConfigPO = sysGroupQuickConfigMapper.selectOne(groupConfigQuery);
            if (groupConfigPO != null && StringUtils.hasText(groupConfigPO.getQuickConfigIds())) {
                // 解析已绑定的快捷配置ID（如"1,2,3" → [1,2,3]）
                boundQuickConfigIds = Arrays.stream(groupConfigPO.getQuickConfigIds().split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toSet());
            }
            log.info("小组{}已绑定的快捷配置ID：{}", groupId, boundQuickConfigIds);

            // ========== 处理空值场景（前端传递空的quickConfigIds） ==========
            if (quickConfigPermMap.isEmpty()) {
                log.warn("快捷配置权限映射为空，清空小组所有快捷配置关联关系和对应权限");

                // 1. 清空已绑定的所有快捷配置的perm_ids_have，并删除对应权限
                if (!boundQuickConfigIds.isEmpty()) {
                    List<SysQuickConfigPO> boundConfigList = this.list(
                            new LambdaQueryWrapper<SysQuickConfigPO>()
                                    .in(SysQuickConfigPO::getId, boundQuickConfigIds)
                    );
                    for (SysQuickConfigPO configPO : boundConfigList) {
                        // 清空perm_ids_have
                        configPO.setPermIdsHave("");
                        baseMapper.updateById(configPO);
                        // 删除该配置下的所有权限
                        if (StringUtils.hasText(configPO.getPermIds())) {
                            try {
                                Set<Long> scopeIds = Arrays.stream(configPO.getPermIds().split(","))
                                        .map(String::trim)
                                        .filter(StringUtils::hasText)
                                        .map(Long::parseLong)
                                        .collect(Collectors.toSet());
                                LambdaQueryWrapper<SysGroupPermissionPO> delWrapper = new LambdaQueryWrapper<>();
                                delWrapper.eq(SysGroupPermissionPO::getGroupId, groupId)
                                        .in(SysGroupPermissionPO::getPermId, scopeIds.stream().map(String::valueOf).collect(Collectors.toSet()));
                                sysGroupPermissionMapper.delete(delWrapper);
                                log.info("清空小组{}下快捷配置{}的所有权限，权限范围：{}", groupId, configPO.getId(), scopeIds);
                            } catch (NumberFormatException e) {
                                log.error("快捷配置ID={}的perm_ids格式错误，无法删除对应权限", configPO.getId(), e);
                            }
                        }
                    }
                }

                // 2. 更新t_sys_group_quick_config表
                String quickConfigIdsStr = "";
                if (groupConfigPO != null) {
                    groupConfigPO.setQuickConfigIds(quickConfigIdsStr);
//                    groupConfigPO.setUpdateUser(grantDTO.getUpdateBy());
                    sysGroupQuickConfigMapper.updateById(groupConfigPO);
                } else {
                    SysGroupQuickConfigPO newPO = new SysGroupQuickConfigPO();
                    newPO.setGroupId(groupId);
                    newPO.setQuickConfigIds(quickConfigIdsStr);
//                    newPO.setCreateUser(grantDTO.getUpdateBy());
//                    newPO.setUpdateUser(grantDTO.getUpdateBy());
                    sysGroupQuickConfigMapper.insert(newPO);
                }
                return Result.ok(true).message("快捷配置权限映射为空，已清空小组所有快捷配置关联关系和对应权限");
            }

            // ========== 步骤2：提取前端传递的快捷配置ID，并找出「整体取消的ID」 ==========
            Set<String> currentQuickConfigIds = quickConfigPermMap.keySet();
            // 整体取消的ID = 已绑定的ID - 本次传递的ID
            Set<String> cancelAllConfigIds = new HashSet<>(boundQuickConfigIds);
            cancelAllConfigIds.removeAll(currentQuickConfigIds);
            log.info("小组{}整体取消的快捷配置ID：{}", groupId, cancelAllConfigIds);

            // ========== 处理「整体取消的快捷配置」：清空perm_ids_have + 删除对应权限 ==========
            if (!cancelAllConfigIds.isEmpty()) {
                List<SysQuickConfigPO> cancelConfigList = this.list(
                        new LambdaQueryWrapper<SysQuickConfigPO>()
                                .in(SysQuickConfigPO::getId, cancelAllConfigIds)
                );
                for (SysQuickConfigPO configPO : cancelConfigList) {
                    // 1. 清空该配置的perm_ids_have
                    configPO.setPermIdsHave("");
                    baseMapper.updateById(configPO);
                    log.info("清空快捷配置{}的perm_ids_have（整体取消）", configPO.getId());

                    // 2. 删除该配置下的所有权限（基于perm_ids范围）
                    if (StringUtils.hasText(configPO.getPermIds())) {
                        try {
                            Set<Long> scopeIds = Arrays.stream(configPO.getPermIds().split(","))
                                    .map(String::trim)
                                    .filter(StringUtils::hasText)
                                    .map(Long::parseLong)
                                    .collect(Collectors.toSet());
                            LambdaQueryWrapper<SysGroupPermissionPO> delWrapper = new LambdaQueryWrapper<>();
                            delWrapper.eq(SysGroupPermissionPO::getGroupId, groupId)
                                    .in(SysGroupPermissionPO::getPermId, scopeIds.stream().map(String::valueOf).collect(Collectors.toSet()));
                            int deleteCount = sysGroupPermissionMapper.delete(delWrapper);
                            log.info("删除小组{}下快捷配置{}的所有权限，删除条数：{}，权限范围：{}",
                                    groupId, configPO.getId(), deleteCount, scopeIds);
                        } catch (NumberFormatException e) {
                            log.error("快捷配置ID={}的perm_ids格式错误，无法删除对应权限", configPO.getId(), e);
                        }
                    }
                }
            }

            // ========== 步骤3：校验前端传递的快捷配置ID ==========
            List<SysQuickConfigPO> configList = this.list(
                    new LambdaQueryWrapper<SysQuickConfigPO>()
                            .in(SysQuickConfigPO::getId, currentQuickConfigIds)
            );
            if (configList.size() != currentQuickConfigIds.size()) {
                Set<String> existConfigIds = configList.stream()
                        .map(p -> p.getId().toString())
                        .collect(Collectors.toSet());
                Set<String> notExistIds = currentQuickConfigIds.stream()
                        .filter(id -> !existConfigIds.contains(id))
                        .collect(Collectors.toSet());
                return Result.fail("快捷配置ID不存在：" + String.join(",", notExistIds));
            }

            // ========== 核心：解析权限范围+选择的权限（原有逻辑保留，添加日志+异常捕获） ==========
            Set<Long> configPermScopeIds = new HashSet<>(); // 所有快捷配置的perm_ids范围
            Set<Long> currentSelectPermIds = new HashSet<>(); // 本次选择的权限

            for (SysQuickConfigPO configPO : configList) {
                String configId = configPO.getId().toString();
                List<Long> permIdList = quickConfigPermMap.get(configId);

                // 1. 解析perm_ids（添加异常捕获）
                if (StringUtils.hasText(configPO.getPermIds())) {
                    try {
                        Set<Long> scopeIds = Arrays.stream(configPO.getPermIds().split(","))
                                .map(String::trim)
                                .filter(StringUtils::hasText)
                                .map(Long::parseLong)
                                .collect(Collectors.toSet());
                        configPermScopeIds.addAll(scopeIds);
                        log.info("快捷配置ID={}，perm_ids解析结果：{}", configId, scopeIds);
                    } catch (NumberFormatException e) {
                        log.error("快捷配置ID={}的perm_ids格式错误，值为：{}，无法解析", configId, configPO.getPermIds(), e);
                        continue;
                    }
                } else {
                    log.warn("快捷配置ID={}的perm_ids为空，无法限定权限范围", configId);
                }

                // 2. 收集本次选择的权限
                if (permIdList != null && !permIdList.isEmpty()) {
                    currentSelectPermIds.addAll(permIdList);
                }

                // 3. 原有逻辑：更新perm_ids_have
                String permIdsHaveStr = permIdList == null || permIdList.isEmpty()
                        ? ""
                        : permIdList.stream().map(String::valueOf).collect(Collectors.joining(","));
                configPO.setPermIdsHave(permIdsHaveStr);
                baseMapper.updateById(configPO);
            }

            // ========== 调试日志：打印关键集合 ==========
            log.info("所有快捷配置的权限范围（configPermScopeIds）：{}", configPermScopeIds);
            log.info("本次选择的权限ID（currentSelectPermIds）：{}", currentSelectPermIds);

            // ========== 计算需要删除的权限 ==========
            Set<Long> needDeletePermIds = new HashSet<>();
            for (Long scopeId : configPermScopeIds) {
                if (!currentSelectPermIds.contains(scopeId)) {
                    needDeletePermIds.add(scopeId);
                }
            }
            log.info("需要删除的权限ID（needDeletePermIds）：{}", needDeletePermIds);

            // ========== 精准删除取消的权限 ==========
            if (!needDeletePermIds.isEmpty()) {
                LambdaQueryWrapper<SysGroupPermissionPO> delWrapper = new LambdaQueryWrapper<>();
                delWrapper.eq(SysGroupPermissionPO::getGroupId, groupId)
                        .in(SysGroupPermissionPO::getPermId, needDeletePermIds.stream().map(String::valueOf).collect(Collectors.toSet()));
                int deleteCount = sysGroupPermissionMapper.delete(delWrapper);
                log.info("删除小组{}下的权限条数：{}，删除的权限ID：{}", groupId, deleteCount, needDeletePermIds);
            } else {
                log.info("无需要删除的权限（needDeletePermIds为空）");
            }

            // ========== 新增选择的权限（去重） ==========
            List<SysGroupPermissionPO> insertList = new ArrayList<>();
            for (Long permId : currentSelectPermIds) {
                if (configPermScopeIds.contains(permId)) { // 仅处理perm_ids范围内的权限
                    LambdaQueryWrapper<SysGroupPermissionPO> existWrapper = new LambdaQueryWrapper<>();
                    existWrapper.eq(SysGroupPermissionPO::getGroupId, groupId)
                            .eq(SysGroupPermissionPO::getPermId, permId.toString());
                    if (sysGroupPermissionMapper.selectCount(existWrapper) == 0) {
                        SysGroupPermissionPO groupPermPO = new SysGroupPermissionPO();
                        groupPermPO.setGroupId(groupId);
                        groupPermPO.setPermId(permId.toString());
                        insertList.add(groupPermPO);
                    }
                }
            }
            if (!insertList.isEmpty()) {
                int insertCount = sysGroupPermissionMapper.insertBatch(insertList);
                log.info("新增小组{}下的权限条数：{}，新增的权限ID：{}", groupId, insertCount, insertList.stream().map(p -> p.getPermId()).collect(Collectors.toList()));
            }

            // ========== 原有逻辑：更新t_sys_group_quick_config ==========
            String quickConfigIdsStr = String.join(",", currentQuickConfigIds);
            if (groupConfigPO != null) {
                groupConfigPO.setQuickConfigIds(quickConfigIdsStr);
//                groupConfigPO.setUpdateUser(grantDTO.getUpdateBy());
                sysGroupQuickConfigMapper.updateById(groupConfigPO);
            } else {
                SysGroupQuickConfigPO newPO = new SysGroupQuickConfigPO();
                newPO.setGroupId(groupId);
                newPO.setQuickConfigIds(quickConfigIdsStr);
//                newPO.setCreateUser(grantDTO.getUpdateBy());
//                newPO.setUpdateUser(grantDTO.getUpdateBy());
                sysGroupQuickConfigMapper.insert(newPO);
            }

            return Result.ok(true).message("快捷配置权限授权成功，已同步更新t_sys_group_permission表");

        } catch (Exception e) {
            log.error("快捷配置权限给小组失败", e);
            return Result.fail("快捷配置权限给小组失败：" + e.getMessage());
        }
    }
}