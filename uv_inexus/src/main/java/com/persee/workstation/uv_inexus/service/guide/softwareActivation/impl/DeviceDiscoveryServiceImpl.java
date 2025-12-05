/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.softwareActivation.impl;

import com.alibaba.excel.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.guide.softwareActivation.DeviceDiscoveryServiceMapper;
import com.persee.workstation.uv_inexus.dto.softwareActivation.DeviceInfoDTO;
import com.persee.workstation.uv_inexus.po.guide.softwareActivation.SoftwareActivationPO;
import com.persee.workstation.uv_inexus.service.guide.softwareActivation.DeviceDiscoveryService;
import com.persee.workstation.uv_inexus.util.changeUtil.ResponseDeal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceDiscoveryServiceImpl implements DeviceDiscoveryService {

    private final DeviceDiscoveryServiceMapper softwareActivationMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 状态定义
    private static final int STATUS_HISTORY = 0;
    private static final int STATUS_NEW = 1;
    private static final int STATUS_DUPLICATE = 2;


    @Override
    public List<DeviceInfoDTO> discoverDevices(int broadcastPort, int timeout) {
        log.info("设备发现功能：端口={}, 超时={}ms", broadcastPort, timeout);
        return new ArrayList<>();
    }


//    @Override
//    public List<DeviceInfoDTO> generateMockDeviceData() {
//        List<DeviceInfoDTO> mockData = new ArrayList<>();
//        String currentTime = LocalDateTime.now().format(DATE_FORMATTER);
//
//
//        String sn = ResponseDeal.Sn;
//
//        if (sn == null || sn.isEmpty()) {
//            sn = "UNKNOWN-SN";
//        }
//
//
//        sn = sn.substring(0, 16);
//        // 使用解析好的序列号替代
//        mockData.add(buildDeviceInfo(sn, currentTime));
//
//        return mockData;
//    }

    @Override
    public List<DeviceInfoDTO> generateMockDeviceData() {
        try {
            List<DeviceInfoDTO> mockData = new ArrayList<>();
            String currentTime = LocalDateTime.now().format(DATE_FORMATTER);
            String sn = ResponseDeal.Sn;

            if (sn == null || sn.isEmpty()) {
                return mockData;
            }else if(sn.length() >= 16){
                sn = sn.substring(0, 16);
            }
            // 使用解析好的序列号替代
            mockData.add(buildDeviceInfo(sn, currentTime));

            return mockData;
        }finally {
//            ResponseDeal.Sn = null;
        }
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DeviceInfoDTO> addNonDuplicateDevicesToDb() {
        return addNonDuplicateDevicesToDb1(null);
    }

    /**
     * 带参处理方法：自动新增/更新符合条件的设备
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DeviceInfoDTO> addNonDuplicateDevicesToDb1(Map<String, String> searchParams) {
        log.info("===== 自动执行设备处理（新增/更新） =====");

        // 1. 用搜索参数过滤模拟数据（模糊匹配）
        List<DeviceInfoDTO> filteredDevices = getFilteredDevices(searchParams);
        if (filteredDevices.isEmpty()) {
            log.info("无符合条件的设备，无需处理");
            return new ArrayList<>();
        }
        String targetNumbers = filteredDevices.stream()
                .map(DeviceInfoDTO::getDeviceNumber)
                .collect(Collectors.joining(","));
        log.info("过滤后待处理设备：{}条，编号：{}", filteredDevices.size(), targetNumbers);

        // 2. 数据库中精确匹配设备编号
        Map<String, SoftwareActivationPO> existingMap = getExistingDevicesFromDb(filteredDevices);
        log.info("数据库中已存在的设备：{}", existingMap.keySet().stream().collect(Collectors.joining(",")));

        // 3. 处理设备：已存在则更新状态为2，不存在则新增为1
        List<DeviceInfoDTO> result = new ArrayList<>();
        String currentTime = LocalDateTime.now().format(DATE_FORMATTER);

        for (DeviceInfoDTO device : filteredDevices) {
            String deviceNumber = device.getDeviceNumber();
            log.info("\n----- 处理设备：{} -----", deviceNumber);

            if (existingMap.containsKey(deviceNumber)) {
                // 已存在：更新状态为重复设备（2）
                updateToDuplicate(device, existingMap.get(deviceNumber), currentTime);
            } else {
                // 不存在：强制新增为新设备（1）
                forceAddNewDevice(device, currentTime);
            }
            result.add(device);
        }

        log.info("===== 设备处理完成 =====");
        return result;
    }


    @Override
    public IPage<DeviceInfoDTO> getAddedDevicesPage(Page<DeviceInfoDTO> page, Map<String, String> searchParams) {
        log.info("===== 启动分页查询 =====");

        // 自动处理设备（新增/更新）
        addNonDuplicateDevicesToDb1(searchParams);

        // 1. 构建查询条件，添加排序规则
        LambdaQueryWrapper<SoftwareActivationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SoftwareActivationPO::getIsAddStatus, STATUS_NEW, STATUS_DUPLICATE, STATUS_HISTORY)
                // 关键：按isAddStatus降序排序（2 → 1 → 0）
                .orderByDesc(SoftwareActivationPO::getIsAddStatus);

        // 模糊查询条件
        if (searchParams!= null && searchParams.containsKey("deviceNumber")) {
            String keyword = searchParams.get("deviceNumber").trim().toLowerCase();
            if (!keyword.isEmpty()) {
                queryWrapper.eq(SoftwareActivationPO::getDeviceNumber, keyword);
                log.info("数据库模糊查询条件：deviceNumber LIKE %{}%，排序：isAddStatus DESC", keyword);
            }
        }

        // 2. 执行分页查询（已包含排序）
        Page<SoftwareActivationPO> poPage = softwareActivationMapper.selectPage(
                new Page<>(page.getCurrent(), page.getSize()), queryWrapper);
        log.info("数据库查询结果：总记录数={}，按isAddStatus降序排序", poPage.getTotal());

        // 3. 转换为DTO返回
        List<DeviceInfoDTO> dtoList = poPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Page<DeviceInfoDTO> resultPage = new Page<>();
        resultPage.setRecords(dtoList);
        resultPage.setTotal(poPage.getTotal());
        resultPage.setCurrent(poPage.getCurrent());
        resultPage.setSize(poPage.getSize());

        log.info("===== 分页查询结束 =====");
        return resultPage;
    }


    // 获取过滤后的设备列表（模拟数据模糊查询）
    private List<DeviceInfoDTO> getFilteredDevices(Map<String, String> searchParams) {
        List<DeviceInfoDTO> allDevices = generateMockDeviceData();
        if (CollectionUtils.isNotEmpty(allDevices)){
            return allDevices;
        }
        if (searchParams == null || searchParams.isEmpty()) {
            return allDevices;
        }

        String keyword = searchParams.getOrDefault("deviceNumber", "").trim().toLowerCase();
        if (keyword.isEmpty()) {
            return allDevices;
        }

        List<DeviceInfoDTO> filtered = allDevices.stream()
                .filter(device -> device.getDeviceNumber()!= null
                        && device.getDeviceNumber().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        log.info("模拟数据模糊过滤结果：关键词'{}'，匹配{}条设备", keyword, filtered.size());
        return filtered;
    }


    // 从数据库查询已存在的设备（精确匹配编号）
    private Map<String, SoftwareActivationPO> getExistingDevicesFromDb(List<DeviceInfoDTO> devices) {
        List<String> deviceNumbers = devices.stream()
                .map(DeviceInfoDTO::getDeviceNumber)
                .collect(Collectors.toList());
        LambdaQueryWrapper<SoftwareActivationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SoftwareActivationPO::getDeviceNumber, deviceNumbers);
        List<SoftwareActivationPO> existingPOs = softwareActivationMapper.selectList(queryWrapper);
        return existingPOs.stream()
                .collect(Collectors.toMap(SoftwareActivationPO::getDeviceNumber, po -> po));
    }


    // 更新重复设备状态
    private void updateToDuplicate(DeviceInfoDTO device, SoftwareActivationPO existingPO, String currentTime) {
        String deviceNumber = device.getDeviceNumber();
        LambdaUpdateWrapper<SoftwareActivationPO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SoftwareActivationPO::getId, existingPO.getId())
                .set(SoftwareActivationPO::getIsAddStatus, STATUS_NEW)
                .set(SoftwareActivationPO::getModificationTime, currentTime);
        int updateRows = softwareActivationMapper.update(null, updateWrapper);
        log.info("设备{}：已存在，状态更新为2（重复），影响行数={}", deviceNumber, updateRows);
        device.setIsAddStatus(STATUS_NEW);
        device.setId(existingPO.getId());
    }


    // 强制新增设备
    private void forceAddNewDevice(DeviceInfoDTO device, String currentTime) {
        String deviceNumber = device.getDeviceNumber();
        log.info("设备{}：数据库不存在，执行新增", deviceNumber);

        // 构建PO对象
        device.setIsAddStatus(STATUS_DUPLICATE);
        device.setModificationTime(currentTime);
        SoftwareActivationPO newPO = convertToPO(device);

        // 执行插入
        int insertRows = softwareActivationMapper.insert(newPO);
        log.info("设备{}：插入数据库，影响行数={}", deviceNumber, insertRows);

        // 验证插入结果
        if (insertRows <= 0) {
            log.error("设备{}：插入失败，重试一次", deviceNumber);
            insertRows = softwareActivationMapper.insert(newPO);
            if (insertRows <= 0) {
                throw new RuntimeException("设备" + deviceNumber + "新增失败");
            }
        }

        // 确认新增成功
        SoftwareActivationPO insertedPO = softwareActivationMapper.selectOne(
                new LambdaQueryWrapper<SoftwareActivationPO>()
                        .eq(SoftwareActivationPO::getDeviceNumber, deviceNumber)
                        .eq(SoftwareActivationPO::getIsAddStatus, STATUS_DUPLICATE)
        );
        if (insertedPO == null) {
            throw new RuntimeException("设备" + deviceNumber + "新增后查询不到");
        }

        log.info("设备{}：新增成功，ID={}", deviceNumber, insertedPO.getId());
        device.setId(insertedPO.getId());
    }


    // 构建设备DTO
    private DeviceInfoDTO buildDeviceInfo(String deviceNumber, String currentTime) {
        DeviceInfoDTO dto = new DeviceInfoDTO();
        dto.setDeviceNumber(deviceNumber);
//        dto.setDeviceIp(Ip);
        dto.setModificationTime(currentTime);
        dto.setAdditionalFunctions("默认功能");
        return dto;
    }


    // DTO转PO
    private SoftwareActivationPO convertToPO(DeviceInfoDTO dto) {
        SoftwareActivationPO po = new SoftwareActivationPO();
        po.setDeviceNumber(dto.getDeviceNumber());
        po.setDeviceIp(dto.getDeviceIp());
        po.setType(dto.getType());
        po.setModificationTime(dto.getModificationTime());
        po.setActivationStatus(dto.getActivationStatus());
        po.setAdditionalFunctions(dto.getAdditionalFunctions());
        po.setIsAddStatus(dto.getIsAddStatus());
        return po;
    }


    // PO转DTO
    private DeviceInfoDTO convertToDTO(SoftwareActivationPO po) {
        DeviceInfoDTO dto = new DeviceInfoDTO();
        dto.setId(po.getId());
        dto.setDeviceNumber(po.getDeviceNumber());
        dto.setDeviceIp(po.getDeviceIp());
        dto.setType(po.getType());
        dto.setModificationTime(po.getModificationTime());
        dto.setActivationStatus(po.getActivationStatus());
        dto.setAdditionalFunctions(po.getAdditionalFunctions());
        dto.setIsAddStatus(po.getIsAddStatus());
        return dto;
    }

    /**
     * 重置所有设备的isAddStatus为0（历史状态）
     * 前端关闭弹窗时调用
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetAllDeviceAddStatus(Map<String, Object> params) {
        String type = MapUtils.getString(params, "type");
        String deviceSerialNumber = MapUtils.getString(params, "deviceSerialNumber");

        if (!StringUtils.equals(type,"0")){
            log.info("===== 点击了取消，不添加 =====");
            LambdaUpdateWrapper<SoftwareActivationPO> eq = new LambdaUpdateWrapper<SoftwareActivationPO>()
                    .eq(SoftwareActivationPO::getDeviceNumber, deviceSerialNumber);
            int i = softwareActivationMapper.delete(eq);
        }
        log.info("===== 执行所有设备isAddStatus重置为0 =====");

        // 构建更新条件：更新所有记录的isAddStatus为0
        LambdaUpdateWrapper<SoftwareActivationPO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(SoftwareActivationPO::getIsAddStatus, STATUS_HISTORY);

        // 执行更新
        int updateRows = softwareActivationMapper.update(null, updateWrapper);
        log.info("重置完成，共影响{}条记录", updateRows);

        // 返回更新结果（只要影响行数≥0就视为成功，即使没有数据可更新）
        return updateRows >= 0;
    }
}
