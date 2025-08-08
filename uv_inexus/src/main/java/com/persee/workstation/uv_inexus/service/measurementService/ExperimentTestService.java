package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.CreateTaskParamDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.ProjectParamDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.QueryTaskDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.TaskBaseVO;

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



    Result reportDataQuery(String taskId, String reportName);

    Result<MeasurementCurrentInfoVO> getProjectInfo(String projectId, String measureType);

    Result<List<TaskBaseVO>> getAllTask(String projectId, String measureType);

    // 使用此方法接口
    Result useThisMethodPort(String methodId);

    Result<MeasurementCurrentInfoVO> useMethodToTest(String methodId, String updateBy, String deviceSerialNumber);

    // 获取方法列表
    Result listOfMethods(String methodName, Integer pageNum, Integer pageSize,String sort);

    // 最近创建的任务
    Result recentlyCreatedTasks(String updateBy, String measureType);
}
