package com.persee.workstation.uv_inexus.service.softwareActivation.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.softwareActivation.DeviceDiscoveryServiceMapper;
import com.persee.workstation.uv_inexus.dto.softwareActivation.DeviceInfoDTO;
import com.persee.workstation.uv_inexus.po.softwareActivation.SoftwareActivationPO;
import com.persee.workstation.uv_inexus.service.softwareActivation.DeviceDiscoveryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceDiscoveryServiceImpl implements DeviceDiscoveryService {

    private final DeviceDiscoveryServiceMapper softwareActivationMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    @Override
    public List<DeviceInfoDTO> discoverDevices(int broadcastPort, int timeout) {
        log.info("设备发现功能：端口={}, 超时={}ms", broadcastPort, timeout);
        return new ArrayList<>();
    }


    @Override
    public List<DeviceInfoDTO> generateMockDeviceData() {
        List<DeviceInfoDTO> mockData = new ArrayList<>();
        String currentTime = LocalDateTime.now().format(DATE_FORMATTER);
        
        // 生成模拟数据时，确保包含所有必要字段
        mockData.add(buildDeviceInfo("DEV001", "192.168.51.10", "Windows设备", currentTime));
        mockData.add(buildDeviceInfo("DEV002", "192.168.51.11", "Linux设备", currentTime));
        mockData.add(buildDeviceInfo("DEV003", "192.168.51.12", "嵌入式设备", currentTime));
        mockData.add(buildDeviceInfo("DEV004", "192.168.51.13", "Windows设备", currentTime));
        mockData.add(buildDeviceInfo("DEV005", "192.168.51.14", "新增设备", currentTime));
        
        log.info("生成5条模拟设备数据：{}", mockData.stream()
                .map(DeviceInfoDTO::getDeviceNumber)
                .collect(Collectors.joining(",")));
        return mockData;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DeviceInfoDTO> addNonDuplicateDevicesToDb() {
        // 1. 重置所有设备的isAddStatus为0
        LambdaUpdateWrapper<SoftwareActivationPO> resetWrapper = new LambdaUpdateWrapper<>();
        resetWrapper.set(SoftwareActivationPO::getIsAddStatus, 0);
        int resetCount = softwareActivationMapper.update(null, resetWrapper);
        log.info("已将数据库中 {} 条设备的isAddStatus重置为0", resetCount);

        // 2. 获取模拟数据
        List<DeviceInfoDTO> allDevices = generateMockDeviceData();
        List<String> allDeviceNumbers = allDevices.stream()
                .map(DeviceInfoDTO::getDeviceNumber)
                .collect(Collectors.toList());
        log.info("待检查的设备号：{}", String.join(",", allDeviceNumbers));

        // 3. 查询数据库中已存在的设备
        LambdaQueryWrapper<SoftwareActivationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SoftwareActivationPO::getDeviceNumber, allDeviceNumbers);
        List<SoftwareActivationPO> existingPOs = softwareActivationMapper.selectList(queryWrapper);
        List<String> existingNumbers = existingPOs.stream()
                .map(SoftwareActivationPO::getDeviceNumber)
                .collect(Collectors.toList());
        log.info("数据库中已存在的设备号：{}", existingNumbers.isEmpty() ? "无" : String.join(",", existingNumbers));

        // 4. 筛选新设备（数据库中不存在的）
        List<DeviceInfoDTO> newDevices = allDevices.stream()
                .filter(device -> !existingNumbers.contains(device.getDeviceNumber()))
                .distinct()
                .collect(Collectors.toList());
        log.info("筛选出的新设备（待插入）：{}", newDevices.stream()
                .map(DeviceInfoDTO::getDeviceNumber)
                .collect(Collectors.joining(",")));

        if (newDevices.isEmpty()) {
            log.info("无新设备需要新增");
            return new ArrayList<>();
        }

        // 5. 插入新设备（关键：补全所有非空字段）
        List<DeviceInfoDTO> resultDevices = new ArrayList<>();
        String currentTime = LocalDateTime.now().format(DATE_FORMATTER);
        
        for (DeviceInfoDTO dto : newDevices) {
            // 补全所有可能的非空字段（根据数据库表结构调整）
            dto.setActivationStatus(0); // 未激活
            // dto.setActivationTime(currentTime); // 激活时间（必须赋值，若数据库非空）
            dto.setModificationTime(currentTime); // 修改时间
            // dto.setExpirationDate("9999-12-31 23:59:59"); // 过期时间（必须赋值，符合DATETIME格式）
            dto.setAdditionalFunctions("默认功能");
            dto.setIsAddStatus(1); // 标记为新增
            
            // 转换为PO并插入
            SoftwareActivationPO po = convertToPO(dto);
            int insertResult = softwareActivationMapper.insert(po); // 执行插入并获取结果
            if (insertResult > 0) {
                log.info("设备 {} 插入成功，ID: {}", dto.getDeviceNumber(), po.getId());
                dto.setId(po.getId());
                resultDevices.add(dto);
            } else {
                log.error("设备 {} 插入失败", dto.getDeviceNumber());
            }
        }

        log.info("本次共成功新增 {} 条设备", resultDevices.size());
        return resultDevices;
    }


    @Override
    public IPage<DeviceInfoDTO> getAddedDevicesPage(Page<DeviceInfoDTO> page, Map<String, String> searchParams) {
        log.info("Service分页查询触发：查询isAddStatus=1的设备"); // 确认是否进入该方法

        LambdaQueryWrapper<SoftwareActivationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SoftwareActivationPO::getIsAddStatus, 1); // 只查新增标记的数据

        // 支持设备号搜索
        if (searchParams.containsKey("deviceNumber") && !searchParams.get("deviceNumber").isEmpty()) {
            queryWrapper.like(SoftwareActivationPO::getDeviceNumber, searchParams.get("deviceNumber"));
            log.info("搜索设备号：{}", searchParams.get("deviceNumber"));
        }

        // 执行查询
        Page<SoftwareActivationPO> poPage = softwareActivationMapper.selectPage(
            new Page<>(page.getCurrent(), page.getSize()), queryWrapper);
        log.info("数据库查询结果：总条数={}, 数据={}", 
                poPage.getTotal(), 
                poPage.getRecords().stream().map(SoftwareActivationPO::getDeviceNumber).collect(Collectors.joining(",")));

        // 转换为DTO
        List<DeviceInfoDTO> dtoList = poPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Page<DeviceInfoDTO> resultPage = new Page<>();
        resultPage.setRecords(dtoList);
        resultPage.setTotal(poPage.getTotal());
        resultPage.setCurrent(poPage.getCurrent());
        resultPage.setSize(poPage.getSize());
        return resultPage;
    }


    // 构建DeviceInfoDTO（补全所有必要字段）
    private DeviceInfoDTO buildDeviceInfo(String deviceNumber, String ip, String type, String currentTime) {
        DeviceInfoDTO dto = new DeviceInfoDTO();
        dto.setDeviceNumber(deviceNumber);
        dto.setDeviceIp(ip);
        dto.setType(type);
        dto.setActivationStatus(0);
        // dto.setActivationTime(currentTime);
        dto.setModificationTime(currentTime);
        // dto.setExpirationDate("9999-12-31 23:59:59");
        dto.setAdditionalFunctions("默认功能");
        dto.setIsAddStatus(1);
        return dto;
    }


    // DTO转PO（确保所有字段映射，尤其是非空字段）
    private SoftwareActivationPO convertToPO(DeviceInfoDTO dto) {
        SoftwareActivationPO po = new SoftwareActivationPO();
        po.setDeviceNumber(dto.getDeviceNumber());
        po.setDeviceIp(dto.getDeviceIp());
        po.setType(dto.getType());
        po.setActivationStatus(dto.getActivationStatus());
        po.setActivationTime(dto.getActivationTime()); // 关键：映射激活时间
        po.setModificationTime(dto.getModificationTime());
        po.setExpirationDate(dto.getExpirationDate()); // 关键：映射过期时间
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
        dto.setActivationStatus(po.getActivationStatus());
        // dto.setActivationTime(po.getActivationTime());
        dto.setModificationTime(po.getModificationTime());
        // dto.setExpirationDate(po.getExpirationDate());
        dto.setAdditionalFunctions(po.getAdditionalFunctions());
        dto.setIsAddStatus(po.getIsAddStatus());
        return dto;
    }
}