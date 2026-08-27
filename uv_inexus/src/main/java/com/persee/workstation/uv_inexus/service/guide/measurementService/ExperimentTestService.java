/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.measurementService;

import com.persee.workstation.uv_inexus.dto.guide.UseMethodDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.vo.measurementVo.MethodNameAndIdVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MeasurementCurrentInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.SaveDataVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.TaskBaseVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExperimentTestService {
    Result newProject(ProjectParamDto paramDto);

    Result recentlyAccomplishTask(String updateBy,String measureType);

    Result queryProjectStatistics(String updateBy);

    Result createMethodPort(MethodParamRequestDto methodDto);

    Result createTask(CreateTaskParamDto paramDto);

//    // 是否添加样本
//    Result isAddSample(String taskId, String deviceSerialNumber,
//                       SampleNumConfig numberConfig, SampleNameConfig nameConfig);

    Result isAddSample(AddSampleDTO addSampleDTO);

    // 通用: 设置参数接口
    Result setUpParametersPort(String taskId, String deviceSerialNumber, String measureType);

    Result queryTaskPort(QueryTaskDto queryTask);

    Result continueMeasuringPort(String projectId, String methodId);

    Result queryAllProject(String updateBy);

    Result queryMethodProt(String projectId, String measureType, String updateBy, Integer pageNum, Integer pageSize);

    Result assignMethodPort(String methodType, String projectId,String updateBy);

    Result queryReferenceStandard(String standardName);

    Result preprocessTheData(String standardId);



    Result reportDataQuery(String taskId, String reportName,String sampleId);

    // 新的报表数据查询
//    Result newReportTableDataQuery(String taskId, List<String> sampleIds);
    Result newReportTableDataQuery(String taskId,String analysisId, Boolean allDisplay,Integer digit,String showType);

    Result<MeasurementCurrentInfoVO> getProjectInfo(String projectId, String measureType);

    Result<List<TaskBaseVO>> getAllTask(String projectId, String measureType);

    // 使用此方法接口
    Result useThisMethodPort(CreateTaskParamDto paramDto);

    Result<MeasurementCurrentInfoVO> useMethodToTest(String methodId, String updateBy, String deviceSerialNumber);

    // 获取方法列表
    Result listOfMethods(String methodName, Integer pageNum, Integer pageSize,String sort,String updateBy,Boolean isMethodManage);

    // 最近创建的任务
    Result recentlyCreatedTasks(String updateBy, String measureType);

    // 获取报告参数
//    Result obtainReportParameters(String taskId,String sampleId,String serialNumber);
    Result obtainReportParameters(String taskId,String analysisId);

    // 获取报告所有参数
    Result queryReportAllParameters(String taskId, String sampleId, String serialNumber);

    // 导出方法
    List<TMeasureMethod> exportMethod(List<String> ids);
    
    // 导出方法到Excel
    byte[] exportMethodToExcel(List<TMeasureMethod> methods);

    // 导入方法
    Result importMethod(MultipartFile file);
    
    // 从Excel导入方法
    Result importMethodFromExcel(MultipartFile file);

    // 清除项目
    Result clearProject(String projectId);

    // 清除任务
    Result clearTask(String taskId);

    // 清除方法
    Result clearMethod(String methodId);

    // 查询保存状态
    Result querySaveState(String taskId);

    // 方法名称重复性检查
    Result methodDuplicationCheck(String projectId, String methodName, String measureType);

    // 任务名称重复性检查
    Result taskDuplicationCheck(String projectId, String taskName, String measureType);

    /**
     * 使用新方法
     */
    Result<MeasurementCurrentInfoVO> useMethod(UseMethodDTO dto);

    Result<String> getTaskStatus(String taskId);

    /**
     * 前端调用保存数据
     */
    Result saveDataInfo(SaveDataVO vo);

    // 查询样本默认表头
    Result querySampleDefaultHead(String measureType);

    /**
     * 获取当前信息
     */
    Result<MeasurementCurrentInfoVO> getCurrentInfo(String updateBy, String measureType);

    /**
     * 任务终止
     */
    Result taskTermination(String taskId);

    Result<List<MethodNameAndIdVO>> queryMethodNameAndId(String measureType, String projectId, String standardId);


}
