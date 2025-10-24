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

package com.persee.workstation.uv_inexus.service.guide.systemManageService.impl;

import com.persee.workstation.uv_inexus.dao.guide.systemManageDao.SystemManageDAO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemManageDTO;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemManagePO;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemManageService;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemManageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class SystemManageServiceImpl implements SystemManageService {

    @Autowired
    SystemManageDAO dao;

    @Value("${sys-manage-default.defaultId}")
    String defaultId;

    @Value("${sys-manage-default.userId}")
    String userId;


    @Override
    public SystemManageVO getSystemManage() {

        SystemManagePO po = dao.selectById(userId);
        SystemManagePO defaultPO = dao.selectById(defaultId);
        SystemManageVO vo = new SystemManageVO();
        BeanUtils.copyProperties(defaultPO, vo);
        if (po != null) {
            BeanUtils.copyProperties(po, vo, getNullPropertyNames(po));
        }

        return vo;
    }

    private static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }
        return emptyNames.toArray(new String[0]);
    }

    @Override
    public Boolean saveSystemManage(SystemManageDTO dto) {
        SystemManagePO po = dao.selectById(userId);
        if (po == null) {
            po = new SystemManagePO();
            BeanUtils.copyProperties(dto, po);
            po.setId(userId);
            po.setCreateBy(dto.getUpdateBy());
            return dao.insert(po) > 0;
        } else {
            BeanUtils.copyProperties(dto, po);
            po.setId(userId);
            return dao.updateById(po) > 0;
        }
    }

    @Override
    public Boolean resetSystemManage(String updateBy) {

        SystemManagePO po = dao.selectById(userId);
        if (po != null) {
            return dao.deleteById(po) > 0;
        } else {
            log.info("没有用户记录");
            return true;
        }
    }

    @Override
    public Boolean changeLanguage(String language) {
        SystemManagePO po = dao.selectById(userId);
        if (po == null) {
            po = new SystemManagePO();
            po.setId(userId);
            po.setLanguage(language);
            return dao.insert(po) > 0;
        } else {
            po.setId(userId);
            po.setLanguage(language);
            return dao.updateById(po) > 0;
        }
    }

    @Override
    public String queryLanguage() {

        SystemManagePO po = dao.selectById(userId);
        SystemManagePO defaultPo = dao.selectById(defaultId);
        if (po != null && StringUtils.hasText(po.getLanguage())) {
            return po.getLanguage();
        } else {
            return defaultPo.getLanguage();
        }
    }

}


