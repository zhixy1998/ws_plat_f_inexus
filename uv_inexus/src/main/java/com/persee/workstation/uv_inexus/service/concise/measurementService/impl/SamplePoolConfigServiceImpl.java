/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.SamplePoolConfigDAO;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceInfoDAO;
import com.persee.workstation.uv_inexus.dto.concise.common.FSampleAttachTypeSaveDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.FSamplePoolInfoChangeDto;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceSamplePoolPurpose;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.sphereUsedEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.i18n.LocaleProvider;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.SamplePoolConfigPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ConcisePublicUtilService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.factory_inspection.SamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.factory_inspection.SamplePoolStatus;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("FSamplePoolConfig")
@DS("guide")
@Slf4j
public class SamplePoolConfigServiceImpl extends ServiceImpl<SamplePoolConfigDAO, SamplePoolConfigPO> implements SamplePoolConfigService {

    @Autowired
    SamplePoolConfigDAO dao;

    @Autowired
    DeviceInfoDAO deviceInfoDAO;

    @Autowired
    DevMessageUtil devMessageUtil;

    @Autowired
    DeviceInfoService deviceInfoService;

    @Autowired
    ConcisePublicUtilService concisePublicUtilService;

    @Autowired
    LocaleProvider localeProvider;

    @Override
    public Boolean updateCurrentPoolNum(String deviceSerialNumber, Integer currentPoolNum, String updateBy) {
        if (!StringUtils.hasText(deviceSerialNumber)) {
            log.info("缺少序列号");
            return false;
        }

        LambdaQueryWrapper<SamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(SamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        SamplePoolConfigPO po = dao.selectOne(query);
        if (po == null) {
            po = new SamplePoolConfigPO();
            DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfo(deviceSerialNumber);
            po.setAttachmentType(attachmentInfo.getAttachmentNum());
            po.setDeviceSerialNumber(deviceSerialNumber);
            po.setCurrentPoolNum(currentPoolNum);
            po.setUpdateBy(updateBy);
            List<FSamplePoolStatus> poolInfo = createFSamplePoolStatuses(attachmentInfo.getSamplePoolNum());
            po.setPoolInfo(poolInfo);
            return dao.insert(po) > 0;
        } else {
            po.setCurrentPoolNum(currentPoolNum);
            po.setUpdateBy(updateBy);
            return dao.updateById(po) > 0;
        }
    }

    //积分球的应用场景选项添加
    private static List<FSamplePoolStatus> createFSampleSphereStatuses() {
        List<FSamplePoolStatus> poolInfo = new ArrayList<>();
        poolInfo.add(createStatus(0, sphereUsedEnum.RS.getCode(), "纸板、哑光板材、粗糙塑料等无镜面反光样品"));
        poolInfo.add(createStatus(1, sphereUsedEnum.SR.getCode(), "抛光金属、光盘、粉末压片、高光泽塑料"));
        poolInfo.add(createStatus(2, sphereUsedEnum.RS.getCode(), "薄膜、镜片、太阳膜"));
        return poolInfo;
    }

    private static FSamplePoolStatus createStatus(Integer poolNum, Integer poolPurpose, String poolPurposeName) {
        FSamplePoolStatus status = new FSamplePoolStatus();
        status.setPoolNum(poolNum);
        status.setPoolStatus(false);
        status.setPoolPurpose(poolPurpose);
        status.setPoolPurposeName(poolPurposeName);
        return status;
    }

    private static List<FSamplePoolStatus> createFSamplePoolStatuses(Integer totalPoolNum) {
        List<FSamplePoolStatus> poolInfo = new ArrayList<>();

        for (int i = 1; i < totalPoolNum + 1; i++) {
            FSamplePoolStatus status = new FSamplePoolStatus();
            status.setPoolNum(i);
            status.setPoolStatus(false);
            status.setPoolPurpose(DeviceSamplePoolPurpose.MEASURE.getCode());
            status.setPoolPurposeName(DeviceSamplePoolPurpose.MEASURE.getName());
            poolInfo.add(status);
        }
        return poolInfo;
    }

    @Override
    public Boolean saveSamplePoolInfo(FSampleAttachTypeSaveDTO dto) {

        if (!StringUtils.hasText(dto.getDeviceSerialNumber())) {
            log.info("缺少序列号信息");
            return false;
        }

        LambdaQueryWrapper<SamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(SamplePoolConfigPO::getDeviceSerialNumber, dto.getDeviceSerialNumber());
        SamplePoolConfigPO po = dao.selectOne(query);
        if (po == null) {
            po = new SamplePoolConfigPO();
        }
        BeanUtils.copyProperties(dto, po);

        String s = dto.getDeviceSerialNumber().substring(3, 8);

        DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfoByAttachmentNum(dto.getAttachmentType(), s);
        //此处判断附件类型，对积分球作特殊处理 xzr20260819
        List<DeviceAttachmentTypeVO> shpjereTypelist = deviceInfoService.getAttachmentListByName("球");
        Integer currentAttachmentType = po.getAttachmentType();
        boolean isShpjere = false;
        if (shpjereTypelist != null && !shpjereTypelist.isEmpty()) {
            for (DeviceAttachmentTypeVO vo : shpjereTypelist) {
                //todo xzr 积分球附件查询
                if (Objects.equals(vo.getAttachmentNum(), currentAttachmentType) && attachmentInfo.getSamplePoolNum() == 1) {
                //if (Objects.equals(vo.getAttachmentNum(), currentAttachmentType)) {
                    isShpjere = true;
                    break;
                }
            }
        }

        List<FSamplePoolStatus> list;
        if (isShpjere) {
            list = createFSampleSphereStatuses();
        } else {
            list = createFSamplePoolStatuses(attachmentInfo.getSamplePoolNum());
        }
        po.setPoolInfo(list);
        po.setIsAutoChangePool(false);

        po.setCreateBy(dto.getUpdateBy());
        po.setUpdateBy(dto.getUpdateBy());
        if (StringUtils.hasText(po.getId())) {
            return dao.updateById(po) > 0;
        } else {
            return dao.insert(po) > 0;
        }
    }

    @Override
    public R<Boolean> updateSamplePoolInfo(FSamplePoolInfoChangeDto changeDto) {
        if (!StringUtils.hasText(changeDto.getDeviceSerialNumber())) {
            log.info("未连接仪器");
            //未连接仪器
            return R.fail("0000001D");
        }

        if (!StringUtils.hasText(changeDto.getUpdateBy())) {
            log.info("缺少更新人信息");
            //请登录后操作！
            return R.fail("00307004");
        }

        LambdaQueryWrapper<SamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(SamplePoolConfigPO::getDeviceSerialNumber, changeDto.getDeviceSerialNumber());
        SamplePoolConfigPO po = dao.selectOne(query);
        if (po == null) {
            //未找到该仪器附件配置
            return R.fail("00106001");
        }
        if (changeDto.getIsAutoChangePool() == null && changeDto.getPoolNum() == null && changeDto.getPoolStatus() != null) {
            changeDto.setIsAutoChangePool(changeDto.getPoolStatus());
        }
        if (changeDto.getIsAutoChangePool() != null) {
            if (changeDto.getIsAutoChangePool()) {
                if (po.getPoolInfo() != null && !po.getPoolInfo().isEmpty()) {
                    List<FSamplePoolStatus> list = po.getPoolInfo();
                    for (FSamplePoolStatus status : list) {
                        status.setPoolStatus(true);
                    }
                    po.setPoolInfo(list);
                }
            } else {
                if (po.getPoolInfo() != null && !po.getPoolInfo().isEmpty()) {
                    List<FSamplePoolStatus> list = po.getPoolInfo();
                    for (FSamplePoolStatus status : list) {
                        status.setPoolStatus(false);
                    }
                    po.setPoolInfo(list);
                }
            }
        }

        po.setUpdateBy(changeDto.getUpdateBy());
        if (changeDto.getIsAutoChangePool() != null) {
            if (po.getPoolInfo() != null && !po.getPoolInfo().isEmpty()) {
                List<FSamplePoolStatus> list = po.getPoolInfo();
                if (list.size() == 1) {
                    //没有切换的样品池
                    return R.fail("00106002");
                }
            }

            po.setIsAutoChangePool(changeDto.getIsAutoChangePool());
        }

        if (changeDto.getPoolNum() != null) {
            List<FSamplePoolStatus> list = po.getPoolInfo();
            for (FSamplePoolStatus status : list) {
                if (status.getPoolNum().equals(changeDto.getPoolNum())) {
                    if (changeDto.getPoolStatus() != null) {
                        status.setPoolStatus(changeDto.getPoolStatus());
                    }
                    if (changeDto.getPoolPurpose() != null) {
                        status.setPoolPurpose(changeDto.getPoolPurpose());
                        status.setPoolPurposeName(DeviceSamplePoolPurpose.getName(changeDto.getPoolPurpose()));
                    }
                }
            }
            po.setPoolInfo(list);
        } else {
            if (changeDto.getPoolStatus() != null || changeDto.getPoolPurpose() != null) {
                //请选择要操作的样品池
                return R.fail("00106003");
            }
        }
        boolean b = dao.updateById(po) > 0;
        if (!b) {
            //更新失败
            return R.fail("00000048");
        }
        if (StringUtils.hasText(changeDto.getTaskId()) && po.getIsAutoChangePool()) {
            List<Integer> usefulLocation = getUsefulLocation(changeDto.getDeviceSerialNumber());
            if (changeDto.getUpdateDatabase().equals("concise")) {
                concisePublicUtilService.changeSampleLocation(changeDto.getTaskId(), usefulLocation);
            }
            if (changeDto.getUpdateDatabase().equals("guide")) {
                concisePublicUtilService.changeSampleLocationTwo(changeDto.getTaskId(), usefulLocation);
            }

        }
        return R.ok();
    }

    @Override
    public R<FSamplePoolInfoVO> querySamplePoolInfo(String deviceSerialNumber) {

        LambdaQueryWrapper<SamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(SamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        SamplePoolConfigPO po = dao.selectOne(query);
        if (po == null || po.getPoolInfo() == null || po.getPoolInfo().isEmpty()) {
            //未找到该仪器附件配置
            return R.fail("00106001");
        }

        FSamplePoolInfoVO vo = new FSamplePoolInfoVO();
        BeanUtils.copyProperties(po, vo);
        String locale = localeProvider.getSystemLocale();
        List<FSamplePoolStatus> list = po.getPoolInfo();
        boolean isChinese = "zh-CN".equals(locale) || "zh_CN".equals(locale);
        list.forEach(status -> status.setPoolName(isChinese ? status.getPoolNum() + "号" : "#" + status.getPoolNum()));
        vo.setPoolStatusList(list);
        return R.ok(vo);
    }

    /**
     * 获取可用的样本位置
     *
     * @param deviceSerialNumber
     * @return
     */
    @Override
    public List<Integer> getUsefulLocation(String deviceSerialNumber) {
        List<Integer> list = new ArrayList<>();

        LambdaQueryWrapper<SamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(SamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        SamplePoolConfigPO po = dao.selectOne(query);
        if (po == null || po.getPoolInfo() == null || po.getPoolInfo().isEmpty()) {
            return list;
        }

        List<FSamplePoolStatus> poolStatuses = po.getPoolInfo();
        for (FSamplePoolStatus status : poolStatuses) {
            if (status.getPoolStatus() && status.getPoolPurpose().equals(DeviceSamplePoolPurpose.MEASURE.getCode())) {
                list.add(status.getPoolNum());
            }
        }

        list.sort(Integer::compareTo);
        return list;
    }

    /**
     * 获取可用的样本位置(含校零池)
     *
     * @param deviceSerialNumber
     * @return
     */
    @Override
    public List<Integer> getAllUsefulLocation(String deviceSerialNumber) {
        List<Integer> list = new ArrayList<>();

        LambdaQueryWrapper<SamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(SamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        SamplePoolConfigPO po = dao.selectOne(query);
        if (po == null || po.getPoolInfo() == null || po.getPoolInfo().isEmpty()) {
            return list;
        }

        List<FSamplePoolStatus> poolStatuses = po.getPoolInfo();
        for (FSamplePoolStatus status : poolStatuses) {
            if (status.getPoolStatus()) {
                list.add(status.getPoolNum());
            }
        }

        list.sort(Integer::compareTo);
        return list;
    }

    @Override
    public R<SamplePoolInfoVO> queryCalibrationSampleInfo(String deviceSerialNumber) {
        LambdaQueryWrapper<SamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(SamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        SamplePoolConfigPO po = dao.selectOne(query);
        if (po == null || po.getPoolInfo() == null || po.getPoolInfo().isEmpty()) {
            //未找到该仪器附件配置
            return R.fail("00106001");
        }

        SamplePoolInfoVO vo = new SamplePoolInfoVO();
        BeanUtils.copyProperties(po, vo);
        String locale2 = localeProvider.getSystemLocale();
        boolean isChinese2 = "zh-CN".equals(locale2) || "zh_CN".equals(locale2);

        List<SamplePoolStatus> list = po.getPoolInfo().stream().map(p -> {
            SamplePoolStatus status = new SamplePoolStatus();
            status.setPoolNum(p.getPoolNum());
            status.setPoolStatus(p.getPoolStatus());
            status.setPoolName(isChinese2 ? status.getPoolNum() + "号" : "#" + status.getPoolNum());
            return status;
        }).collect(Collectors.toList());
        vo.setPoolStatusList(list);
        return R.ok(vo);

    }

    @Override
    public FSamplePoolStatus getPoolCorrectionData(String deviceSerialNumber, Integer poolNum) {
        if (!StringUtils.hasText(deviceSerialNumber) || poolNum == null) {
            log.warn("获取样品池校正数据参数无效：device={}, poolNum={}", deviceSerialNumber, poolNum);
            return null;
        }

        LambdaQueryWrapper<SamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(SamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        SamplePoolConfigPO po = dao.selectOne(query);

        if (po == null || po.getPoolInfo() == null || po.getPoolInfo().isEmpty()) {
            log.warn("未找到设备 {} 的样品池配置", deviceSerialNumber);
            return null;
        }

        for (FSamplePoolStatus status : po.getPoolInfo()) {
            if (poolNum.equals(status.getPoolNum())) {
                Map<String, String> poolData = status.getPoolData();
                return poolData != null ? status : null;
            }
        }

        log.warn("未找到样品池 {} 的配置信息", poolNum);
        return null;
    }

    // ... existing code ...
    @Override
    public Boolean clearAllPoolCorrectionData(String deviceSerialNumber) {
        if (!StringUtils.hasText(deviceSerialNumber)) {
            log.warn("清除校正数据参数无效：device={}", deviceSerialNumber);
            return false;
        }

        LambdaQueryWrapper<SamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(SamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        SamplePoolConfigPO po = dao.selectOne(query);

        if (po == null || po.getPoolInfo() == null || po.getPoolInfo().isEmpty()) {
            log.warn("未找到设备 {} 的样品池配置，无法清除校正数据", deviceSerialNumber);
            return false;
        }

        int clearedCount = 0;
        for (FSamplePoolStatus status : po.getPoolInfo()) {
            if (status.getPoolData() != null || status.getLuminosity() != null) {
                status.setPoolData(null);
                status.setLuminosity(null);
                clearedCount++;
            }
        }

        if (clearedCount == 0) {
            log.info("设备 {} 没有需要清除的校正数据", deviceSerialNumber);
            return true;
        }

        // 更新数据库
        boolean updateResult = dao.updateById(po) > 0;
        if (updateResult) {
            log.info("已清除设备 {} 共 {} 个样品池的校正数据", deviceSerialNumber, clearedCount);
        } else {
            log.error("清除设备 {} 的校正数据失败", deviceSerialNumber);
        }

        return updateResult;
    }

    @Override
    public String applyPoolCorrection(String measured, String correction, String currentLuminosity) {

        BigDecimal measuredValue = new BigDecimal(measured);
        BigDecimal correctionValue = new BigDecimal(correction);

        // 3. 校验光度模式
        if (!StringUtils.hasText(currentLuminosity)) {
            log.warn("光度模式为空，无法应用校正");
            return null;
        }

        boolean isAbs = WaveLengthEnum.abs.getCode().equals(currentLuminosity);

        BigDecimal correctedValue;
        if (isAbs) {
            // Abs模式：校正时用 样品值 - 校零值，还原时用 测量值 - 校正值
            correctedValue = measuredValue.subtract(correctionValue);
            log.debug("Abs模式校正：测量值={}, 校正值={}, 校正后={}", measuredValue, correctionValue, correctedValue);
        } else {
            // T%模式：校正时用 样品值 / 校零值，还原时用 测量值 / 校正值
            if (correctionValue.compareTo(BigDecimal.ZERO) == 0) {
                log.warn("校正值为0，无法进行T%除法运算");
                return null;
            }
            correctedValue = measuredValue.divide(correctionValue, 10, RoundingMode.HALF_UP);
            log.debug("T%模式校正：测量值={}, 校正值={}, 校正后={}", measuredValue, correctionValue, correctedValue);
        }

        return correctedValue.toPlainString();
    }

    @Override
    public R<Boolean> updatesphereMode(FSamplePoolInfoChangeDto changeDto) {
        Boolean res = false;
        if (!StringUtils.hasText(changeDto.getDeviceSerialNumber())) {
            log.info("未连接仪器");
            //未连接仪器
            return R.fail("0000001D");
        }

        if (!StringUtils.hasText(changeDto.getUpdateBy())) {
            log.info("缺少更新人信息");
            //请登录后操作！
            return R.fail("00307004");
        }
        LambdaQueryWrapper<SamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(SamplePoolConfigPO::getDeviceSerialNumber, changeDto.getDeviceSerialNumber());
        SamplePoolConfigPO po = dao.selectOne(query);
        if (po == null) {
            //未找到该仪器附件配置
            return R.fail("00106001");
        }
        //借用池号 poolNum 设置使用场景
        po.setCurrentPoolNum(changeDto.getPoolNum());
        //更新config表
        po.getPoolInfo().stream()
                .filter(item->changeDto.getPoolNum().equals(item.getPoolNum()))
                .findFirst()
                .ifPresent(item->item.setPoolStatus(true));
        boolean b = dao.updateById(po) > 0;
        if (!b) {
            //更新失败
            return R.fail("00000048");
        }
        return R.ok();
        //后面调用暗电流系数和计算反转的时候调用这个表的模式
    }

}
