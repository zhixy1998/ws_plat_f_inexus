/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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