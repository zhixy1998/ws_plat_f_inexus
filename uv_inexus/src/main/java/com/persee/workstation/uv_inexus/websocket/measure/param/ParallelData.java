/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket.measure.param;

import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.RationSaveCountDataInfo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ParallelData {

    @ApiModelProperty("平行组ID")
    private String parallelId = null;

    @ApiModelProperty("平行组值")
    private String parallelValue = null;

    @ApiModelProperty("平行组数据列表")
    List<String> parallelValueList = new ArrayList<>();

    @ApiModelProperty("处理数据列表")
    List<RationSaveCountDataInfo> dealDataList = new ArrayList<>();



}
