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

package com.persee.workstation.uv_inexus.service.guide.instrumentService.impl;


import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceMonitorEnum;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceMonitorService;
import com.persee.workstation.uv_inexus.vo.instrumentVo.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DeviceMonitorServiceImpl implements DeviceMonitorService {
    @Override
    public List<DeviceMonitorVO> queryStatus(DeviceQueryDTO queryDTO) {


        DeviceMonitorVO vo = new DeviceMonitorVO();
        vo.setType(DeviceMonitorEnum.getName("1"));
        DataNameValue value =new DataNameValue();
        value.setDataValue("09");
        value.setDataKey("s");

        List<DataNameValue> neeList = new ArrayList<>();
        neeList.add(value);

        vo.setData(neeList);
        ArrayList<DeviceMonitorVO> deviceMonitorVOS = new ArrayList<>();
        deviceMonitorVOS.add(vo);
        return deviceMonitorVOS;
    }

    @Override
    public DeviceComponentMonitorVO componentQuery(DeviceQueryDTO queryDTO) {
        return null;
    }

    @Override
    public List<DeviceUsageStatisticsVO> countUsageInfo(DeviceUsageCountDTO countDTO) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getAllHistoryUsage(LocalDateTime startTime, LocalDateTime endTime, String search) {
        return Collections.emptyList();
    }


}
