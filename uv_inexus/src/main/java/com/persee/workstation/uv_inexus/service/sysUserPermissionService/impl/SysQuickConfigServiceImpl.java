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
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysQuickConfigPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysQuickConfigService;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.UserPermissionService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Override
    public Result<List<Map<String, Object>>> getQuickConfigTree(String groupId) {
        try {
            // ========== 步骤1：获取该小组绑定的快捷配置ID集合 ==========
            Set<String> haveConfigIds = new HashSet<>();
            if (StringUtils.hasText(groupId)) {
                // 1.1 查询t_sys_group_quick_config表中该小组的记录
                QueryWrapper<SysGroupQuickConfigPO> queryWrapper = new QueryWrapper<>();
                queryWrapper.select("quick_config_ids")
                        .eq("group_id", groupId);
                SysGroupQuickConfigPO groupConfig = sysGroupQuickConfigMapper.selectOne(queryWrapper);

                // 1.2 拆分逗号分隔的ID字符串为集合
                if (groupConfig != null && StringUtils.hasText(groupConfig.getQuickConfigIds())) {
                    String[] ids = groupConfig.getQuickConfigIds().split(",");
                    // 过滤空字符串，避免无效匹配
                    for (String id : ids) {
                        if (StringUtils.hasText(id)) {
                            haveConfigIds.add(id.trim());
                        }
                    }
                }
            }

            // ========== 步骤2：查询所有快捷配置，构建基础Map列表 ==========
            List<SysQuickConfigPO> allConfig = baseMapper.selectList(null);
            List<Map<String, Object>> allConfigMap = allConfig.stream().map(po -> {
                Map<String, Object> configMap = new HashMap<>();
                // 映射原有所有字段
                configMap.put("id", po.getId());
                configMap.put("createBy", po.getCreateBy() == null ? "" : po.getCreateBy());
                configMap.put("updateBy", po.getUpdateBy() == null ? "" : po.getUpdateBy());
                configMap.put("name", po.getName());
                configMap.put("permIds", po.getPermIds());
                configMap.put("parentId", po.getParentId());
                configMap.put("quickConfigType", po.getQuickConfigType());
                // 核心：仅匹配到的节点添加isHave=true，未匹配的不添加该字段
                if (haveConfigIds.contains(po.getId())) {
                    configMap.put("isHave", true);
                }
                // 初始化children字段（空列表）
                configMap.put("children", new ArrayList<Map<String, Object>>());
                return configMap;
            }).collect(Collectors.toList());

            // ========== 步骤3：构建树形结构（基于Map） ==========
            List<Map<String, Object>> rootList = new ArrayList<>();
            for (Map<String, Object> dtoMap : allConfigMap) {
                Long parentId = (Long) dtoMap.get("parentId");
                if (parentId == 0) {
                    // 一级节点：加入根列表
                    rootList.add(dtoMap);
                } else {
                    // 二级节点：找到父节点并添加到children
                    for (Map<String, Object> parentMap : allConfigMap) {
                        String parentMapId = (String) parentMap.get("id");
                        if (parentId.equals(Long.parseLong(parentMapId))) {
                            List<Map<String, Object>> children = (List<Map<String, Object>>) parentMap.get("children");
                            children.add(dtoMap);
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

    @Override
    @Transactional(rollbackFor = Exception.class) // 事务保证双表操作一致性
    public Result<Boolean> grantQuickConfigToGroup(QuickConfigGrantPermDTO grantDTO) {
        try {
            // 1. 查询所有指定的快捷配置
            List<SysQuickConfigPO> configList = this.list(
                    new LambdaQueryWrapper<SysQuickConfigPO>()
                            .in(SysQuickConfigPO::getId, grantDTO.getQuickConfigIds())
            );
            if (configList.isEmpty()) {
                return Result.fail("快捷配置ID不存在");
            }

            // 2. 汇总所有perm_ids（去重、过滤空值）
            Set<String> allPermIds = new HashSet<>();
            for (SysQuickConfigPO configPO : configList) {
                String permIds = configPO.getPermIds();
                if (StringUtils.hasText(permIds)) {
                    // 拆分perm_ids（逗号分隔）并加入集合去重
                    String[] permIdArray = permIds.split(",");
                    allPermIds.addAll(Arrays.asList(permIdArray));
                }
            }

            if (allPermIds.isEmpty()) {
                return Result.fail("快捷配置未绑定任何权限");
            }

            // 3. 调用更新小组权限方法（操作t_sys_group_permission）
            Result<Boolean> result = userPermissionService.updateGroupPermissions(grantDTO.getGroupId(), new ArrayList<>(allPermIds));

            // 4. 根据Result的code和data判断结果
            if (!ResultCodeEnum.SUCCESS.getCode().equals(result.getCode()) || !Boolean.TRUE.equals(result.getData())) {
                return Result.fail("更新小组权限失败：" + result.getMessage());
            }

            // 1. 基础校验（小组ID非空，避免空指针）
            if (!StringUtils.hasText(grantDTO.getGroupId())) {
                return Result.fail("小组ID不能为空");
            }
            // 2. 拼接传入的快捷配置ID为逗号分隔字符串
            String quickConfigIdsStr = grantDTO.getQuickConfigIds().stream()
                    .map(String::valueOf) // Long转String
                    .collect(Collectors.joining(","));

            // 3. 查询该小组是否已存在关联记录
            QueryWrapper<SysGroupQuickConfigPO> queryWrapper = new QueryWrapper<>();
            // 只查询表中存在的字段，避免MyBatis-Plus自动拼接不存在的字段（核心修复）
            queryWrapper.select("id", "group_id", "quick_config_ids")
                    .eq("group_id", grantDTO.getGroupId());
            SysGroupQuickConfigPO existPO = sysGroupQuickConfigMapper.selectOne(queryWrapper);

            // 4. 存在则更新，不存在则新增
            if (existPO != null) {
                // 更新逻辑：仅修改quick_config_ids字段
                existPO.setQuickConfigIds(quickConfigIdsStr);
                sysGroupQuickConfigMapper.updateById(existPO);
            } else {
                // 新增逻辑：构建PO（仅设置表中存在的字段）
                SysGroupQuickConfigPO newPO = new SysGroupQuickConfigPO();
                newPO.setGroupId(grantDTO.getGroupId());
                newPO.setQuickConfigIds(quickConfigIdsStr);
                sysGroupQuickConfigMapper.insert(newPO);
            }

            // ========== 返回最终结果 ==========
            return Result.ok(true).message("快捷配置权限给小组成功，且已同步存储快捷配置ID至t_sys_group_quick_config表");

        } catch (Exception e) {
            log.error("快捷配置权限给小组失败", e);
            return Result.fail("快捷配置权限给小组失败：" + e.getMessage());
        }
    }
}