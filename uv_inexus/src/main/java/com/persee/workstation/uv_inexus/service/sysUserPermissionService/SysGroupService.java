/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

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