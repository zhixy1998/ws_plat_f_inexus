package com.persee.workstation.uv_inexus.service.guide.systemManageService.impl;

import com.persee.workstation.uv_inexus.dao.guide.systemManageDao.SystemAlgorithmConfDAO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemAlgorithmConfDTO;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemAlgorithmConfPO;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemAlgorithmConfService;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class SystemAlgorithmConfServiceImpl implements SystemAlgorithmConfService {

    @Autowired
    SystemAlgorithmConfDAO dao;

    @Value("${sys-manage-default.defaultId}")
    String defaultId;

    @Value("${sys-manage-default.userId}")
    String userId;


    @Override
    public SystemAlgorithmConfVO queryAlgorithmConf() {
        SystemAlgorithmConfPO po = dao.selectById(userId);
        SystemAlgorithmConfPO defPo = dao.selectById(defaultId);
        SystemAlgorithmConfVO vo = new SystemAlgorithmConfVO();

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
    public Boolean saveAlgorithmConf(SystemAlgorithmConfDTO dto) {

        SystemAlgorithmConfPO po = dao.selectById(userId);
        if (po == null) {
            po = new SystemAlgorithmConfPO();
            BeanUtils.copyProperties(dto, po);
            po.setId(userId);
            po.setCreateBy(dto.getUpdateBy());

            return dao.insert(po) > 0;
        } else {

            BeanUtils.copyProperties(dto, po);
            po.setUpdateBy(dto.getUpdateBy());
            po.setId(userId);
            return dao.updateById(po) > 0;
        }

    }


    @Override
    public Boolean resetAlgorithmConf() {

        SystemAlgorithmConfPO po = dao.selectById(userId);
        if (po != null) {
            log.info("删除用户记录");
            return dao.deleteById(po) > 0;
        } else {
            log.info("没有用户记录");
            return true;
        }
    }
}
