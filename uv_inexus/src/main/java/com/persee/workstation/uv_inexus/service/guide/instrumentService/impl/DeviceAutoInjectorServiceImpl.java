/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.instrumentService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceAutoInjectorDAO;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.InjectorTrayParamDAO;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceAutoInjectorPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.InjectionTrayParamPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceAutoInjectorService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Slf4j
public class DeviceAutoInjectorServiceImpl implements DeviceAutoInjectorService {

    @Autowired
    DeviceAutoInjectorDAO dao;

    @Autowired
    InjectorTrayParamDAO paramDAO;

    @Autowired
    DeviceInfoService infoService;

    @Value("请先连接仪器！")
    private String misSerialNumber;

    @Override
    public R<Boolean> setInjectorConf(DeviceAutoInjectorPO po) {
        if (!StringUtils.hasText(po.getDeviceSerialNumber())){
            log.info(misSerialNumber);
            return R.fail(misSerialNumber);
        }
        log.info("发送消息给仪器配置设置");


        log.info("配置成功，维护数据库");
        LambdaQueryWrapper<DeviceAutoInjectorPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceAutoInjectorPO::getDeviceSerialNumber, po.getDeviceSerialNumber());

        if (dao.selectCount(queryWrapper) == 0) {
            log.info("没有记录，插入数据");
            po.setCreateBy(po.getUpdateBy());
            for (int i = 0; i < 2; i++) {
                if (dao.insert(po) > 0) {
                    log.info("插入成功");
                    return R.ok();
                }
            }
        } else {
            log.info("有记录，更新数据");
            for (int i = 0; i < 2; i++) {
                if (dao.update(po, queryWrapper)>0) {
                    log.info("更新成功");
                    return R.ok();
                }
            }
        }
        //仪器设置成功，数据库保存失败，请检查数据库设置
        return R.fail("00201002");
    }

    @Override
    public R<Boolean> setInjectorTray(List<InjectionTrayParamPO> poList) {
        String deviceSerialNumber = null;
        if (poList.isEmpty()) {
            //没有数据
            return R.fail("0000002A");
        }
        for (InjectionTrayParamPO paramPO : poList) {
            if(StringUtils.hasText(paramPO.getDeviceSerialNumber())) {
                deviceSerialNumber = paramPO.getDeviceSerialNumber();
                break;
            }
        }

        if (!StringUtils.hasText(deviceSerialNumber)) {
            log.info(misSerialNumber);
            return R.fail(misSerialNumber);
        }
        for (InjectionTrayParamPO paramPO : poList) {
            if (!StringUtils.hasText(paramPO.getDeviceSerialNumber())){
                paramPO.setDeviceSerialNumber(deviceSerialNumber);
            }

            log.info("调用仪器设置");

            LambdaQueryWrapper<InjectionTrayParamPO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(InjectionTrayParamPO::getDeviceSerialNumber,deviceSerialNumber)
                    .eq(InjectionTrayParamPO::getPanel,paramPO.getPanel());

            if (paramDAO.selectCount(queryWrapper) == 0) {
                paramPO.setCreateBy(paramPO.getUpdateBy());

                for (int i = 0; i < 2; i++) {
                    if (paramDAO.insert(paramPO) > 0) {
                        log.info("插入成功");
                        break;
                    }
                }
            } else {
                log.info("更新数据");
                for (int i = 0; i < 2; i++) {
                    if (paramDAO.update(paramPO, queryWrapper) > 0) {
                        log.info("更新成功");
                        break;
                    }
                }
            }
        }

        return R.ok();
    }

    @Override
    public R<DeviceAutoInjectorPO> queryInjectorConf(String deviceSerialNumber) {
        if (!StringUtils.hasText(deviceSerialNumber)) {
            log.info(misSerialNumber);
            return R.fail(misSerialNumber);
        }

        LambdaQueryWrapper<DeviceAutoInjectorPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceAutoInjectorPO::getDeviceSerialNumber,deviceSerialNumber);

        DeviceAutoInjectorPO injectorPO = dao.selectOne(queryWrapper);
        if (injectorPO == null) {
            //设置默认值
            injectorPO = new DeviceAutoInjectorPO();
            injectorPO.setDeviceSerialNumber(deviceSerialNumber);
            injectorPO.setCreateBy(deviceSerialNumber);

        } else {
            injectorPO.setParamList(selectInjectorTray(deviceSerialNumber));
        }
        return R.ok(injectorPO);

    }

    @Override
    public List<InjectionTrayParamPO> selectInjectorTray(String deviceSerialNumber) {

        if (!StringUtils.hasText(deviceSerialNumber)) {
            log.info(misSerialNumber);
            return null;
        }
        LambdaQueryWrapper<InjectionTrayParamPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InjectionTrayParamPO::getDeviceSerialNumber,deviceSerialNumber);

        return paramDAO.selectList(queryWrapper);
    }

    @Override
    public R<Integer> checkConnectStatus(String deviceSerialNumber) {

        String ip = infoService.getIp(deviceSerialNumber);

        log.info("工具类通过ip去查找连接状态");


        return R.ok(1);

    }
}
