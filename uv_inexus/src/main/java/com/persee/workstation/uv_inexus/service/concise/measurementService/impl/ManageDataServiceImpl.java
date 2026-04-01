package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.dto.concise.common.Box;
import com.persee.workstation.uv_inexus.dto.concise.common.FMethodParamRequestDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureProject;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureMethodService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureProjectService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ManageDataService;
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
    
    // 日期格式化器
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public Result listOfMethods(String methodName, Integer pageNum, Integer pageSize, String sort, String updateBy) {
        // 1. 获取所有方法或按条件查询方法
        List<MethodBaseVo> methodVo;
        if (methodName == null || methodName.isEmpty()) {
            List<FMeasureMethod> methods = null;
            if (updateBy != null && !updateBy.isEmpty()) {
                LambdaQueryWrapper<FMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FMeasureMethod::getMethodCreator, updateBy);
                methods = measureMethodService.list(queryWrapper);  // 根据创建人查询
            } else {
                methods = measureMethodService.list();  // 查询所有方法
            }
            
            if (methods == null || methods.isEmpty()) {
                return Result.ok(createEmptyPageResult(pageNum, pageSize));
            }
            
            methodVo = methods.stream()
                    .map(method -> {
                        // 将 FMeasureMethod 拷贝到 MethodBaseVo
                        MethodBaseVo vo = new MethodBaseVo();
                        vo.setId(method.getId());
                        vo.setMethodName(method.getMethodName());
                        vo.setCreator(method.getMethodCreator());
                        vo.setMeasurementType(method.getMeasurementType());
                        vo.setCreateTime(method.getMethodCreateTime() != null ? 
                                method.getMethodCreateTime().format(formatter) : null);
                        
                        // 查询所属项目
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
        } else {
            List<FMeasureMethod> methods = null;
            if (updateBy != null && !updateBy.isEmpty()) {
                LambdaQueryWrapper<FMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.like(FMeasureMethod::getMethodName, methodName)
                           .eq(FMeasureMethod::getMethodCreator, updateBy);
                methods = measureMethodService.list(queryWrapper);  // 根据方法名和创建人查询
            } else {
                LambdaQueryWrapper<FMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.like(FMeasureMethod::getMethodName, methodName);
                methods = measureMethodService.list(queryWrapper);  // 根据方法名查询
            }
            
            methodVo = methods.stream()
                    .map(method -> {
                        // 将 FMeasureMethod 拷贝到 MethodBaseVo
                        MethodBaseVo vo = new MethodBaseVo();
                        vo.setId(method.getId());
                        vo.setMethodName(method.getMethodName());
                        vo.setCreator(method.getMethodCreator());
                        vo.setMeasurementType(method.getMeasurementType());
                        vo.setCreateTime(method.getMethodCreateTime() != null ? 
                                method.getMethodCreateTime().format(formatter) : null);
                        
                        // 查询所属项目
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
        }
        
        // 2. 获取每个方法的最近任务使用时间
        for (MethodBaseVo method : methodVo) {
            // 获取方法的 ID
            String methodId = method.getId();

            // 查询该方法的最近任务，按创建时间降序排序
            LambdaQueryWrapper<FMeasureTask> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FMeasureTask::getMethodKey, methodId)
                       .orderByDesc(FMeasureTask::getCreateTime)
                       .last("LIMIT 1");  // 只获取最近的一条任务
            
            FMeasureTask latestTask = taskService.getOne(queryWrapper);

            // 获取最近任务的创建时间，作为使用时间
            LocalDateTime lastUsedTime = latestTask != null ? latestTask.getCreateTime() : null;
            if (lastUsedTime == null) {
                method.setRecentUsageTime("—");
            } else {
                method.setRecentUsageTime(lastUsedTime.format(formatter));  // 将使用时间设置到方法对象中
            }
        }
        
        // 3. 根据使用时间进行排序 (根据 sort 参数判断排序方式)
        if ("0".equals(sort)) {  // 倒序 - 最近使用的在前
            methodVo.sort((m1, m2) -> {
                String time1 = m1.getRecentUsageTime();
                String time2 = m2.getRecentUsageTime();
                        
                boolean isTime1Null = time1 == null || "—".equals(time1);
                boolean isTime2Null = time2 == null || "—".equals(time2);
                        
                // 如果两者都没有使用时间，按创建时间倒序
                if (isTime1Null && isTime2Null) {
                    if (m1.getCreateTime() == null && m2.getCreateTime() == null) return 0;
                    if (m1.getCreateTime() == null) return 1;
                    if (m2.getCreateTime() == null) return -1;
                    return m2.getCreateTime().compareTo(m1.getCreateTime());
                }
                        
                // 有使用时间的排在前面
                if (isTime1Null) return 1;
                if (isTime2Null) return -1;
                        
                // 都有使用时间，按时间倒序
                return time2.compareTo(time1);
            });
        } else if ("1".equals(sort)) {  // 顺序 - 最早使用的在前
            methodVo.sort((m1, m2) -> {
                String time1 = m1.getRecentUsageTime();
                String time2 = m2.getRecentUsageTime();
                        
                boolean isTime1Null = time1 == null || "—".equals(time1);
                boolean isTime2Null = time2 == null || "—".equals(time2);
                        
                // 如果两者都没有使用时间，按创建时间顺序
                if (isTime1Null && isTime2Null) {
                    if (m1.getCreateTime() == null && m2.getCreateTime() == null) return 0;
                    if (m1.getCreateTime() == null) return 1;
                    if (m2.getCreateTime() == null) return -1;
                    return m1.getCreateTime().compareTo(m2.getCreateTime());
                }
                        
                // 没有使用时间的排在后面
                if (isTime1Null) return 1;
                if (isTime2Null) return -1;
                        
                // 都有使用时间，按时间顺序
                return time1.compareTo(time2);
            });
        }
        
        // 4. 分页处理
        int total = methodVo.size();  // 总条数
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, methodVo.size());
        
        // 处理边界情况
        if (fromIndex >= total) {
            return Result.ok(createEmptyPageResult(pageNum, pageSize));
        }

        // 获取当前页的数据
        List<MethodBaseVo> pagedResult = methodVo.subList(fromIndex, toIndex);

        // 构造分页返回结果
        Map<String, Object> response = new HashMap<>();
        response.put("data", pagedResult);  // 数据
        response.put("total", total);       // 总条数
        response.put("pageNum", pageNum);  // 当前页码
        response.put("pageSize", pageSize);  // 每页大小
        response.put("totalPages", (int) Math.ceil((double) total / pageSize)); // 总页数

        return Result.ok(response);
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
    public Result methodDuplicationCheck(String projectId, String methodName, String measureType) {
        try {
            // 参数校验
            if (projectId == null || projectId.isEmpty() || 
                methodName == null || methodName.isEmpty() || 
                measureType == null || measureType.isEmpty()) {
                return Result.fail("参数不能为空");
            }
            
            // 构建查询条件
            LambdaQueryWrapper<FMeasureMethod> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FMeasureMethod::getProjectItemKey, projectId)
                       .eq(FMeasureMethod::getMethodName, methodName)
                       .eq(FMeasureMethod::getMeasurementType, measureType);
            
            // 查询是否存在
            long count = measureMethodService.count(queryWrapper);
            
            // 打印日志
            System.out.println(String.format("方法名称查重检查：projectId=%s, methodName=%s, count=%d", 
                    projectId, methodName, count));
            
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
