/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.dto.measurementDto.experimentTest.TimeSweepMethodParamsDto;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.PeakValleyMarking;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.MethodChangeCheckVO;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;
import java.util.Map;

public interface TimeSweepService {
    // 查询项目任务方法接口
    Result queryProjectTaskMethod(String updateBy, String measureType);

    // 查询项目下的所有方法
    Result queryProjectAllMethods(String projectId, String measureForm ,String updateBy);

    // 检查参数接口
    Result<MethodChangeCheckVO> checkMethodParam(TimeSweepMethodParamsDto methodParam);

    // 查询方法参数接口
    Result queryMethodParameter(String methodId);

    // 修改方法参数接口
    Result updateMethodParameter(TimeSweepMethodParameterDto params);

    // 获取样本列表接口
    Result sampleListPort(String taskId, String type,Integer  digit,String deviceSerialNumber,
                          Boolean showPeak, Boolean showValley, Boolean showMarkPoint);

    // 删除样品列表接口
    Result deleteSampleListPort(String sampleId, String taskId, String analysisId);

    // 更新样品选中状态(已测:传analysisId;未测:传taskId+sampleId;全选/全取消:传taskId+allSelectType,1-所有样品 2-所有分析样品)
    Result updateSampleSelected(String analysisId, String isSelected, String taskId, String allSelectType, String sampleId);

    // 获取变换列表接口
    Result changeTheListPort(String taskId, String type,Integer  digit);

    // 删除变换列表接口
    Result deleteChangeListPort(String analysisId);

    // 恢复已删除数据接口
    Result restoreDeletedListData(String taskId,String sampleId, String type,String analysisId);

    // 设置方法参数接口
    Result setMethodParameters(String methodId, String deviceSerialNumber);

    // 校零接口
    Result zero(String taskId,String serialNumber, String methodId);

    // 获取样本按钮列表接口
    Result querySampleList(String taskId);

    // 获取样本输入表头接口
    Result<List<HeadVO>> querySampleHeads(String taskId);

    // 添加或删除样本接口
    Result addOrDeleteSamplePort(AddOrDeleteSampleDTO paramDto);

    // 图片上传
    Result uploadImage(SpectrumImageUpload upload);

    // 查询峰谷数据列表接口
    Result queryPeakValleyDataList(String dataId,  Integer digit);

    // 寻峰寻谷接口
    Result seekingPeaksAndValleysPort(SeekingPeaksValleysDto paramDto);

    // 添加峰谷备注接口
    Result addPeakValleyRemarks(PeakValleyMarkingDto paramDto);

    // 获取标记点数据列表
    Result queryGaugePointDataList(String dataId, Integer digit);

    // 添加标记点接口
    Result addGaugePoint(AddGaugePointDto paramDto);

    // 添加标记点备注接口
    Result addGaugePointRemarks(PeakValleyMarkingDto paramDto);

    // 更新数据颜色接口
    Result updateDataColor(UpdateDataColorDto paramDto);

    // 获取报表内容数据
    Result gainStatementData(String taskId);

    // 导出Word接口
    void exportToWord(String taskId, HttpServletResponse response) throws Exception;

    // 查询运算源数据接口
    Result queryArithmeticSourceData(String taskId);

    // 四则运算接口
    Result fourOperationsPort(FourOperationsParamDto paramDto);

    // 高级运算接口
    Result advancedOperationsPort(AdvancedOperationsDto paramDto);

    // 平滑接口
    Result smoothnessPort(SmoothnessDto paramDto);

    // 微分接口
    Result differentialPort(DifferentialDto paramDto);

    // 光度模式转换
    Result luminanceModeConversionPort(LuminosityConversionDto paramDto);

    // 保存数据接口
    Result saveData(String taskId);

    // 多格式文件下载接口（支持word/excel/txt）
    R<File> downloadFile(String taskId, Boolean allDisplay, String fileType, List<String> analysisIds,String deviceSerialNumber, Integer  digit);

    // 仪器设置参数
    boolean setInstrumentParameters(String deviceSerialNumber, SetParam setParam);

    // (实验测试)创建方法
    Result createMethodPort(MethodParamRequestDto methodDto, String updateBy, TimeSweepParamter timeSweepParamter);

    // (实验测试)查询报告表格数据接口
    List<PeakValleyMarking>queryReportFormData(String taskId, String sampleId);

    // (实验测试)表格头接口
    Result<List<HeadVO>> tableHead(String taskId, String methodId, List<String> analysisIdList, Boolean isOneSample);

    // (实验测试)查询表格数据接口
    List<Map<String, Object>> queryTableData(String taskId, List<String> analysisIdList,Integer decimalDigits, Boolean isOneSample);

    // 停止测量接口
    Result stopMeasure(String deviceSerialNumber);


}