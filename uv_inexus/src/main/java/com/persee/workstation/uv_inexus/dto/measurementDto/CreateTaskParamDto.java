/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.dto.measurementDto;

import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 创建任务参数数据传输对象(DTO)
 * 用于封装创建任务所需的参数信息
 */
@Data
public class CreateTaskParamDto {
    private String projectId;    // 项目ID，标识任务所属的项目
    private String taskName;    // 任务名称，用于标识任务的名称
    private String methodId;    // 方法ID，标识任务所采用的方法
    private String measurementType;  // 测量类型，标识任务的测量类型
    private String deviceSerialNumber;  // 设备序列号，标识执行任务的设备
//    private List<SpectrumSpecimenDto> specimens;  // 光谱样本数据列表，已注释掉

    private List<Map<String, String>> specimens;  // 样本数据列表，使用Map存储键值对形式的样本数据

    private List<HeadVO> heads;  // 表头信息列表，用于展示表格的表头

    private List<HeadVO> rationStandHeads;    // 标准品比例表头列表
    private List<HeadVO> rationUnknowHeads;   // 未知品比例表头列表
    private List<Map<String, String>> rationStandData;    // 标准品比例数据列表，使用Map存储键值对形式的数据
    private List<Map<String, String>> rationUnknowData;   // 未知品比例数据列表，使用Map存储键值对形式的数据
}
