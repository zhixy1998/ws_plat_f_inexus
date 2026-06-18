/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.concise.measurementService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.dto.measurementDto.AddSampleDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRoundingVO;
import com.persee.workstation.uv_inexus.vo.concise.common.DeviceMeasureVO;
import com.persee.workstation.uv_inexus.vo.concise.common.FMethodListWithPageVO;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FSaveDataVO;

/**
 * 简洁模式: 通用服务
 */
public interface ConciseCommonService {

    // 获取默认参数
    Result queryDefaultParameters(String measureType);

    // 查找方法按钮
    Result<FMethodListWithPageVO> searchMethodButton(FQueryMethodWithPageDto dto);
    // 查询保存状态
    Result querySaveState(String taskId);

    // 保存草稿按钮
    Result saveDraftButton(String taskId);

    Result<Page<FMeasureMethodParam>> queryHistoryMethod(QueryMethodDto queryMethodDto);

    // 查询方法参数
    Result queryMethodParameter(String methodId);

    Result<Page<FMeasureTaskDTO>> queryHistoryTask(SelectTaskDto selectTaskDto);

    Result<String> updateMethod(UpdateMethodDto updateMethodDto);

    // 修改任务名称
    Result updateTask(UpdateTaskDto updateTaskDto);

    /**
     * 光度和定量方法里输入参数校验
     */
    Result<Boolean> checkSampleInputParam(FCheckSampleInputParamDto checkSampleInputParamDto);
    /*
    光度和定量方法里公式校验
     */
    Result<Boolean> checkFormula(FCheckFormulaDto checkFormulaDto);

    // 获取报告参数
    Result obtainReportParameters(String taskId,String analysisId);


    // 获取报告所有参数
    Result queryReportAllParameters(String taskId, String sampleId, String serialNumber);

    // 新的报表数据查询
    Result newReportTableDataQuery(String taskId,String analysisId, Boolean allDisplay,Integer digit);

    // 是否添加样品
    Result isAddSample(AddSampleDTO addSampleDTO);

    Result<DeviceMeasureVO> queryDeviceTypeInfo(String deviceSerialNumber);

    //删除方法
    Result<String> delMethod(String methodId);
    //另存为方法
    Result<String> copy(CopyParam copyParam);

    /**
     * 前端调用保存数据
     */
    Result saveDataInfo(FSaveDataVO vo);

    /**
     * 检查修约范围是否合格
     */
    Result<Boolean> checkApproximateRange(FRoundingVO vo);

    // 设置参数接口
    Result setUpParametersPort(String taskId, String deviceSerialNumber, String measureType);


    // 查询多任务样品接口
    Result queryMultipleTaskSample(MultipTaskSample multipTaskSample);
}
