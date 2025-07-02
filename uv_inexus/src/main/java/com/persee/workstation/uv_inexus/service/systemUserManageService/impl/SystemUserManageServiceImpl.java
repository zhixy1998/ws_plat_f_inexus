package com.persee.workstation.uv_inexus.service.systemUserManageService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dao.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.systemUserManageDto.CreateSystemUserDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.systemUserManageService.SystemUserManageService;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.SystemUserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SystemUserManageServiceImpl implements SystemUserManageService {

    @Autowired
    SystemUserManageDAO dao;

    private static final String createSysUser = "1";

    @Override
    public R<Boolean> addSystemUser(CreateSystemUserDTO dto) {
        String permissionList = dao.getUserPermissionList(dto.getUpdateBy());
        if (!permissionList.contains(createSysUser)) {
            log.info("用户没有权限添加系统用户");
            return R.fail("没有权限添加系统用户");
        }

        LambdaQueryWrapper<SystemUserManagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemUserManagePO::getUserName, dto.getUserName())
                .or()
                .eq(SystemUserManagePO::getPhone, dto.getPhone())
                .or()
                .eq(SystemUserManagePO::getEmail, dto.getEmail());
        if (!dao.selectList(queryWrapper).isEmpty()) {
            log.info("当前用户名、手机号、或邮箱已存在");
            return R.fail("当前用户名、手机号、或邮箱已存在");
        }

        SystemUserManagePO po = new SystemUserManagePO();
        BeanUtils.copyProperties(dto, po);
        po.setCreateBy(dto.getUpdateBy());
        po.setUpdateBy(dto.getUpdateBy());
        return R.ok(dao.insert(po) > 0);

    }

    @Override
    public R<Boolean> addGroupUser(CreateSystemUserDTO dto) {
        return null;
    }

    @Override
    public R<SystemUserInfoVO> getUserInfo(String userName) {
        return null;
    }

    @Override
    public R<Boolean> deleteSystemUser(String userName) {
        return null;
    }

    @Override
    public R<Boolean> deleteGroupUser(String userName) {
        return null;
    }
}
