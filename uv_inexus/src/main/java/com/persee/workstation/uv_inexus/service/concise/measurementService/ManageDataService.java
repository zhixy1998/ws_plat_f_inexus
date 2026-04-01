package com.persee.workstation.uv_inexus.service.concise.measurementService;

import com.persee.workstation.uv_inexus.dto.concise.common.Box;
import com.persee.workstation.uv_inexus.dto.concise.common.FMethodParamRequestDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TaskQueryParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
public interface ManageDataService  {
    Result listOfMethods(String methodName, Integer pageNum, Integer pageSize, String sort, String updateBy);

    /**
     * 获取任务列表
     * @param taskQueryParam
     * @return
     */
    Result<Map<String, Object>> getTasksManagementList(TaskQueryParam taskQueryParam);

    Result<String> deleteTask(Box deleteTaskIdList);
    
    /**
     * 导出方法
     * @param ids 方法 ID 列表
     * @return 方法列表
     */
    java.util.List<com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod> exportMethod(java.util.List<String> ids);
    
    /**
     * 导出方法到 Excel
     * @param methods 方法列表
     * @return Excel 字节数组
     */
    byte[] exportMethodToExcel(java.util.List<com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod> methods);
    
    /**
     * 导入方法
     * @param file Excel 文件
     * @return 导入结果
     */
    Result importMethodFromExcel(MultipartFile file);
    
    /**
     * 删除方法
     * @param deleteMethodIdList 方法 ID 列表
     * @return 删除结果
     */
    Result<String> deleteMethod(Box deleteMethodIdList);
    
    /**
     * 查询方法详情
     * @param methodId 方法 ID
     * @return 方法详情
     */
    Result queryMethodDetail(String methodId);
    
    /**
     * 查询所有项目
     * @param updateBy 用户标识
     * @return 项目列表
     */
    Result queryAllProject(String updateBy);
    
    /**
     * 方法名称重复性检查
     * @param projectId 项目 ID
     * @param methodName 方法名称
     * @param measureType 测量类型
     * @return 是否重复
     */
    Result methodDuplicationCheck(String projectId, String methodName, String measureType);
    
    /**
     * 新建方法
     * @param methodDto 方法参数
     * @return 创建结果
     */
    Result createMethodPort(FMethodParamRequestDto methodDto);



}
