package com.persee.workstation.uv_inexus.controller.sysUserPermissionController;

import java.util.List;

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
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.GroupQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.GroupSaveDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysGroupService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/group")
@Api(tags = "小组管理")
public class GroupController {

    @Autowired
    private SysGroupService sysGroupService;

    // 分页查询：返回Result<PageView>
    @GetMapping("/")
    @ApiOperation("分页查询小组")
    public Result<PageView<SysGroupPO>> getGroupPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            GroupQueryDTO queryDTO) {
        Page<SysGroupPO> page = new Page<>(pageNum, pageSize);
        return sysGroupService.getGroupPage(page, queryDTO);  // 直接返回Service的Result结果
    }

    // 其他接口保持不变（已返回Result格式）
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询小组")
    public Result<SysGroupPO> getGroupById(@PathVariable String id) {
        return sysGroupService.getGroupById(id);
    }

    @PostMapping("/addGroup")
    @ApiOperation("新增小组")
    public Result<String> addGroup(@RequestBody @Valid GroupSaveDTO saveDTO) {
        return sysGroupService.addGroup(saveDTO);
    }

    @PutMapping("/updateGroup")
    @ApiOperation("修改小组")
    public Result<String> updateGroup(@RequestBody @Valid GroupSaveDTO saveDTO) {
        return sysGroupService.updateGroup(saveDTO);
    }

    @PostMapping("/deleteBatch")
    @ApiOperation("批量删除小组")
    public Result<String> deleteBatch(@RequestBody List<String> ids) {
        return sysGroupService.deleteBatch(ids);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("根据ID删除单个小组")
    public Result<String> deleteById(@PathVariable String id) {
        return sysGroupService.deleteById(id);
    }
}