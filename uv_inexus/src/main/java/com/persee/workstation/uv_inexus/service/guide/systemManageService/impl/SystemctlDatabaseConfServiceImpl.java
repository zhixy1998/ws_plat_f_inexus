/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.systemManageService.impl;

import com.persee.workstation.uv_inexus.dao.guide.systemManageDao.SystemDatabaseConfDAO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemCustomManageDTO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemDataBaseConfDTO;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemDatabaseConfPO;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemctlDatabaseConfService;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemDataBaseVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemManageUserConfVO;
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
public class SystemctlDatabaseConfServiceImpl implements SystemctlDatabaseConfService {

    @Autowired
    SystemDatabaseConfDAO dao;

    @Value("${sys-manage-default.defaultId}")
    String defaultId;

    @Value("${sys-manage-default.userId}")
    String userId;

    @Override
    public Boolean saveDatabaseConf(SystemDataBaseConfDTO dto) {

        SystemDatabaseConfPO po = dao.selectById(userId);


        if (po == null) {
            po = new SystemDatabaseConfPO();
            SystemDatabaseConfPO defaultPo = dao.selectById(defaultId);
            BeanUtils.copyProperties(defaultPo, po);

            BeanUtils.copyProperties(dto, po);

            po.setId(userId);
            po.setCreateBy(dto.getUpdateBy());
            po.setUpdateBy(dto.getUpdateBy());

            return dao.insert(po) > 0;
        } else {
            BeanUtils.copyProperties(dto, po);
            po.setId(userId);
            po.setUpdateBy(dto.getUpdateBy());
            return dao.updateById(po) > 0;
        }
    }

    @Override
    public Boolean saveCustomDatabaseConf(SystemCustomManageDTO dto) {
        SystemDatabaseConfPO po = dao.selectById(userId);
        if (po == null) {
            po = new SystemDatabaseConfPO();
            SystemDatabaseConfPO defaultPo = dao.selectById(defaultId);
            BeanUtils.copyProperties(defaultPo, po);
            BeanUtils.copyProperties(dto, po);

            po.setId(userId);
            po.setCreateBy(dto.getUpdateBy());
            po.setUpdateBy(dto.getUpdateBy());

            return dao.insert(po) > 0;
        } else {
            BeanUtils.copyProperties(dto, po);
            po.setId(userId);
            po.setUpdateBy(dto.getUpdateBy());
            return dao.updateById(po) > 0;
        }
    }

    @Override
    public SystemManageUserConfVO querySysManageUserConf(String updateBy) {

        SystemDatabaseConfPO userDataPO = dao.selectById(userId);
        SystemDatabaseConfPO confPO = dao.selectById(defaultId);
        SystemManageUserConfVO vo = new SystemManageUserConfVO();

        BeanUtils.copyProperties(confPO, vo);
        if (userDataPO != null) {
            BeanUtils.copyProperties(userDataPO, vo, getNullPropertyNames(userDataPO));
        }
        return vo;
    }
    @Override
    public SystemDataBaseVO queryDatabaseConf(String updateBy) {

        SystemDatabaseConfPO userDataPO = dao.selectById(userId);
        SystemDatabaseConfPO confPO = dao.selectById(defaultId);
        SystemDataBaseVO vo = new SystemDataBaseVO();

        BeanUtils.copyProperties(confPO, vo);
        if (userDataPO != null) {
            BeanUtils.copyProperties(userDataPO, vo, getNullPropertyNames(userDataPO));
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
    public Boolean resetDatabaseConf(String updateBy) {
        SystemDatabaseConfPO po = dao.selectById(userId);
        if (po != null) {
            return dao.deleteById(po) > 0;
        } else {
            log.info("没有用户记录");
        }
        return true;
    }


    @Override
    public Boolean getDefaultClassicSwitch() {
        SystemDatabaseConfPO po = dao.selectById(userId);
        SystemDatabaseConfPO defaultPo = dao.selectById(defaultId);
        if (po != null && po.getDefaultEnterClassic() != null) {
            return po.getDefaultEnterClassic();
        } else {
            return defaultPo.getDefaultEnterClassic();
        }
    }

}
