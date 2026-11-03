/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.instrumentService;


import com.persee.workstation.uv_inexus.dto.factory_inspection.DeviceTaskDto;
import com.persee.workstation.uv_inexus.dto.instrumentDto.*;
import com.persee.workstation.uv_inexus.exception.BusinessException;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.WorkTimeInfo;
import com.persee.workstation.uv_inexus.util.deviceUtil.DevOperationInputParam;
import com.persee.workstation.uv_inexus.util.deviceUtil.OperationResultEntity;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SearchInfo;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceCalibrationVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceConfigVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceInfoVO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.FindDeviceVO;

import java.util.List;

public interface DeviceManageService {

    R<DeviceInfoVO> queryDeviceInfo(String deviceSerialNumber, String updateBy);

    R<Integer> queryDeviceStatus(String deviceSerialNumber);

    R<Boolean> init(DeviceInitDTO initDTO);

    R<Boolean> stopInit(String deviceSerialNumber, String updateBy);

    Boolean startDevice(DeviceStartDTO startDTO);

    Boolean softClose(DeviceSoftCloseDTO closeDTO);

    /**
     * 开关灯
     * @param lampType 灯类型 0-钨灯，1-氘灯
     * @param status 状态：1-开，0-关
     */
    R<Boolean> closeOrOpenLamp(String lampType, Integer status, String deviceSerialNumber);

    /**
     * 切换灯
     * @param currentLampType 当前灯类型， 0-钨灯，1-氘灯
     */
    R<Boolean> changeLamp(String currentLampType, String deviceSerialNumber);

    R<List<FindDeviceVO>> findDevice() throws Exception;

    /**
     * 仪器激活接口
     * 参数暂时未知 @param
     * @return
     */
    Boolean deviceActivation(String name);


    R<DeviceConfigVO> newQueryDeviceConfig();

    R<Boolean> newDevConfig(DevConfigDTO dto);

    R<Boolean> setDevMessagePort(Integer messagePort, String updateBy);

    /**
     * 查询光谱带宽接口
     * @return
     */
    List<Double> getBandWidth(String deviceSerialNumber);

    /**
     * 自动进样器配置
     */
//    R<Boolean> autoInjectorConfig();

    /**
     * 定时关机设置
     * @param closeTime
     * @return
     */
    Boolean timedStart(String deviceSerialNumber, String closeTime);

    /**
     * 仪器校准接口
     * @param calibrationDTO
     * @return
     */
    R<Boolean> deviceCalibration(DeviceCalibrationDTO calibrationDTO);


    /**
     * 查询仪器校准方法接口
     * @param deviceSerialNumber
     * @return
     */
    List<DeviceCalibrationVO> newQueryCalibrationInfo(String deviceSerialNumber);
    List<DeviceCalibrationVO> queryMultiCalibration(String deviceSerialNumber);

    /**
     * 检查仪器状态
     */
    R<Boolean> checkDeviceStatus(String deviceSerialNumber);

    /**
     * 更新仪器当前状态
     */
    R<Boolean> updateDeviceStatus(String deviceSerialNumber, String status);

    R<DeviceMeasureVO> queryDeviceTypeInfo(String deviceSerialNumber);

    // 获取默认参数映射
    Result<DeviceMeasureVO> queryDeviceTypeInfoWithDefault(String deviceSerialNumber);


    void test();

    R<List<SearchInfo>> searchDevice(Integer connectType, String updateBy);
    R<List<SearchInfo>> searchDevice();

    R<FindDeviceVO> connect(SearchInfo param);

    R<Boolean> stop(
            String deviceSerialNumber
    );

    R<OperationResultEntity> setParamOneDevice(DevOperationInputParam param);

    R<OperationResultEntity> getParamOneDevice(DevOperationInputParam param);

    R<OperationResultEntity> Action(DevOperationInputParam param);

    R<Boolean> disconnectDevice(String deviceSerialNumber, String updateBy);

    R<Boolean> startInitDevice(String deviceSerialNumber, String updateBy);

    R<Boolean> continueInitDevice(String deviceSerialNumber, String updateBy);

    R<String> getConnectDevice(String updateBy);

    /**
     * 查询该用户下所有已连接设备（只查询，不断开任何设备）
     */
    R<List<DeviceInfoPO>> getConnectedDeviceList(String updateBy);

    /**
     * 校验指定序列号的设备是否已连接（多设备安全，不会断开其他设备）
     */
    R<Boolean> isDeviceConnected(String deviceSerialNumber, String updateBy);

    R<Boolean> newStopInit(String deviceSerialNumber, String updateBy);

    R<Boolean> deviceNewCalibration(DeviceCalibrationDTO calibrationDTO);

    R<Boolean> deleteMultiCalibrationInfo(DeviceCalibrationDTO dto);

    R<Boolean> saveAllData(DeviceCalibrationDTO dto);

    R<Boolean> sendMultiCalibrationToDevice(DeviceCalibrationDTO dto);

    R<Boolean> multiCalibration(DeviceCalibrationDTO dto, String multiType);

    R<Boolean> reFindPeak(String deviceSerialNumber, String updateBy);

    R<List<WorkTimeInfo>> queryLampLifeInfo(String deviceSerialNumber, Boolean isUpdate);

    R<Boolean> hasWorkTime(String deviceSerialNumber);

    R<OperationResultEntity> TimeMeasure(DevOperationInputParam param);

    R<Boolean> zero(String deviceSerialNumber, List<String> param);

    R<Boolean> addDevice(SearchInfo info);

    R<FindDeviceVO> connect(List<SearchInfo> param,String updateBy,String templateCode);

    Result<DeviceTaskDto> createTaskAfterInstrumentInit(String deviceSerialNumber, String updateBy, String templateCode) throws BusinessException;

    R<Boolean> setCurrentPool(String deviceSerialNumber, Integer poolNum, String updateBy);

    //样品池校正
    R<Boolean> samplePoolCalibration(SamplePoolCalibrationDTO dto);

    R<Boolean> checkMercuryLamp(String deviceSerialNumber);

    //开关汞灯
    R<Boolean> switchHgLamp(String deviceSerialNumber, Integer status);


}
