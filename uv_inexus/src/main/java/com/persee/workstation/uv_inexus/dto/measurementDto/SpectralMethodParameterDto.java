/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.measurementDto;


import com.persee.workstation.uv_inexus.vo.measurementVo.spectrum.SpectrumParameterVo;
import lombok.Data;

@Data
public class SpectralMethodParameterDto extends SpectrumParameterVo {
    // 项目ID
    private String projectId;
    // 测量类型
    private String measureType;
    // 方法ID
    private String methodId;
    // 方法id2
    private String methodId2;
    // 方法名称
    private String methodName;
    // 任务id
    private String taskId;
    // 用户名
    private String updateBy;
    // 是否共享
    private String isShare;
    // 序列号
    private String deviceSerialNumber;
}