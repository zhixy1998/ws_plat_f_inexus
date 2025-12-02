/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.parseReport;

import java.util.List;
import java.util.Map;

import com.persee.workstation.uv_inexus.po.guide.parseReport.Detection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("检测详情DTO")
@Data
public class DetectionDetailDTO {
    @ApiModelProperty("主数据信息")
    private DetectionListDTO mainData;

    @ApiModelProperty("原始数据列表")
    private List<Detection> originalDataList;

    @ApiModelProperty("标准标识、检测类型及id数组")
    private List<Map<String, Object>> identityTypeList; // 包含id
}