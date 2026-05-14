/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.instrumentDto;

import com.persee.workstation.uv_inexus.vo.instrumentVo.DataNameValue;
import com.persee.workstation.uv_inexus.vo.instrumentVo.CalibrationTableData;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 仪器校准传参
 */
@Data
public class DeviceCalibrationDTO {

    @ApiModelProperty("校准参数")
    private List<DataNameValue> data;

    @ApiModelProperty("表数据")
    private List<CalibrationTableData> calibrationTableData;

    @ApiModelProperty("表格里所有数据")
    private List<CalibrationTableData> allTableData;

    @ApiModelProperty("校准类型")
    private Integer calibrationType;

    @ApiModelProperty("校准名称")
    private String calibrationName;

    @ApiModelProperty("仪器序列号")
    private String deviceSerialNumber;

    @ApiModelProperty("操作类型")
    private String operateType;

    @ApiModelProperty("操作人")
    private String updateBy;
}
