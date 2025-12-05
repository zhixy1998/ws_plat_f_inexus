/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.instrumentService;

import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;

public interface DeviceInfoService {

    DeviceInfoPO getInfo(String deviceSerialNumber);

    Boolean saveOrUpdateInfo(DeviceInfoPO po);

    String getIp(String deviceSerialNumber);

    //查找附件类型
    String getAttachmentType(String deviceSerialNumber);

    void updateDeviceCurrentStatus(String deviceSerialNumber, Integer currentStatus);

    Boolean changeDeviceName(String deviceSerialNumber, String newDeviceName, String updateBy);

    //占用仪器
    R<Boolean> useDevice(String deviceSerialNumber, Integer status);

    //释放仪器
    void releaseDevice(String deviceSerialNumber);

    //激活仪器
    void activeDevice(String deviceSerialNumber);

    //查询仪器激活状态
    Integer queryDeviceActivationStatus(String deviceSerialNumber);
    
    Boolean isNeedSetSit(String deviceSerialNumber);

    void checkDeviceStatus();
}
