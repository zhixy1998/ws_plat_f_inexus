/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.concise.common;

import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FAddOrDeleteSampleDTO {
    @ApiModelProperty("操作类型：add/edit/delete")
    private String operateType;

    @ApiModelProperty("任务id")
    private String taskId;

    @ApiModelProperty("方法id")
    private String methodId;

    @ApiModelProperty("更新人")
    private String updateBy;

    @ApiModelProperty("新增的样本")
    private List<Map<String, String>> newSamples;

    @ApiModelProperty("新增的样本表头")
    private List<FHeadVO> heads;

    @ApiModelProperty("删除的样本id")
    private List<String> delSampleIds;

    @ApiModelProperty("设备序列号")
    private String deviceSerialNumber;
}
