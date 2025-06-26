package com.persee.workstation.uv_inexus.service.systemManageService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dao.systemManageDao.SystemAlgorithmConfDAO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemAlgorithmConfDTO;
import com.persee.workstation.uv_inexus.po.systemManagePo.SystemAlgorithmConfPO;
import com.persee.workstation.uv_inexus.service.systemManageService.SystemAlgorithmConfService;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SystemAlgorithmConfServiceImpl implements SystemAlgorithmConfService {

    @Autowired
    SystemAlgorithmConfDAO dao;

    @Override
    public List<SystemAlgorithmConfVO> queryAlgorithmConf() {
        List<SystemAlgorithmConfPO> poList = dao.selectList(null);
        List<SystemAlgorithmConfVO> voList = new ArrayList<>();
        for (SystemAlgorithmConfPO po : poList) {
            SystemAlgorithmConfVO vo = new SystemAlgorithmConfVO();
            vo.setAlgorithmName(po.getAlgorithmName());
            vo.setAlgorithmType(po.getAlgorithmType());
            vo.setConfParam(po.getConfParam());
            vo.setConfParamName(po.getConfParamName());
            if (po.getConfUserValue() == null) {
                vo.setConfValue(po.getConfDefaultValue());
            }else {
                vo.setConfValue(po.getConfUserValue());
            }
            vo.setMaxValue(po.getMaxValue());
            vo.setMinValue(po.getMinValue());
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public Boolean saveAlgorithmConf(List<SystemAlgorithmConfDTO> list) {
        for (SystemAlgorithmConfDTO dto : list) {
            LambdaQueryWrapper<SystemAlgorithmConfPO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SystemAlgorithmConfPO::getAlgorithmType, dto.getAlgorithmType())
                    .eq(SystemAlgorithmConfPO::getConfParam, dto.getConfParam());
            SystemAlgorithmConfPO po = dao.selectOne(queryWrapper);

            po.setUpdateBy(dto.getUpdateBy());
            po.setConfUserValue(dto.getConfValue());
            dao.update(po, queryWrapper);
        }
        return true;
    }


    @Override
    public Boolean resetAlgorithmConf() {
        List<SystemAlgorithmConfPO> poList = dao.selectList(null);
        for (SystemAlgorithmConfPO po : poList) {
            po.setConfUserValue(po.getConfDefaultValue());
            dao.updateById(po);
        }
        return true;
    }
}
