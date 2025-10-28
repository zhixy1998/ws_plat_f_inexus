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

package com.persee.workstation.uv_inexus.service.guide.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.CreateTaskParamDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.ProjectParamDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.QueryTaskDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.TaskBaseVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExperimentTestService {
    Result newProject(ProjectParamDto paramDto);

    Result recentlyAccomplishTask(String updateBy,String measureType);

    Result queryProjectStatistics(String updateBy);

    Result createMethodPort(MethodParamRequestDto methodDto);

    Result createTask(CreateTaskParamDto paramDto);

    Result queryTaskPort(QueryTaskDto queryTask);

    Result continueMeasuringPort(String projectId, String methodId);

    Result queryAllProject();

    Result queryMethodProt(String projectId, String measureType, Integer pageNum, Integer pageSize);

    Result assignMethodPort(String methodType, String projectId);

    Result queryReferenceStandard(String standardName);

    Result preprocessTheData(String standardId);



    Result reportDataQuery(String taskId, String reportName,String sampleId);

    // 新的报表数据查询
    Result newReportTableDataQuery(String taskId, List<String> sampleIds);

    Result<MeasurementCurrentInfoVO> getProjectInfo(String projectId, String measureType);

    Result<List<TaskBaseVO>> getAllTask(String projectId, String measureType);

    // 使用此方法接口
    Result useThisMethodPort(String methodId);

    Result<MeasurementCurrentInfoVO> useMethodToTest(String methodId, String updateBy, String deviceSerialNumber);

    // 获取方法列表
    Result listOfMethods(String methodName, Integer pageNum, Integer pageSize,String sort);

    // 最近创建的任务
    Result recentlyCreatedTasks(String updateBy, String measureType);

    // 获取报告参数
    Result obtainReportParameters(String taskId,String sampleId,String serialNumber);


    // 获取报告所有参数
    Result queryReportAllParameters(String taskId, String sampleId, String serialNumber);

    // 导出方法
    List<TMeasureMethod> exportMethod(List<String> ids);

    // 导入方法
    Result importMethod(MultipartFile file);

    // 清除项目
    Result clearProject(String projectId);

    // 清除任务
    Result clearTask(String taskId);

    // 清除方法
    Result clearMethod(String methodId);

}
