/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dao.guide.instrumentDao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import org.apache.ibatis.annotations.Param;

@DS("guide")
public interface DeviceInfoDAO extends BaseMapper<DeviceInfoPO> {

    String getIp(@Param("device_serial_number") String deviceSerialNumber);

    String getAttachmentType(@Param("device_serial_number") String deviceSerialNumber);
    DeviceAttachmentTypeVO getAttachmentInfo(@Param("device_serial_number") String deviceSerialNumber);


    DeviceAttachmentTypeVO getAttachmentInfoByAttachmentNum(@Param("attachment_num") Integer attachmentNum);

    //修改计算公式
    int updateFormula(@Param("deviceSerialNumber") String deviceSerialNumber, @Param("linearSegment") String linearSegment);
    //根据序列号获取计算公式
    String getFormula(@Param("deviceSerialNumber") String deviceSerialNumber);

    Integer queryDeviceActivateStatus(@Param("deviceSerialNumber") String deviceSerialNumber);

    void updateDeviceActivateStatus(@Param("deviceSerialNumber") String deviceSerialNumber, @Param("activateStatus") Integer activateStatus);
}
