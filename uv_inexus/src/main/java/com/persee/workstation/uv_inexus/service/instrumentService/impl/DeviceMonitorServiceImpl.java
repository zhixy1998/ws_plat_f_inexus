package com.persee.workstation.uv_inexus.service.instrumentService.impl;


import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceMonitorEnum;
import com.persee.workstation.uv_inexus.service.instrumentService.DeviceMonitorService;
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
