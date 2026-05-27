package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dto.concise.common.Box;
import com.persee.workstation.uv_inexus.dto.concise.common.FMethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.TaskManagementDto;
import com.persee.workstation.uv_inexus.i18n.I18nMessageSource;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TaskQueryParam;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysUserService;
import com.persee.workstation.uv_inexus.vo.measurementVo.MethodBaseVo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Service
public class ManageDataServiceImpl implements ManageDataService {
    @Autowired
    private FMeasureMethodService measureMethodService;
    
    @Autowired
    private FMeasureProjectService projectService;
    
    @Autowired
    private FMeasureTaskService taskService;

    @Autowired
    private FMeasureSampleService sampleService;

    @Autowired
    private FMeasureDataService dataService;

    @Autowired
    private FMeasureAnalysisDataService analysisDataService;

    @Autowired
    private FMeasureAnalysisImageService analysisImageService;

    @Autowired
    private SysUserService userService;

    
    // 日期格式化器
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Result listOfMethods(String methodName, Integer pageNum, Integer pageSize, String sort, String updateBy) {
        // 1. 构建查询条件
        LambdaQueryWrapper<FMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();

        // 方法名模糊查询
        if (methodName != null && !methodName.isEmpty()) {
            queryWrapper.like(FMeasureMethod::getMethodName, methodName);
        }

        // 创建人精确查询
        if (updateBy != null && !updateBy.isEmpty()) {
            queryWrapper.eq(FMeasureMethod::getMethodCreator, updateBy);
        }

        // 关键修改：直接在数据库层面按创建时间排序
        if ("1".equals(sort)) {
            queryWrapper.orderByAsc(FMeasureMethod::getMethodCreateTime);  // 顺序（旧的在前面）
        } else {
            queryWrapper.orderByDesc(FMeasureMethod::getMethodCreateTime); // 倒序（新的在前面），默认
        }

        // 查询所有方法
        List<FMeasureMethod> methods = measureMethodService.list(queryWrapper);

        if (methods == null || methods.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("data", new ArrayList<>());
            emptyResult.put("total", 0);
            emptyResult.put("pageNum", pageNum);
            emptyResult.put("pageSize", pageSize);
            emptyResult.put("totalPages", 0);
            return Result.ok(emptyResult);
        }

        // 2. 转换为Vo
        List<MethodBaseVo> methodVo = methods.stream()
                .map(method -> {
                    MethodBaseVo vo = new MethodBaseVo();
                    vo.setId(method.getId());
                    vo.setMethodName(method.getMethodName());
                    vo.setCreator(method.getMethodCreator());
                    vo.setMeasurementType(method.getMeasurementType());
                    vo.setCreateTime(method.getMethodCreateTime() != null ?
                            method.getMethodCreateTime().format(formatter) : null);
                    vo.setRecentUsageTime(method.getMethodUseTime() != null ?
                            method.getMethodUseTime().format(formatter) : "—");

                    if (method.getProjectItemKey() != null) {
                        FMeasureProject project = getProjectById(method.getProjectItemKey());
                        if (project != null) {
                            vo.setProject(project.getProjectName());
                            vo.setProjectId(project.getId());
                        }
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        // 3. 分页处理
        int total = methodVo.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        if (fromIndex >= total) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("data", new ArrayList<>());
            emptyResult.put("total", total);
            emptyResult.put("pageNum", pageNum);
            emptyResult.put("pageSize", pageSize);
            emptyResult.put("totalPages", (int) Math.ceil((double) total / pageSize));
            return Result.ok(emptyResult);
        }

        List<MethodBaseVo> pagedResult = methodVo.subList(fromIndex, toIndex);

        // 4. 返回结果
        Map<String, Object> response = new HashMap<>();
        response.put("data", pagedResult);
        response.put("total", total);
        response.put("pageNum", pageNum);
        response.put("pageSize", pageSize);
        response.put("totalPages", (int) Math.ceil((double) total / pageSize));

        return Result.ok(response);
    }

    /**
     * 获取任务列表
     * @param taskQueryParam
     * @return
     */
    @Override
    public Result<Map<String, Object>> getTasksManagementList(TaskQueryParam taskQueryParam) {
        // 参数校验
        if (taskQueryParam == null) {
            //查询参数不能为空
            return Result.fail("00306002");
        }

        // 分页参数默认值设置
        int pageNum = taskQueryParam.getPage() != null ? taskQueryParam.getPage() : 1;
        int pageSize = taskQueryParam.getSize() != null ? taskQueryParam.getSize() : 10;

        Page<FMeasureTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FMeasureTask> queryWrapper = new LambdaQueryWrapper<>();

        // 模糊查询条件（添加空值判断）
        if (StringUtils.isNotBlank(taskQueryParam.getKeyword())) {
            queryWrapper.like(FMeasureTask::getTaskName, taskQueryParam.getKeyword());
        }

        // 根据名称查询当前用户id
        if (StringUtils.isNotBlank(taskQueryParam.getUpdateBy())){
            SysUserPO user = userService.getOne(new QueryWrapper<SysUserPO>().eq("username", taskQueryParam.getUpdateBy()));
            queryWrapper.eq(FMeasureTask::getUserId, user.getId());
        }


        Page<FMeasureTask> result = taskService.page(page, queryWrapper);
        List<FMeasureTask> records = result.getRecords();
        List<TaskManagementDto> dtoList = new ArrayList<>();
        for (FMeasureTask record : records) {
            String methodName;
            if (record.getMethodKey()!=null){
                FMeasureMethod tMeasureMethod = measureMethodService.getById(record.getMethodKey());
                if (tMeasureMethod!=null){
                    methodName=tMeasureMethod.getMethodName();
                }else{
                    methodName="";
                }
            }else {
                methodName="";
            }

            List<TaskSpecimen> specimen = JSON.parseArray(record.getSampleKey(), TaskSpecimen.class);
            TaskManagementDto dto = TaskManagementDto.builder()
                    .id(record.getId())
                    .taskName(record.getTaskName())
                    .startTime(record.getCreateTime())
                    .sampleKey(specimen != null ? specimen.size() : 0)
                    .methodId(record.getMethodKey())
                    .methodName(methodName)
                    .measureType(record.getMeasureType())
                    .taskStatus(record.getIsSave()==(null)?"未保存":record.getIsSave().equals(Constants.PARAM_ZERO)?"未保存":"已保存")
                    .build();
            dtoList.add(dto);
        }
        // 创建map结构用于返回
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("current", result.getCurrent());      // 当前页数
        resultMap.put("size", result.getSize());           // 每页记录数
        resultMap.put("total", result.getTotal());         // 总记录数
        resultMap.put("pages", result.getPages());         // 总页数
        resultMap.put("result", dtoList);                 // 数据列表
        return Result.ok(resultMap);
    }

    /**
     * 经典: 删除任务
     * @param List
     * @return
     */
    @Override
    public Result<String> deleteTask(Box List) {
        List<String> deleteTaskIdList = List.getParams();
        // 遍历要删除的任务
        for (String deleteTaskId : deleteTaskIdList) {
            try {
                // 根据当前任务id查询当前任务数据
                FMeasureTask tMeasureTask = taskService.getById(deleteTaskId);
                // 取出并转换当前任务的所有样品
                List<FMeasureSample> tMeasureSamples = JSON.parseArray(tMeasureTask.getSampleKey(), FMeasureSample.class);
                if (tMeasureSamples!=null&&tMeasureSamples.size()!=0) {
                    for (FMeasureSample TMeasureSample : tMeasureSamples) {
                        String specimenKey = TMeasureSample.getId();
                        sampleService.removeById(specimenKey);
                    }
                }
                // 查询当前任务是否有测量数据,如果有则进行删除
                LambdaQueryWrapper<FMeasureData> dataWrapper = new LambdaQueryWrapper<>();
                dataWrapper.eq(FMeasureData::getTaskId, deleteTaskId);
                dataWrapper.last("LIMIT 1");
                FMeasureData tMeasureData = dataService.getOne(dataWrapper);
                if (tMeasureData != null) {
                    dataService.remove(new LambdaQueryWrapper<FMeasureData>().eq(FMeasureData::getTaskId, deleteTaskId));
                }
                // 查询当前任务是否有分析数据,如果有则进行删除(附加分析图片表数据)
                LambdaQueryWrapper<FMeasureAnalysisData> analyWrapper = new LambdaQueryWrapper<>();
                analyWrapper.eq(FMeasureAnalysisData::getTaskId, deleteTaskId);
                List<FMeasureAnalysisData> analysisData = analysisDataService.list(analyWrapper);
                if (analysisData != null && analysisData.size() != 0) {
                    for (FMeasureAnalysisData tMeasureAnalysisData : analysisData) {
                        String analysisDataKey = tMeasureAnalysisData.getId();
                        QueryWrapper<FMeasureAnalysisImage> wrapper = new QueryWrapper<>();
                        wrapper.eq("analysis_id", analysisDataKey);
                        FMeasureAnalysisImage byId = analysisImageService.getOne(wrapper);
                        if (byId != null) {
                            analysisImageService.remove(wrapper);
                        }
                        analysisDataService.removeById(analysisDataKey);
                    }
                }
                boolean res = taskService.removeById(deleteTaskId);
                if (!res) {
                    //任务删除失败
                    return Result.fail("0030101D");
                }
            } catch (Exception e) {
                boolean i = taskService.removeById(deleteTaskId);
                if (!i) {
                    //任务删除失败
                    return Result.fail("0030101D");
                }
            }

        }
        return Result.ok();
    }

    /**
     * 根据 ID 获取项目
     */
    private FMeasureProject getProjectById(String projectId) {
        try {
            return projectService.getById(projectId);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 创建空的分页结果
     */
    private Map<String, Object> createEmptyPageResult(Integer pageNum, Integer pageSize) {
        Map<String, Object> response = new HashMap<>();
        response.put("data", new ArrayList<>());
        response.put("total", 0);
        response.put("pageNum", pageNum);
        response.put("pageSize", pageSize);
        response.put("totalPages", 0);
        return response;
    }
    
    /**
     * 导出方法
     */
    @Override
    public List<FMeasureMethod> exportMethod(List<String> ids) {
        return measureMethodService.listByIds(ids);
    }
    
    /**
     * 导出方法到 Excel
     */
    @Override
    public byte[] exportMethodToExcel(List<FMeasureMethod> methods) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("测量方法");
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "方法 ID", "前处理 Key", "多个计算公式 Key", "所属项目 Key", "方法名称",
                    "测量类型", "测量参数信息", "是否共享方法", "方法创建时间", "方法创建人",
                    "方法使用时间", "是否草稿", "使用状态", "预留字段 5"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                // 设置表头样式
                CellStyle headerStyle = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                cell.setCellStyle(headerStyle);
            }
            
            // 填充数据
            for (int i = 0; i < methods.size(); i++) {
                Row row = sheet.createRow(i + 1);
                FMeasureMethod method = methods.get(i);
                
                row.createCell(0).setCellValue(method.getId() != null ? method.getId() : "");
                row.createCell(1).setCellValue(method.getPreprocessKey() != null ? method.getPreprocessKey() : "");
                row.createCell(2).setCellValue(method.getMultipleCalculationFormulaKey() != null ? method.getMultipleCalculationFormulaKey() : "");
                row.createCell(3).setCellValue(method.getProjectItemKey() != null ? method.getProjectItemKey() : "");
                row.createCell(4).setCellValue(method.getMethodName() != null ? method.getMethodName() : "");
                row.createCell(5).setCellValue(method.getMeasurementType() != null ? method.getMeasurementType() : "");
                row.createCell(6).setCellValue(method.getMeasurementParamInfo() != null ? method.getMeasurementParamInfo() : "");
                row.createCell(7).setCellValue(method.getIsSharingMethod() != null ? method.getIsSharingMethod() : "");
                row.createCell(8).setCellValue(method.getMethodCreateTime() != null ?
                        method.getMethodCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
                row.createCell(9).setCellValue(method.getMethodCreator() != null ? method.getMethodCreator() : "");
                row.createCell(10).setCellValue(method.getMethodUseTime() != null ?
                        method.getMethodUseTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
                row.createCell(11).setCellValue(method.getIsDraft() != null ? method.getIsDraft() : "");
                row.createCell(12).setCellValue(method.getUseState() != null ? method.getUseState() : "");
                row.createCell(13).setCellValue(method.getField5() != null ? method.getField5() : "");
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 将工作簿写入字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 从 Excel 导入方法
     */
    @Override
    public Result importMethodFromExcel(MultipartFile file) {
        try {
            List<FMeasureMethod> methodList = new ArrayList<>();
            
            // 解析 Excel 文件
            try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                
                // 从第二行开始读取数据（跳过表头）
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    
                    FMeasureMethod method = new FMeasureMethod();
                    
                    // 读取各列数据
                    method.setId(getCellValueAsString(row.getCell(0)));
                    method.setPreprocessKey(getCellValueAsString(row.getCell(1)));
                    method.setMultipleCalculationFormulaKey(getCellValueAsString(row.getCell(2)));
                    method.setProjectItemKey(getCellValueAsString(row.getCell(3)));
                    method.setMethodName(getCellValueAsString(row.getCell(4)));
                    method.setMeasurementType(getCellValueAsString(row.getCell(5)));
                    method.setMeasurementParamInfo(getCellValueAsString(row.getCell(6)));
                    method.setIsSharingMethod(getCellValueAsString(row.getCell(7)));
                    
                    // 处理时间字段
                    String createTimeStr = getCellValueAsString(row.getCell(8));
                    if (createTimeStr != null && !createTimeStr.isEmpty()) {
                        try {
                            LocalDateTime createTime = LocalDateTime.parse(createTimeStr,
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            method.setMethodCreateTime(createTime);
                        } catch (Exception e) {
                            // 时间格式不正确，跳过设置
                        }
                    }
                    
                    method.setMethodCreator(getCellValueAsString(row.getCell(9)));
                    String useTimeStr = getCellValueAsString(row.getCell(10));
                    if (useTimeStr != null && !useTimeStr.isEmpty()) {
                        try {
                            LocalDateTime useTime = LocalDateTime.parse(useTimeStr,
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            method.setMethodUseTime(useTime);
                        } catch (Exception e) {
                            // 时间格式不正确，跳过设置
                        }
                    }
                    
                    method.setIsDraft(getCellValueAsString(row.getCell(11)));
                    method.setUseState(getCellValueAsString(row.getCell(12)));
                    method.setField5(getCellValueAsString(row.getCell(13)));
                    
                    methodList.add(method);
                }
            }
            
            // 保存到数据库
            if (!methodList.isEmpty()) {
                for (FMeasureMethod method : methodList) {
                    // 判断是否存在
                    FMeasureMethod exist = measureMethodService.getById(method.getId());
                    if (exist == null) {
                        // 新建
                        if (method.getId() == null || method.getId().isEmpty()) {
                            method.setId(UUID.randomUUID().toString());
                        }
                        measureMethodService.save(method);
                    } else {
                        // 更新
                        measureMethodService.updateById(method);
                    }
                }
            }
            
            return Result.ok("成功导入 " + methodList.size() + " 个方法");
        } catch (Exception e) {
            return Result.fail("导入方法失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取单元格的字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }
    
    /**
     * 删除方法
     */
    @Override
    public Result<String> deleteMethod(Box deleteMethodIdList) {
        try {
            List<String> methodIds = deleteMethodIdList.getParams();
            
            if (methodIds == null || methodIds.isEmpty()) {
                return Result.fail("未指定要删除的方法");
            }
            
            // 检查方法是否被任务使用
            for (String methodId : methodIds) {
                LambdaQueryWrapper<FMeasureTask> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FMeasureTask::getMethodKey, methodId);
                Long count = taskService.count(queryWrapper);
                
                if (count > 0) {
                    FMeasureMethod method = measureMethodService.getById(methodId);
                    String methodName = method != null ? method.getMethodName() : methodId;
                    return Result.fail(String.format("方法 '%s' 已被任务使用，无法删除", methodName));
                }
            }
            
            // 批量删除方法
            measureMethodService.removeByIds(methodIds);
            
            return Result.ok("成功删除 " + methodIds.size() + " 个方法");
        } catch (Exception e) {
            return Result.fail("删除方法失败：" + e.getMessage());
        }
    }
    
    /**
     * 查询方法详情
     */
    @Override
    public Result queryMethodDetail(String methodId) {
        try {
            // 根据方法 ID 查询方法
            FMeasureMethod method = measureMethodService.getById(methodId);
            
            if (method == null) {
                return Result.fail("方法不存在");
            }
            
            // 将方法的参数信息转换为 Map 返回
            Map<String, Object> result = new HashMap<>();
            result.put("id", method.getId());
            result.put("methodName", method.getMethodName());
            result.put("measurementType", method.getMeasurementType());
            result.put("measurementParamInfo", method.getMeasurementParamInfo());
            result.put("projectItemKey", method.getProjectItemKey());
            result.put("methodCreator", method.getMethodCreator());
            result.put("methodCreateTime", method.getMethodCreateTime() != null ? 
                    method.getMethodCreateTime().format(formatter) : null);
            result.put("isSharingMethod", method.getIsSharingMethod());
            result.put("isDraft", method.getIsDraft());
            result.put("useState", method.getUseState());
            
            // 查询所属项目
            if (method.getProjectItemKey() != null) {
                FMeasureProject project = getProjectById(method.getProjectItemKey());
                if (project != null) {
                    result.put("projectName", project.getProjectName());
                    result.put("projectId", project.getId());
                }
            }
            
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail("查询方法详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 查询所有项目
     */
    @Override
    public Result queryAllProject(String updateBy) {
        try {
            // 根据用户标识查询项目
            LambdaQueryWrapper<FMeasureProject> queryWrapper = new LambdaQueryWrapper<>();
            if (updateBy != null && !updateBy.isEmpty()) {
                // 查询用户创建的项目
                queryWrapper.eq(FMeasureProject::getCreatorKey, updateBy);
            }
            List<FMeasureProject> projects = projectService.list(queryWrapper);
            
            return Result.ok(projects);
        } catch (Exception e) {
            return Result.fail("查询项目失败：" + e.getMessage());
        }
    }
    
    /**
     * 方法名称重复性检查
     */
    @Override
    public Result methodDuplicationCheck(String methodName, String measureType) {
        try {
            // 参数校验
            if (methodName == null || methodName.isEmpty() ||
                measureType == null || measureType.isEmpty()) {
                return Result.fail("参数不能为空");
            }
            
            // 构建查询条件
            LambdaQueryWrapper<FMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FMeasureMethod::getMethodName, methodName)
                       .eq(FMeasureMethod::getMeasurementType, measureType);
            
            // 查询是否存在
            long count = measureMethodService.count(queryWrapper);
            
            // 打印日志
            System.out.println(String.format("方法名称查重检查：methodName=%s, count=%d", methodName, count));
            
            if (count > 0) {
                return Result.ok(true);  // 存在重复
            } else {
                return Result.ok(false); // 不存在重复
            }
        } catch (Exception e) {
            return Result.fail("方法名称检查失败：" + e.getMessage());
        }
    }
    
    /**
     * 新建方法
     */
    @Override
    public Result createMethodPort(FMethodParamRequestDto methodDto) {
        try {
            // 根据方法名称去方法表中进行查询，查看是否有重复
            LambdaQueryWrapper<FMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FMeasureMethod::getMethodName, methodDto.getMethodName())
                       .eq(FMeasureMethod::getMeasurementType, methodDto.getMethodType())
                       .eq(FMeasureMethod::getProjectItemKey, methodDto.getProjectId());
            List<FMeasureMethod> list = measureMethodService.list(queryWrapper);
            
            if (list.size() > 0) {
                // 方法名称重复
                return Result.fail("方法名称重复");
            }
            
            // 创建测量方法表
            FMeasureMethod fMeasureMethod = new FMeasureMethod();
            fMeasureMethod.setProjectItemKey(methodDto.getProjectId());
            fMeasureMethod.setMethodName(methodDto.getMethodName());
            fMeasureMethod.setMeasurementType(methodDto.getMethodType());
            fMeasureMethod.setMeasurementParamInfo(com.alibaba.fastjson.JSON.toJSONString(methodDto.getParams()));
            fMeasureMethod.setMethodCreator(methodDto.getUpdateBy());
            fMeasureMethod.setMethodCreateTime(LocalDateTime.now());
//            fMeasureMethod.setIsDelete(Constants.PARAM_ZERO);
            
            boolean save = measureMethodService.save(fMeasureMethod);
            if (save) {
                // 创建成功
                return Result.ok("创建成功");
            } else {
                // 创建失败
                return Result.fail("创建失败");
            }
        } catch (Exception e) {
            return Result.fail("创建方法失败：" + e.getMessage());
        }
    }
}
