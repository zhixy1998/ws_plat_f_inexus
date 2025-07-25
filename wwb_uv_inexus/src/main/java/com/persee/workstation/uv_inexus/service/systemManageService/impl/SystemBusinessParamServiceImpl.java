package com.persee.workstation.uv_inexus.service.systemManageService.impl;

import com.persee.workstation.uv_inexus.dao.systemManageDao.SystemBusinessParamDAO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemBusinessParamDTO;
import com.persee.workstation.uv_inexus.po.systemManagePo.SystemBusinessParamPO;
import com.persee.workstation.uv_inexus.service.systemManageService.SystemBusinessParamService;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemBusinessParamVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;


@Service
@Slf4j
public class SystemBusinessParamServiceImpl implements SystemBusinessParamService {

    @Autowired
    SystemBusinessParamDAO dao;

    @Value("${sys-manage-default.defaultId}")
    String defaultId;

    @Value("${sys-manage-default.userId}")
    String userId;

    @Override
    public SystemBusinessParamVO queryBusinessParam() {
        SystemBusinessParamPO po = dao.selectById(userId);
        SystemBusinessParamPO defPo = dao.selectById(defaultId);
        SystemBusinessParamVO vo = new SystemBusinessParamVO();
        BeanUtils.copyProperties(defPo, vo);
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
    public Boolean saveBusinessParam(SystemBusinessParamDTO dto) {
        SystemBusinessParamPO po = dao.selectById(userId);
        if (po == null) {
            po = new SystemBusinessParamPO();
            BeanUtils.copyProperties(dto, po);
            po.setId(userId);

            po.setCreateBy(dto.getUpdateBy());
            po.setUpdateBy(dto.getUpdateBy());
            return dao.insert(po) > 0;
        } else {

            BeanUtils.copyProperties(dto, po);
            po.setId(userId);
            return dao.updateById(po) > 0;
        }
    }

    @Override
    public Boolean resetBusinessParam() {
        SystemBusinessParamPO po = dao.selectById(userId);
        if (po != null) {
            return dao.deleteById(po) > 0;
        } else {
            log.info("没有用户记录");
            return true;
        }
    }


}
