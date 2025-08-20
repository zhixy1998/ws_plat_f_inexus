package com.persee.workstation.uv_inexus.service.sysUserPermissionService;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.GroupQueryDTO;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.GroupSaveDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPO;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

public interface SysGroupService extends IService<SysGroupPO> {

    /**
     * 分页查询小组
     */
    Result<PageView<SysGroupPO>> getGroupPage(Page<SysGroupPO> page, GroupQueryDTO queryDTO);

    /**
     * 新增小组
     */
    Result<String> addGroup(GroupSaveDTO saveDTO);

    /**
     * 修改小组
     */
    Result<String> updateGroup(GroupSaveDTO saveDTO);

    /**
     * 批量删除小组
     */
    Result<String> deleteBatch(List<String> ids);

    /**
     * 根据ID查询小组
     */
    Result<SysGroupPO> getGroupById(String id);

    /**
     * 根据ID删除单个小组
     */
    Result<String> deleteById(String id);
}