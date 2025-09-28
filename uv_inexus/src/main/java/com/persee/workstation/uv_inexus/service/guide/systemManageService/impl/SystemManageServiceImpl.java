package com.persee.workstation.uv_inexus.service.guide.systemManageService.impl;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.persee.workstation.uv_inexus.dao.guide.systemManageDao.SystemManageDAO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemManageDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemManagePO;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemManagerConf;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemManageService;
import com.persee.workstation.uv_inexus.vo.systemManageVo.ConfigVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.LongDistanceConfigVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemManageVO;
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
    public Result<String> changeLanguage(String language) {
        Integer res = dao.changeLanguage(language);
        if (res == 1) {
            return Result.ok(language);
        }
        return Result.fail("fail");
    }

    @Override
    public Result<Boolean> setConfiguration(ConfigVO configVO) throws JsonProcessingException {


        String configJson = JSON.toJSONString(configVO);
        Integer res = dao.setConfiguration(configJson);
        if (res == 1) {
            return Result.ok(true);
        }
        return Result.fail();
    }

    @Override
    public Result<Boolean> longDistanceConfig(LongDistanceConfigVO longDistanceConfigVO) {
        Integer remoteControl = longDistanceConfigVO.getRemote_control();
        Integer remoteDataAccess = longDistanceConfigVO.getRemote_data_access();
        Integer res = dao.longDistanceConfig(remoteDataAccess, remoteControl);
        if (res == 1) {
            return Result.ok(true);
        }
        return Result.fail();
    }

    @Override
    public Result<SystemManagerConf> changeSystemManagerConf(SystemManagerConf systemManagerConf) throws JsonProcessingException {
        String language = systemManagerConf.getLanguage();
        SystemManagerConf newConf = new SystemManagerConf();
        Integer res = dao.changeLanguage(language);
        if (res == 0) {
            return Result.fail("语言修改失败");
        }
        newConf.setLanguage(language);

        ConfigVO configVO = systemManagerConf.getConfig();


        String configJson = JSON.toJSONString(configVO);
        Integer res1 = dao.setConfiguration(configJson);
        if (res1 == 0) {
            return Result.fail("物联代理配置信息修改失败");
        }
        newConf.setConfig(configVO);
        LongDistanceConfigVO longDistanceConfigVO = systemManagerConf.getLongDistanceConfigVO();
        Integer remoteControl = longDistanceConfigVO.getRemote_control();
        Integer remoteDataAccess = longDistanceConfigVO.getRemote_data_access();
        Integer res2 = dao.longDistanceConfig(remoteDataAccess, remoteControl);
        if (res2 == 0) {
            return Result.fail("远程访问配置修改失败");
        }
        newConf.setLongDistanceConfigVO(longDistanceConfigVO);
        return Result.ok(newConf);
    }

    @Override
    public Result<SystemManagerConf> getSystemManagerConf() throws JsonProcessingException {
        SystemManagerConf systemManagerConf = new SystemManagerConf();
        SystemManagePO systemManagePO = dao.selectById(userId);
        systemManagerConf.setLanguage(systemManagePO.getLanguage());
        String configuration = systemManagePO.getConfiguration();
        ConfigVO config = JSON.parseObject(configuration, ConfigVO.class);

        systemManagerConf.setConfig(config);
        LongDistanceConfigVO longDistanceConfigVO = getLongDistanceConfigVO(systemManagePO);
        systemManagerConf.setLongDistanceConfigVO(longDistanceConfigVO);
        return Result.ok(systemManagerConf);
    }

    private static LongDistanceConfigVO getLongDistanceConfigVO(SystemManagePO systemManagePO) {
        LongDistanceConfigVO longDistanceConfigVO = new LongDistanceConfigVO();
        Boolean remoteControl = systemManagePO.getRemoteControl();
        if (remoteControl) {
            longDistanceConfigVO.setRemote_control(1);
        } else {
            longDistanceConfigVO.setRemote_control(0);
        }

        Boolean remoteDataAccess = systemManagePO.getRemoteDataAccess();
        if (remoteDataAccess) {
            longDistanceConfigVO.setRemote_data_access(1);
        } else {
            longDistanceConfigVO.setRemote_data_access(0);
        }
        return longDistanceConfigVO;
    }

}


