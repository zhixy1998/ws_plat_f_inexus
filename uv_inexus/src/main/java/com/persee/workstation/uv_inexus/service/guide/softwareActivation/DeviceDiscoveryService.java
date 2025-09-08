package com.persee.workstation.uv_inexus.service.guide.softwareActivation;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.softwareActivation.DeviceInfoDTO;

public interface DeviceDiscoveryService {
    // 保留必要的三个方法
    List<DeviceInfoDTO> discoverDevices(int broadcastPort, int timeout);
    List<DeviceInfoDTO> generateMockDeviceData();
    List<DeviceInfoDTO> addNonDuplicateDevicesToDb();
    // 新增带参方法定义（关键）
    List<DeviceInfoDTO> addNonDuplicateDevicesToDb1(Map<String, String> searchParams); 
    // 新增：分页查询新增设备（支持搜索）
    IPage<DeviceInfoDTO> getAddedDevicesPage(Page<DeviceInfoDTO> page, Map<String, String> searchParams);

    // 新增：重置所有设备的isAddStatus为0的方法
    boolean resetAllDeviceAddStatus();
}