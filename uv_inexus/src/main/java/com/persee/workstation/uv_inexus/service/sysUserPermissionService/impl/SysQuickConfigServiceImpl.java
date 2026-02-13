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
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysQuickConfigMapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigGrantPermDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigSaveDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigTreeDTO;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
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
    public Result<List<QuickConfigTreeDTO>> getQuickConfigTree() {
        // 1. 查询所有快捷配置（平铺列表）
        List<SysQuickConfigPO> allConfig = baseMapper.selectList(null);

        // 2. 转换为树形DTO并构建层级结构
        List<QuickConfigTreeDTO> treeList = buildQuickConfigTree(allConfig);

        return Result.ok(treeList);
    }

    /**
     * 构建快捷配置树形结构（核心方法）
     */
    private List<QuickConfigTreeDTO> buildQuickConfigTree(List<SysQuickConfigPO> allConfig) {
        // 步骤1：将PO转换为TreeDTO
        List<QuickConfigTreeDTO> allTreeDTO = allConfig.stream().map(po -> {
            QuickConfigTreeDTO dto = new QuickConfigTreeDTO();
            // 基础字段映射
            dto.setId(po.getId().toString()); // 转字符串匹配示例
            dto.setCreateBy(po.getCreateBy() == null ? "" : po.getCreateBy());
            dto.setUpdateBy(po.getUpdateBy() == null ? "" : po.getUpdateBy());
            dto.setName(po.getName());
            dto.setPermIds(po.getPermIds());
            dto.setParentId(po.getParentId());
            dto.setQuickConfigType(po.getQuickConfigType());
            dto.setChildren(new ArrayList<>()); // 初始化空children
            return dto;
        }).collect(Collectors.toList());

        // 步骤2：构建父子层级（parentId=0为一级，其他为子级）
        List<QuickConfigTreeDTO> rootList = new ArrayList<>();
        for (QuickConfigTreeDTO dto : allTreeDTO) {
            if (dto.getParentId() == 0) {
                // 一级节点：加入根列表
                rootList.add(dto);
            } else {
                // 二级节点：找到父节点并添加到children
                for (QuickConfigTreeDTO parentDTO : allTreeDTO) {
                    if (Long.parseLong(parentDTO.getId()) == dto.getParentId()) {
                        parentDTO.getChildren().add(dto);
                        break;
                    }
                }
            }
        }

        return rootList;
    }

    @Override
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

            // 3. 调用更新小组权限方法
            Result<Boolean> result = userPermissionService.updateGroupPermissions(grantDTO.getGroupId(), new ArrayList<>(allPermIds));

            // 4. 根据Result的code和data判断结果
            if (ResultCodeEnum.SUCCESS.getCode().equals(result.getCode()) && Boolean.TRUE.equals(result.getData())) {
                return Result.ok(true).message("快捷配置权限给小组成功");
            } else {
                return Result.fail("更新小组权限失败：" + result.getMessage());
            }
        } catch (Exception e) {
            log.error("快捷配置权限给小组失败", e);
            return Result.fail("快捷配置权限给小组失败：" + e.getMessage());
        }
    }
}