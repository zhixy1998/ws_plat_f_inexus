package com.persee.workstation.uv_inexus.service.guide.systemManageService.impl;

import com.persee.workstation.uv_inexus.dao.guide.systemManageDao.SystemDatabaseConfDAO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.InitSystemConf;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemDataBaseConfDTO;
import com.persee.workstation.uv_inexus.enums.systemManageEnums.AutoTimeEnum;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemDatabaseConfPO;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemctlDatabaseConfService;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemDataBaseVO;
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
    public SystemDataBaseVO queryDatabaseConf() {

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
    public Boolean changeFastLoginSwitch(Boolean switchStatus, String updateBy) {
        SystemDatabaseConfPO po = dao.selectById(userId);
        if (po == null) {
            po = new SystemDatabaseConfPO();
            po.setId(userId);
            po.setCreateBy(updateBy);
            po.setUpdateBy(updateBy);
            po.setFastLoginSwitch(switchStatus);
            return dao.insert(po) > 0;
        } else {
            po.setUpdateBy(updateBy);
            po.setFastLoginSwitch(switchStatus);
            return dao.updateById(po) > 0;
        }
    }

    public Result<InitSystemConf> updateSystemDatabaseConf(InitSystemConf initSystemConf) {

        SystemDatabaseConfPO systemDatabaseConfPO = dao.selectById("00000000");
        if (systemDatabaseConfPO != null) {
            BeanUtils.copyProperties(initSystemConf, systemDatabaseConfPO);
            dao.updateById(systemDatabaseConfPO);
            return Result.ok(initSystemConf);
        }
        return Result.fail();

    }

    public Result<InitSystemConf> initSystemConf() {
        SystemDatabaseConfPO systemDatabaseConfPO = dao.selectById("00000000");
        if (systemDatabaseConfPO != null) {
            InitSystemConf initSystemConf = new InitSystemConf();
            BeanUtils.copyProperties(systemDatabaseConfPO,initSystemConf);
            return Result.ok(initSystemConf);
        }
        return Result.fail();

    }

    @Override
    public Result<Boolean> updateAutoTime(Integer autoTime) {
        String byTimeEnum = AutoTimeEnum.getByTimeEnum(autoTime);
        SystemDatabaseConfPO systemDatabaseConfPO = dao.selectById("00000000");
        if (systemDatabaseConfPO != null) {
            SystemDatabaseConfPO systemDatabaseConf = new SystemDatabaseConfPO();
            BeanUtils.copyProperties(systemDatabaseConfPO, systemDatabaseConf);
            systemDatabaseConf.setAutoSaveTime(byTimeEnum);
            int i = dao.updateById(systemDatabaseConf);
            if (i == 0) {
                return Result.fail(false);
            }
        }
        return Result.ok(true);
    }

    @Override
    public Result<String> getAutoTime() {
        SystemDatabaseConfPO systemDatabaseConfPO = dao.selectById("00000000");
        if (systemDatabaseConfPO == null) {
            return Result.fail();
        }
        return Result.ok(systemDatabaseConfPO.getAutoSaveTime());
    }


}
