/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.concise.rationDto;

import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Data
public class FRationAddOrDeleteSampleDTO {
    @ApiModelProperty("操作类型：add/edit/delete")
    private String operateType;

    @ApiModelProperty("任务id")
    private String taskId;

    @ApiModelProperty("方法id")
    private String methodId;

    @ApiModelProperty("更新人")
    private String updateBy;

    @Autowired
    private String deviceSerialNumber;

//    @ApiModelProperty("新增的样本")
//    private List<SampleVO> newSamples;

    @ApiModelProperty("标准样品")
    private List<Map<String, String>> newStandSamples;

    @ApiModelProperty("新增的未知样本")
    private List<Map<String, String>> newUnknowSamples;

    @ApiModelProperty("新增的标准样本表头")
    private List<FHeadVO> standHeads;

    @ApiModelProperty("新增的未知样本表头")
    private List<FHeadVO> unknowHeads;

    @ApiModelProperty("删除的样本id")
    private List<String> delSampleIds;
}
