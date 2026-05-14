/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.SamplePoolConfigDAO;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceInfoDAO;
import com.persee.workstation.uv_inexus.dto.concise.common.FSampleAttachTypeSaveDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.FSamplePoolInfoChangeDto;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceSamplePoolPurpose;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.SamplePoolConfigPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ConcisePublicUtilService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SamplePoolConfigService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.DevOperationInputParam;
import com.persee.workstation.uv_inexus.util.deviceUtil.DevOperationUtilService;
import com.persee.workstation.uv_inexus.util.deviceUtil.OperationResultEntity;
import com.persee.workstation.uv_inexus.util.deviceUtil.OperationTypeEnums;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.vo.concise.common.FSamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.factory_inspection.SamplePoolInfoVO;
import com.persee.workstation.uv_inexus.vo.factory_inspection.SamplePoolStatus;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

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
    DevOperationUtilService operationUtilService;

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
            po.setPoolInfo(JSON.toJSONString(poolInfo));
            return dao.insert(po) > 0;
        } else {
            po.setCurrentPoolNum(currentPoolNum);
            po.setUpdateBy(updateBy);
            return dao.updateById(po) > 0;
        }
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
        List<FSamplePoolStatus> list = createFSamplePoolStatuses(attachmentInfo.getSamplePoolNum());
        po.setPoolInfo(JSON.toJSONString(list));
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
                if (StringUtils.hasText(po.getPoolInfo())) {
                    List<FSamplePoolStatus> list = JSON.parseArray(po.getPoolInfo(), FSamplePoolStatus.class);
                    if (list != null && !list.isEmpty()) {
                        for (FSamplePoolStatus status : list) {
                            status.setPoolStatus(true);
                        }
                        po.setPoolInfo(JSON.toJSONString(list));
                    }
                }
            } else {
                if (StringUtils.hasText(po.getPoolInfo())) {
                    List<FSamplePoolStatus> list = JSON.parseArray(po.getPoolInfo(), FSamplePoolStatus.class);
                    if (list != null && !list.isEmpty()) {
                        for (FSamplePoolStatus status : list) {
                            status.setPoolStatus(false);
                        }
                        po.setPoolInfo(JSON.toJSONString(list));
                    }
                }
            }
        }

        po.setUpdateBy(changeDto.getUpdateBy());
        if (changeDto.getIsAutoChangePool() != null) {
            if (StringUtils.hasText(po.getPoolInfo())) {
                List<FSamplePoolStatus> list = JSON.parseArray(po.getPoolInfo(), FSamplePoolStatus.class);
                if (list == null || list.size() <= 1) {
                    //没有切换的样品池
                    return R.fail("00106002");
                }
            }

            po.setIsAutoChangePool(changeDto.getIsAutoChangePool());
        }

        if (changeDto.getPoolNum() != null) {
            List<FSamplePoolStatus> list = JSON.parseArray(po.getPoolInfo(), FSamplePoolStatus.class);
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
            po.setPoolInfo(JSON.toJSONString(list));
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
            if (changeDto.getUpdateDatabase().equals("concise")){
                concisePublicUtilService.changeSampleLocation(changeDto.getTaskId(), usefulLocation);
            } if (changeDto.getUpdateDatabase().equals("guide")){
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
        if (po == null || !StringUtils.hasText(po.getPoolInfo())) {
            //未找到该仪器附件配置
            return R.fail("00106001");
        }

        FSamplePoolInfoVO vo = new FSamplePoolInfoVO();
        BeanUtils.copyProperties(po, vo);
        List<FSamplePoolStatus> list = JSON.parseArray(po.getPoolInfo(), FSamplePoolStatus.class);
        list.forEach(status -> status.setPoolName(status.getPoolNum() + "号"));
        vo.setPoolStatusList(list);
        return R.ok(vo);
    }

    /**
     * 获取可用的样本位置
     * @param deviceSerialNumber
     * @return
     */
    @Override
    public List<Integer> getUsefulLocation(String deviceSerialNumber) {
        List<Integer> list = new ArrayList<>();

        LambdaQueryWrapper<SamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(SamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        SamplePoolConfigPO po = dao.selectOne(query);
        if (po == null || !StringUtils.hasText(po.getPoolInfo())) {
            return list;
        }

        List<FSamplePoolStatus> poolStatuses = JSON.parseArray(po.getPoolInfo(), FSamplePoolStatus.class);
        for (FSamplePoolStatus status : poolStatuses) {
            if (status.getPoolStatus() && status.getPoolPurpose().equals(DeviceSamplePoolPurpose.MEASURE.getCode())) {
                list.add(status.getPoolNum());
            }
        }

        list.sort(Integer::compareTo);
        return list;
    }

    @Override
    public R<Boolean> setCurrentPool(String deviceSerialNumber, Integer poolNum, String updateBy) {
        if (!StringUtils.hasText(deviceSerialNumber)) {
            log.info("未连接仪器");
            //未连接仪器
            return R.fail("0000001D");
        }
        if (!StringUtils.hasText(updateBy)) {
            log.info("缺少更新人信息");
            //请登录后操作
            return R.fail("00307004");
        }
        LambdaQueryWrapper<SamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(SamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        SamplePoolConfigPO po = dao.selectOne(query);
        if (po == null) {
            po = new SamplePoolConfigPO();
            po.setCreateBy(updateBy);
            DeviceAttachmentTypeVO attachmentInfo = deviceInfoDAO.getAttachmentInfo(deviceSerialNumber);
            po.setAttachmentType(attachmentInfo.getAttachmentNum());
            po.setDeviceSerialNumber(deviceSerialNumber);
            List<FSamplePoolStatus> poolInfo = createFSamplePoolStatuses(attachmentInfo.getSamplePoolNum());
            po.setPoolInfo(JSON.toJSONString(poolInfo));
        }

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.SetParam.getType());

        SetParam param = new SetParam();
        param.setCellPos(poolNum);
        inputParam.setParam(param);
        R<OperationResultEntity> operation = operationUtilService.operation(inputParam);

        if (!operation.getSuccess()) {
            return R.fail("参数设置出错！");
        }

        po.setCurrentPoolNum(poolNum);
        po.setUpdateBy(updateBy);
        boolean b = dao.updateById(po) > 0;
        if (b) {
            return R.ok(true);
        } else {
            //更新失败
            return R.fail("00000048");
        }
    }

    @Override
    public R<SamplePoolInfoVO> queryCalibrationSampleInfo(String deviceSerialNumber) {
        LambdaQueryWrapper<SamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(SamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        SamplePoolConfigPO po = dao.selectOne(query);
        if (po == null || !StringUtils.hasText(po.getPoolInfo())) {
            //未找到该仪器附件配置
            return R.fail("00106001");
        }

        SamplePoolInfoVO vo = new SamplePoolInfoVO();
        BeanUtils.copyProperties(po, vo);
        List<SamplePoolStatus> list = JSON.parseArray(po.getPoolInfo(), SamplePoolStatus.class);
        list.forEach(status -> status.setPoolName(status.getPoolNum() + "号"));
        vo.setPoolStatusList(list);
        return R.ok(vo);

    }


}
