/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.sysUserPermissionController;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigGrantPermDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.QuickConfigSaveDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysQuickConfigPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysQuickConfigService;
import com.persee.workstation.uv_inexus.utils.OperateLog.OperateLog;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * 快捷配置Controller
 */
@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/quick-config")
@Api(tags = "快捷配置管理")
public class SysQuickConfigController {

    @Autowired
    private SysQuickConfigService sysQuickConfigService;

    @GetMapping("/")
    @ApiOperation("分页查询快捷配置")
    public Result<PageView<SysQuickConfigPO>> getQuickConfigPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            QuickConfigQueryDTO queryDTO) {
        Page<SysQuickConfigPO> page = new Page<>(pageNum, pageSize);
        return sysQuickConfigService.getQuickConfigPage(page, queryDTO);
    }

    @GetMapping("/{id}")
    @ApiOperation("根据ID查询快捷配置")
    public Result<SysQuickConfigPO> getQuickConfigById(@PathVariable Long id) {
        return sysQuickConfigService.getQuickConfigById(id);
    }

    @PostMapping("/add")
    @ApiOperation("新增快捷配置")
//    @ButtonPermission("revokeActivation")
    @OperateLog(module = "快捷配置管理", description = "新增快捷配置", recordParams = true)
    public Result<String> addQuickConfig(@RequestBody @Valid QuickConfigSaveDTO saveDTO) {
        return sysQuickConfigService.addQuickConfig(saveDTO);
    }

    @PutMapping("/update")
    @ApiOperation("修改快捷配置")
    @OperateLog(module = "快捷配置管理", description = "修改快捷配置", recordParams = true)
    public Result<String> updateQuickConfig(@RequestBody @Valid QuickConfigSaveDTO saveDTO) {
        return sysQuickConfigService.updateQuickConfig(saveDTO);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("根据ID删除单个快捷配置")
    @OperateLog(module = "快捷配置管理", description = "删除单个快捷配置", recordParams = true)
    public Result<String> deleteById(@PathVariable Long id) {
        return sysQuickConfigService.deleteById(id);
    }

    @PostMapping("/deleteBatch")
    @ApiOperation("批量删除快捷配置")
    @OperateLog(module = "快捷配置管理", description = "批量删除快捷配置", recordParams = true)
    public Result<String> deleteBatch(@RequestBody List<Long> ids) {
        return sysQuickConfigService.deleteBatch(ids);
    }

    /**
     * 查询快捷配置树形结构（一级+二级）
     */
    @GetMapping("/tree")
    @ApiOperation("查询快捷配置树形结构（按小组ID匹配拥有状态）")
    public Result<List<Map<String, Object>>> getQuickConfigTree(
            @ApiParam(value = "小组ID", required = false)
            @RequestParam(required = false) String groupId,
            @ApiParam(value = "快捷配置类型：0-普通用户，1-GMP", required = false)
            @RequestParam(required = false) Integer quickConfigType) {
        return sysQuickConfigService.getQuickConfigTree(groupId, quickConfigType);
    }

    @PostMapping("/grant-group-perm")
    @ApiOperation("快捷配置权限给小组")
    @OperateLog(module = "快捷配置管理", description = "快捷配置权限给小组", recordParams = true)
    public Result<Boolean> grantQuickConfigToGroup(@RequestBody @Valid QuickConfigGrantPermDTO grantDTO) {
        return sysQuickConfigService.grantQuickConfigToGroup(grantDTO);
    }
}