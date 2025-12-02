/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.systemManageService.impl;

import com.persee.workstation.uv_inexus.dao.guide.systemManageDao.SystemBusinessParamDAO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemBusinessParamDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemBusinessParamPO;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemBusinessParamService;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAutoLogoutConfVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemBusinessParamVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemManageMeasureConfVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemStandCurveConfVO;
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

    @Override
    public R<SystemManageMeasureConfVO> queryMeasureConf() {
        SystemBusinessParamPO po = dao.selectById(userId);
        SystemBusinessParamPO defPo = dao.selectById(defaultId);
        SystemManageMeasureConfVO vo = new SystemManageMeasureConfVO();
        BeanUtils.copyProperties(defPo, vo);
        if (po != null) {
            BeanUtils.copyProperties(po, vo, getNullPropertyNames(po));
        }
        return R.ok(vo);
    }

    @Override
    public R<SystemStandCurveConfVO> queryStandCurveConf() {
        SystemBusinessParamPO po = dao.selectById(userId);
        SystemBusinessParamPO defPo = dao.selectById(defaultId);
        SystemStandCurveConfVO vo = new SystemStandCurveConfVO();
        BeanUtils.copyProperties(defPo, vo);
        if (po != null) {
            BeanUtils.copyProperties(po, vo, getNullPropertyNames(po));
        }
        return R.ok(vo);
    }

    @Override
    public R<SystemAutoLogoutConfVO> queryAutoLogoutConf() {
        SystemBusinessParamPO po = dao.selectById(userId);
        SystemBusinessParamPO defPo = dao.selectById(defaultId);
        SystemAutoLogoutConfVO vo = new SystemAutoLogoutConfVO();
        BeanUtils.copyProperties(defPo, vo);
        if (po != null) {
            BeanUtils.copyProperties(po, vo, getNullPropertyNames(po));
        }
        return R.ok(vo);
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
