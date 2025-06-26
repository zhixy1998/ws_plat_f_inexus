package com.persee.workstation.uv_inexus.service.systemManageService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dao.systemManageDao.SystemBusinessParamDAO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemBusinessParamDTO;
import com.persee.workstation.uv_inexus.po.systemManagePo.SystemBusinessParamPO;
import com.persee.workstation.uv_inexus.service.systemManageService.SystemBusinessParamService;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemBusinessParamVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SystemBusinessParamServiceImpl implements SystemBusinessParamService {

    @Autowired
    SystemBusinessParamDAO dao;

    @Override
    public List<SystemBusinessParamVO> queryBusinessParam() {
        List<SystemBusinessParamPO> poList = dao.selectList(null);
        List<SystemBusinessParamVO> voList = new ArrayList<>();
        for (SystemBusinessParamPO po : poList) {
            SystemBusinessParamVO vo = new SystemBusinessParamVO();
            vo.setBusinessName(po.getBusinessName());
            vo.setBusinessType(po.getBusinessType());
            vo.setConfParam(po.getConfParam());
            vo.setParamName(po.getParamName());
            vo.setParamType(po.getParamType());
            vo.setParamMinValue(po.getParamMinValue());
            vo.setParamMaxValue(po.getParamMaxValue());
            if (po.getParamUserValue() == null) {
                vo.setParamValue(po.getParamDefaultValue());
            } else {
                vo.setParamValue(po.getParamUserValue());
            }
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public Boolean saveBusinessParam(List<SystemBusinessParamDTO> list) {
        for (SystemBusinessParamDTO dto : list) {
            LambdaQueryWrapper<SystemBusinessParamPO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SystemBusinessParamPO::getBusinessType, dto.getBusinessType())
                    .eq(SystemBusinessParamPO::getConfParam, dto.getConfParam());
            SystemBusinessParamPO po = dao.selectOne(queryWrapper);
            po.setUpdateBy(dto.getUpdateBy());
            po.setParamUserValue(dto.getParamValue());
            dao.update(po, queryWrapper);
        }

        return true;
    }

    @Override
    public Boolean resetBusinessParam() {
        List<SystemBusinessParamPO> poList = dao.selectList(null);
        for (SystemBusinessParamPO po : poList) {
            po.setParamUserValue(po.getParamDefaultValue());
            dao.updateById(po);
        }
        return true;
    }


}
