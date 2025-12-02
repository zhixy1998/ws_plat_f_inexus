/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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
