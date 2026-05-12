package com.persee.workstation.uv_inexus.service.factory_inspection.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.component.ExcelDynamicMerger;
import com.persee.workstation.uv_inexus.component.WorkbookCacheManager;
import com.persee.workstation.uv_inexus.dao.factory_inspection.TemplateMapper;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateCreationRequest;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateQeqDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateResponseDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateUpdateRequest;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.*;
import com.persee.workstation.uv_inexus.service.factory_inspection.*;
import com.persee.workstation.uv_inexus.util.ExcelCellEditor;
import com.persee.workstation.uv_inexus.util.ExcelCellLocator;
import com.persee.workstation.uv_inexus.util.ExcelUpdateNotifier;
import com.persee.workstation.uv_inexus.util.IdGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.*;

@Slf4j
@Service
public class TemplateServiceImpl extends ServiceImpl<TemplateMapper, ExcelTemplate> implements TemplateService {
    @Autowired
    private TemplateMapper templateMapper;
    @Autowired
    private ExcelDataAssetService excelDataAssetService;
    /*@Value("${excel.save.path}")
    private String excelSavePath;*/
    @Autowired
    private ExcelCellLocator excelCellLocator;
    @Autowired
    private ExcelCellEditor excelCellEditor;
    @Autowired
    private ExcelUpdateNotifier excelUpdateNotifier;
    @Autowired
    private WorkbookCacheManager workbookCacheManager;
    @Autowired
    private TaskExecutionDetailService detailService;
    @Autowired
    @Lazy
    private TaskManagementService taskManagementService;
    @Autowired
    private CalArtifactsService calArtifactsService;
    @Autowired
    private CalStdCatsService calStdCatsService;
    @Autowired
    private WorkbookDataService workbookDataService;
    @Autowired
    private ExcelDynamicMerger excelDynamicMerger;
    private final Map<String, Integer> operationCount = new ConcurrentHashMap<>();


    @Override
    public Result<List<String>> getHeaders(String templateCode) {
        try {
            // 根据模板编码查询模板信息
            QueryWrapper<ExcelTemplate> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("template_code", templateCode);
            ExcelTemplate template = this.getOne(queryWrapper);

            if (template == null) {
                return Result.fail("未找到模板: " + templateCode);
            }

            // 从模板中获取字段列表（假设字段以逗号分隔存储在field_config字段中）
            String fieldConfig = template.getColumnConfig();
            if (fieldConfig == null || fieldConfig.isEmpty()) {
                return Result.fail("模板字段配置为空");
            }

            List<String> headers = Arrays.asList(fieldConfig.split(","));
            return Result.ok(headers);
        } catch (Exception e) {
            return Result.fail("获取字段列表失败: " + e.getMessage());
        }
    }


    @Override
    @DS("factory_inspection")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> createTemplate(TemplateCreationRequest request) {
        if (StringUtils.equals(request.getType(), TEMPLATE_CREATE.getCode())) {
            log.info("创建模板开始");
            try {

                // 验证模板名称是否重复
                LambdaQueryWrapper<ExcelTemplate> wrapper1 = new LambdaQueryWrapper<>();
                wrapper1.eq(ExcelTemplate::getTemplateName, request.getTemplateName());
                ExcelTemplate one = this.getOne(wrapper1);
                if (one != null) {
                    return Result.fail("模板名称重复！！");
                }

                // 创建新模板
                ExcelTemplate template = new ExcelTemplate();
//                template.setTemplateCode(templateCode);
                template.setTemplateName(request.getTemplateName());
                template.setCreateBy(request.getUpdateBy());
                //非内置模板
                template.setCategory(TEMPLATE_USER_DEFINED.getCode());
                template.setRemark(request.getRemark());
                template.setId(IdGeneratorUtil.generateIdWithPrefix("template"));
                template.setParentId(request.getTemplateCode());
                template.setColumnConfig("{}");
                String templateCode = template.getId();

                // 保存到数据库
                boolean saved = this.save(template);
                LambdaQueryWrapper<ExcelDataAsset> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ExcelDataAsset::getTemplateCode, request.getTemplateCode());

                List<Map<String, Object>> selectedRows = new ArrayList<>();
                request.getSelectedRows().entrySet()
                        .forEach(entry -> {
//                            String key = entry.getKey(); // 这里可以获取到 key
                            List<Map<String, Object>> valueList = entry.getValue();
                            // 如果需要在数据中添加 key 信息
                            //                                dataMap.put("id", key); // 可选：将 key 作为数据的一部分
                            selectedRows.addAll(valueList);
                        });


//                List<Map<String, Object>> selectedRows = request.getSelectedRows().values().stream()
//                        .flatMap(List::stream)
//                        .collect(Collectors.toList());
                int rowIndex = 1; // 从第1行开始计数（跳过表头）
                ArrayList<ExcelDataAsset> list = new ArrayList<>();

                for (Map.Entry<String, List<Map<String, Object>>> entry : request.getSelectedRows().entrySet()) {
                    String key = entry.getKey();
                    Map<String, Object> dataMap = entry.getValue().get(0);
//                }
//
//                for (Map<String, Object> dataMap : selectedRows) {
                    ExcelDataAsset asset = new ExcelDataAsset();
                    asset.setId(IdGeneratorUtil.generateIdWithPrefix("asset"));
                    asset.setTemplateCode(templateCode);
                    asset.setBatchNo(String.valueOf(rowIndex++));
                    dataMap.put("id", asset.getId());
                    asset.setStructuredData(JSON.toJSONString(dataMap));
                    asset.setImportStatus("SUCCESS");
                    LambdaQueryWrapper<TaskExecutionDetail> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(TaskExecutionDetail::getItemId, key)
                            .isNull(TaskExecutionDetail::getTaskId);
                    //这块查出来是两个，一个校零，一个测量
                    List<TaskExecutionDetail> detailList = detailService.list(queryWrapper);
                    if (CollectionUtils.isNotEmpty(detailList)) {
                        for (TaskExecutionDetail detail : detailList) {
                            detail.setItemId(asset.getId());
                            detail.setId(IdGeneratorUtil.generateId());
                        }
                    }
                    detailService.saveBatch(detailList);
                    list.add(asset);
                }
                excelDataAssetService.saveBatch(list);
                //不存在的检测项
                List<Map<String, Object>> rowsNotInTemplate = new ArrayList<>();


                // 从request.getSelectedRows()中筛选key以"new"开头的条目
                Map<String, List<Map<String, Object>>> filteredRows = request.getSelectedRows().entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith("new"))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                // 将筛选后的数据添加到rowsNotInTemplate列表中
                filteredRows.values().forEach(rowsNotInTemplate::addAll);
                buildResultTemplate(template.getTemplateCode(), rowsNotInTemplate);

                if (saved) {
                    return Result.ok("模板创建成功");
                } else {
                    return Result.fail("模板创建失败");
                }
            } catch (Exception e) {
                return Result.fail("模板创建失败: " + e.getMessage());
            }
        }
        if (StringUtils.equals(request.getType(), TEMPLATE_UPDATE.getCode())) {
            log.info("编辑模板开始");
            try {
                // 参数校验
                if (StringUtils.isBlank(request.getId())) {
                    return Result.fail("模板ID不能为空");
                }

                // 查找现有模板
                ExcelTemplate existingTemplate = this.getById(request.getId());
                if (existingTemplate == null) {
                    return Result.fail("未找到要编辑的模板");
                }
                if (StringUtils.equals(existingTemplate.getCategory(), TEMPLATE_BUILT_IN.getCode())) {
                    return Result.fail("内置模板不允许编辑");
                }
                // 更新模板信息
                ExcelTemplate template = new ExcelTemplate();
                template.setId(request.getId());
                template.setTemplateName(request.getTemplateName());
                template.setRemark(request.getRemark());
                template.setUpdateBy(request.getUpdateBy());
                template.setUpdated(LocalDateTime.now());
                template.setCategory(TEMPLATE_USER_DEFINED.getCode()); // 非内置模板

                boolean updated = this.updateById(template);

                if (updated) {
                    // 删除原有的数据资产
                    LambdaQueryWrapper<ExcelDataAsset> deleteWrapper = new LambdaQueryWrapper<>();
                    deleteWrapper.eq(ExcelDataAsset::getTemplateCode, existingTemplate.getTemplateCode());
                    excelDataAssetService.remove(deleteWrapper);

                    // 保存新的数据资产
                    List<Map<String, Object>> selectedRows = request.getSelectedRows().values().stream()
                            .flatMap(List::stream)
                            .collect(Collectors.toList());
                    int rowIndex = 1;
                    ArrayList<ExcelDataAsset> list = new ArrayList<>();
                    for (Map<String, Object> dataMap : selectedRows) {
                        ExcelDataAsset asset = new ExcelDataAsset();
                        asset.setId(IdGeneratorUtil.generateIdWithPrefix("asset"));
                        asset.setTemplateCode(existingTemplate.getTemplateCode()); // 使用原模板编码
                        asset.setBatchNo(String.valueOf(rowIndex++));
                        asset.setStructuredData(JSON.toJSONString(dataMap));
                        asset.setImportStatus("SUCCESS");
                        list.add(asset);
                    }
                    excelDataAssetService.saveBatch(list);

                    return Result.ok("模板编辑成功");
                } else {
                    return Result.fail("模板编辑失败");
                }
            } catch (Exception e) {
                return Result.fail("模板编辑失败: " + e.getMessage());
            }
        }
        return Result.fail("未知错误");
    }

    @Override
    public Result updateTemplate(TemplateUpdateRequest request) {
        try {
            // 参数校验
            if (request.getTemplateCode() == null || request.getTemplateCode().isEmpty()) {
                return Result.fail("模板编码不能为空");
            }

            if (request.getSelectedFields() == null || request.getSelectedFields().isEmpty()) {
                return Result.fail("选中字段不能为空");
            }

            // 查找现有模板
            QueryWrapper<ExcelTemplate> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("template_code", request.getTemplateCode());
            ExcelTemplate existingTemplate = this.getOne(queryWrapper);

            if (existingTemplate == null) {
                return Result.fail("未找到模板: " + request.getTemplateCode());
            }

            // 更新字段配置（JSON格式）
            List<ExcelTemplate.ColumnConfig> columnConfigs = new ArrayList<>();
            for (String header : request.getSelectedFields()) {
                ExcelTemplate.ColumnConfig config = new ExcelTemplate().new ColumnConfig();
//                config.setFieldName(header);  // 字段名暂时与表头相同
                config.setExcelHeader(header);  // Excel表头名
                config.setDataType("STRING");  // 默认数据类型为STRING
                columnConfigs.add(config);
            }
            existingTemplate.setColumnConfig(JSON.toJSONString(columnConfigs));

            // 更新数据库
            boolean updated = this.updateById(existingTemplate);
            if (updated) {
                return Result.ok("模板更新成功");
            } else {
                return Result.fail("模板更新失败");
            }
        } catch (Exception e) {
            return Result.fail("模板更新失败: " + e.getMessage());
        }
    }

    /**
     * 根据原始结果模板生成定制结果模板
     *
     * @param
     * @param sheetRetainRows
     * @param rowsNotInTemplate
     * @return
     * @throws IOException
     */
    // ... existing code ...
    // ... existing code ...
    public Result editExcel(Workbook workbook, ExcelTemplate template, Map<String, Set<String>> sheetRetainRows, List<Map<String, Object>> rowsNotInTemplate) throws IOException {
//        System.out.println("88888888888888888888888888888888" + resultTemplate);
////        File file = new File(resultTemplate);
//        ClassPathResource resource = new ClassPathResource(resultTemplate);
//        File file = resource.getFile();
////        boolean b = file.getParentFile().mkdirs();// 创建父目录（如果不存在）
////        InputStream is = Files.newInputStream(Paths.get(resultTemplate));
//        InputStream is = Files.newInputStream(file.toPath());
//        Workbook workbook = WorkbookFactory.create(is);

        try {
            // 【新增】在编辑前先获取原有的黄色高亮列信息
            // 根据模板是否有任务来决定查询条件（优先级从高到低）：
            // 1. 优先查询任务级别的WorkbookData（task_id = taskId）
            // 2. 如果没有任务，再查询模板级别的WorkbookData（task_id = templateCode）

            WorkbookData existingWorkbookData = null;

            // 第一步：先查找该模板关联的任务
            LambdaQueryWrapper<TaskManagement> taskQueryWrapper = new LambdaQueryWrapper<>();
            taskQueryWrapper.eq(TaskManagement::getUseTemplate, template.getId())
                    .orderByDesc(TaskManagement::getCreated)
                    .last("LIMIT 1");

            TaskManagement task = taskManagementService.getOne(taskQueryWrapper);

            if (task != null) {
                log.info("找到模板关联的任务: {}", task.getId());

                // 按任务ID查询WorkbookData（任务级别的数据）
                LambdaQueryWrapper<WorkbookData> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(WorkbookData::getTaskId, task.getId())
                        .orderByDesc(WorkbookData::getCreateTime)
                        .last("LIMIT 1");

                existingWorkbookData = workbookDataService.getOne(queryWrapper);

                if (existingWorkbookData != null) {
                    log.info("找到任务{}的WorkbookData", task.getId());
                } else {
                    log.warn("任务{}没有对应的WorkbookData", task.getId());
                }
            } else {
                log.info("模板{}没有关联的任务，尝试查询模板级别的WorkbookData", template.getTemplateCode());
            }

            // 第二步：如果没有找到任务级别的数据，查询模板级别的数据
            if (existingWorkbookData == null) {
                LambdaQueryWrapper<WorkbookData> templateQueryWrapper = new LambdaQueryWrapper<>();
                templateQueryWrapper.eq(WorkbookData::getTaskId, template.getTemplateCode())
                        .orderByDesc(WorkbookData::getCreateTime)
                        .last("LIMIT 1");

                existingWorkbookData = workbookDataService.getOne(templateQueryWrapper);

                if (existingWorkbookData != null) {
                    log.info("找到模板{}级别的WorkbookData", template.getTemplateCode());
                } else {
                    log.warn("模板{}也没有对应的WorkbookData", template.getTemplateCode());
                }
            }

            Map<String, List<String>> existingYellowColumns = null;
            if (existingWorkbookData != null && existingWorkbookData.getYellowHighlightedColumns() != null) {
                ObjectMapper mapper = new ObjectMapper();
                existingYellowColumns = mapper.readValue(existingWorkbookData.getYellowHighlightedColumns(),
                        new TypeReference<Map<String, List<String>>>(){});
                log.info("获取到原有黄色高亮列信息: {}, taskId: {}", existingYellowColumns, existingWorkbookData.getTaskId());
            } else {
                log.warn("未找到模板{}的黄色高亮列信息", template.getTemplateCode());
            }

            // 1. 删除未指定的Sheet
            List<String> existingSheets = IntStream.range(0, workbook.getNumberOfSheets())
                    .mapToObj(workbook::getSheetName)
                    .collect(Collectors.toList());

            existingSheets.stream()
                    .filter(sheetName -> !sheetRetainRows.containsKey(sheetName))
                    .forEach(sheetName -> workbook.removeSheetAt(workbook.getSheetIndex(sheetName)));

            // 【新增】过滤掉已被删除的sheet对应的黄色高亮列信息
            if (existingYellowColumns != null && !existingYellowColumns.isEmpty()) {
                // 获取当前workbook中保留的sheet名称集合
                Set<String> retainedSheetNames = workbook.getNumberOfSheets() > 0
                        ? IntStream.range(0, workbook.getNumberOfSheets())
                        .mapToObj(workbook::getSheetName)
                        .collect(Collectors.toSet())
                        : Collections.emptySet();

                // 只保留仍然存在的sheet的黄色列信息
                Map<String, List<String>> filteredYellowColumns = existingYellowColumns.entrySet().stream()
                        .filter(entry -> retainedSheetNames.contains(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                existingYellowColumns = filteredYellowColumns;
                log.info("过滤后的黄色高亮列信息（已删除不存在的sheet）: {}", existingYellowColumns);
            }

            // 2. 处理保留的Sheet
            for (Map.Entry<String, Set<String>> entry : sheetRetainRows.entrySet()) {
                Sheet sheet = workbook.getSheet(entry.getKey());
                if (sheet == null) continue;

                // 收集需要保留的行索引（标题行+指定行）
                Set<Integer> rowsToKeep = new HashSet<>(Collections.singleton(0)); // 保留标题行(第0行)

                // 处理合并区域和独立行
                collectRetainedRows(sheet, entry.getValue(), rowsToKeep);

                // 重新构建Sheet（解决行不上移和公式引用问题）
                rebuildSheetWithRetainedRows(sheet, rowsToKeep);
            }
            //获取以 检测类别 分组的数据，检测类别就是sheet名
            Map<String, List<Map<String, Object>>> map = getGroupedData(template.getTemplateCode(), "检测类别", null).getData();

            for (Map.Entry<String, List<Map<String, Object>>> entry : map.entrySet()) {
                String sheetName = entry.getKey();
                Sheet sheet = workbook.getSheet(sheetName);
                List<Map<String, Object>> value = entry.getValue();

                for (Map<String, Object> temp : value) {
                    System.out.println(temp);

                    // 提前获取常用值
                    Object projectNameObj = temp.get(PROJECT_NAME.getDescription());
                    if (projectNameObj == null) {
                        continue;
                    }
                    String projectName = String.valueOf(projectNameObj);

                    // 处理特征值
                    Object featureValueObj = temp.get(CHARACTERISTIC.getDescription());
                    if (featureValueObj != null) {
                        String featureValue = String.valueOf(featureValueObj);
                        updateCellValues(sheet, excelCellLocator, excelCellEditor, CHARACTERISTIC.getDescription(), projectName, featureValue);
                    }

//                    updateCellValues(sheet, excelCellLocator, excelCellEditor, "测量值", projectName, "123");
                    // 处理标准值
                    Object standardValueObj = temp.get(STANDARD.getDescription());
                    if (standardValueObj != null) {
                        String standardValue = String.valueOf(standardValueObj);
                        updateCellValues(sheet, excelCellLocator, excelCellEditor, STANDARD.getDescription(), projectName, standardValue);
                    }
                }
            }

            //新增不存在的检测项赋值
            if (CollectionUtils.isNotEmpty(rowsNotInTemplate)) {
                log.info("新增不存在的检测项赋值: {}", rowsNotInTemplate);
                //需要给这个检测项和标准物来个关联关系，目前没有关联关系
                for (Map<String, Object> row : rowsNotInTemplate) {
                    //找检测类别，获取对应的Sheet
                    String sheetName = MapUtils.getString(row, TEST_CATEGORY.getDescription());
                    Sheet sheet = workbook.getSheet(sheetName);
                    //获取这个sheet页的表头
                    Row headerRow = sheet.getRow(0);
                    //
                    // 创建表头映射
                    Map<String, List<Integer>> headerColumns = parseHeaderStructureWithMergedCells(sheet);
                    /*
                    {项目名称=[0], 判定结果=[10], 平均值（nm）=[5], 波长重复性=[9], 误差值（nm）=[6],
                    特征值（nm）=[1], 重复性（nm）=[7], 测量值（nm）=[2, 3, 4], 波长示值误差(nm)=[8]}
                    */
                    //在最后一行插入新的检测项
                    int lastRow = sheet.getLastRowNum();
                    Map<Integer, List<Object>> columnData = this.buildColumnData(row, headerColumns);
                    log.info("列数据: {}", columnData);
                    System.out.println("855555555555555555555555555" + row);
                    excelDynamicMerger.insertDynamicMergedRow(workbook, sheetName, lastRow + 1, columnData);
                    log.info("表头映射: {}", headerColumns);
                }
            }
            processWorkbookCells(workbook);

            // 返回编辑后的文件
            //生成新模板入库，使用过滤后的黄色高亮列信息
            boolean b = workbookDataService.uploadExcel(workbook, template.getTemplateCode(), existingYellowColumns);
            if (b) {
                log.info("模板更新成功，黄色高亮列信息: {}", existingYellowColumns);
            }
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            workbook.write(out);
//            String excelPath = excelSavePath + template.getTemplateCode() + ".xlsx";
//
//            File excelFile = new File(excelPath);
//            File excelFile.getParentFile().mkdirs(); // 创建父目录（如果不存在）
//
//            try (FileOutputStream fos = new FileOutputStream(excelPath)) {
//                out.writeTo(fos);  // 将输出流内容写入文件
//            }
//            out.close();
            return Result.ok();
        } catch (Exception e) {
            log.error("文件处理失败", e);
        } finally {
            IOUtils.closeQuietly(workbook);
        }
        return Result.fail("文件处理失败");
    }
// ... existing code ...





    private Map<Integer, List<Object>> buildColumnData(Map<String, Object> row, Map<String, List<Integer>> headerColumns) {
        if (MapUtils.isEmpty(headerColumns)) {
            return new HashMap<>();
        }
// 获取字符串列表
        List<String> stringList = row.values().stream()
                .map(Object::toString)
                .collect(Collectors.toList());

// 找出最多逗号数
        int maxCommaCount = stringList.stream()
                .mapToInt(str -> (int) str.chars().filter(ch -> ch == ',').count())
                .max()
                .orElse(0);

        Map<Integer, List<Object>> data = new HashMap<>();

        for (Map.Entry<String, List<Integer>> entry : headerColumns.entrySet()) {
            String headerName = entry.getKey();
            List<Integer> columnIndices = entry.getValue();
            int rowNum = 1;
            // 从 row 中获取对应的值，测量值就是空，所以就要赋空，行数按照最大行，也就是特征值数
//            Object cellValueObj = row.get(headerName);
            Object cellValueObj = findFlexibleValue(row, headerName);
            if (Objects.isNull(cellValueObj)) {
                for (Integer columnIndex : columnIndices) {
                    if (maxCommaCount > 0) {
                        rowNum = maxCommaCount + 1;
                    }
                    //最大有几行
                    data.put(columnIndex, Collections.nCopies(rowNum, ""));
                }
            }
            if (!Objects.isNull(cellValueObj)) {
                // 处理值，将其转换为 List<Object>
                List<Object> cellValues = convertToValueList(cellValueObj);
                log.info("特征值设置: {}", cellValues);
                // 为每个列索引设置对应的值列表
                for (Integer columnIndex : columnIndices) {
                    data.put(columnIndex, cellValues);
                }
            }
        }

        return data;
    }

    /**
     * 从输入的行中，根据给定的列名，尝试从行中获取最匹配的值。
     *
     * @param row
     * @param headerName
     * @return
     */
    private Object findFlexibleValue(Map<String, Object> row, String headerName) {
        // 1. 精确匹配
        if (row.containsKey(headerName)) {
            return row.get(headerName);
        }

        // 2. 移除后缀后匹配
        String baseName = extractBaseName(headerName);
        if (baseName != null) {
            // 先尝试直接匹配基础名称
            if (row.containsKey(baseName)) {
                return row.get(baseName);
            }

            // 再尝试反向匹配（row中的键可能有后缀）
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String rowKey = entry.getKey();
                String rowBaseName = extractBaseName(rowKey);
                if (baseName.equals(rowBaseName)) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    private String extractBaseName(String key) {
        if (key == null) return null;
        // 移除括号后缀，支持英文()和中文（）括号
        return key.replaceAll("\\s*\\([^)]*\\)\\s*$", "")
                .replaceAll("\\s*（[^）]*）\\s*$", "")
                .trim();
    }

    /**
     * 将对象值转换为 List<Object>
     */
    private List<Object> convertToValueList(Object value) {
        if (value == null) {
            return Collections.singletonList("");
        }

        String stringValue = value.toString().trim();

        if (stringValue.contains(",")) {
            // 按逗号分割字符串
            return Arrays.stream(stringValue.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        } else {
            // 单个值
            return Collections.singletonList(stringValue);
        }
    }

    /*private Map<Integer, List<Object>> buildColumnData(Map<String, Object> row, Map<String, List<Integer>> headerColumns) {
        if (MapUtils.isNotEmpty(headerColumns)){
            int size = headerColumns.values().size();
            //搞一个新的行
            Map<Integer, List<Object>> data = new HashMap<>(size);
            for (Map.Entry<String, List<Integer>> entry : headerColumns.entrySet()) {
                String key = entry.getKey();
                //这一步获取到的是单元格要填充的值
                String cellValue = MapUtils.getString(row, key);
                //这是列的索引
                List<Integer> value = entry.getValue();
                if (CollectionUtils.isNotEmpty(value)) {
                    //一般是一次循环，有的是测量值测量多次，会有三次
                    for (Integer i : value) {
                        data.put(i,Arrays.asList(value));
                    }
                }
            }
        }

        // 项目名称列 (跨6行合并)
        data.put(0, Collections.singletonList("9595"));

        // 特征值列 (6个字符串)
        data.put(1, Arrays.asList("222.7", "333.8", "444.5", "555.0", "666.4", "777.0"));

        // 空列处理 (使用nCopies保证行数一致)
        IntStream.rangeClosed(2, 7).forEach(i ->
                data.put(i, Collections.nCopies(6, ""))
        );

        // 动态转换数值类型
        data.put(8, Arrays.asList(3, 4, 5, 6, 7, 3).stream()
                .map(Object::toString)
                .collect(Collectors.toList()));
        data.put(9, Arrays.asList(2, 2, 2, 2, 2, 2).stream()
                .map(Object::toString)
                .collect(Collectors.toList()));
// 空列处理 (使用nCopies保证行数一致)
        data.put(10, Collections.nCopies(6, ""));
        return data;
    }*/

    /**
     * 解析表头结构，包含合并单元格处理
     */
    private Map<String, List<Integer>> parseHeaderStructureWithMergedCells(Sheet sheet) {
        Map<String, List<Integer>> headerColumns = new HashMap<>();
        Row headerRow = sheet.getRow(0);

        if (headerRow == null) return headerColumns;

        // 首先处理合并区域
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress mergedRegion = sheet.getMergedRegion(i);

            // 检查是否为表头行的合并区域
            if (mergedRegion.getFirstRow() == 0) { // 表头行
                String headerName = getCellValueAsString(
                        headerRow.getCell(mergedRegion.getFirstColumn())
                );

                // 将合并区域的所有列都映射到该表头名称
                for (int col = mergedRegion.getFirstColumn();
                     col <= mergedRegion.getLastColumn(); col++) {
                    headerColumns.computeIfAbsent(headerName, k -> new ArrayList<>()).add(col);
                }
            }
        }

        // 处理非合并单元格
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            // 检查当前列是否已在合并区域中处理过
            boolean isMerged = false;
            for (int j = 0; j < sheet.getNumMergedRegions(); j++) {
                CellRangeAddress merged = sheet.getMergedRegion(j);
                if (merged.getFirstRow() == 0 &&
                        i >= merged.getFirstColumn() && i <= merged.getLastColumn()) {
                    isMerged = true;
                    break;
                }
            }

            if (!isMerged) {
                Cell cell = headerRow.getCell(i);
                String headerName = getCellValueAsString(cell);
                if (!headerName.isEmpty()) { // 只处理非空表头
                    headerColumns.computeIfAbsent(headerName, k -> new ArrayList<>()).add(i);
                }
            }
        }

        return headerColumns;
    }

    /**
     * 编辑Excel文件
     * 入参的话
     * valueType 是表头
     * projectName  是项目名称
     * cellValue 是这个单元格你要改成啥名字
     * 入参
     *
     * @return
     * @throws IOException
     */
    public void updateCellValues(Sheet sheet, ExcelCellLocator excelCellLocator,
                                 ExcelCellEditor excelCellEditor, String valueType,
                                 String projectName, String cellValue) {
        List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, valueType, projectName);
        if (cellValue.contains(",")) {
            String[] values = cellValue.split(",");
            for (int i = 0; i < positions.size() && i < values.length; i++) {
                ExcelCellLocator.CellPosition position = positions.get(i);
                if (StringUtils.isBlank(values[i])) {
                    continue;
                }
                excelCellEditor.setCellValue(sheet, position.getRow(), position.getCol(), values[i].trim());
            }
        } else {
            for (ExcelCellLocator.CellPosition position : positions) {
                excelCellEditor.setCellValue(sheet, position.getRow(), position.getCol(), cellValue);
            }
        }
    }

    // 设置单个值到指定位置0,1,2
    public void updateCellValue(Sheet sheet, ExcelCellLocator excelCellLocator,
                                ExcelCellEditor excelCellEditor, String valueType,
                                String projectName, int positionIndex, String cellValue) {
        List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, valueType, projectName);

        if (positionIndex >= 0 && positionIndex < positions.size() &&
                cellValue != null && !cellValue.isEmpty()) {
            ExcelCellLocator.CellPosition position = positions.get(positionIndex);
            excelCellEditor.setCellValue(sheet, position.getRow(), position.getCol(), cellValue);
        }
    }

    @Override
    /**
     * 更新单元格值 - 支持通过项目名称和特征值定位
     *
     * @param sheet Excel工作表
     * @param excelCellLocator 单元格定位器
     * @param excelCellEditor 单元格编辑器
     * @param valueType 表头名称
     * @param projectName 项目名称（第一列）
     * @param featureValue 特征值（第二列）
     * @param cellValue 要设置的值
     */
    public void updateCellValue(Sheet sheet, ExcelCellLocator excelCellLocator,
                                ExcelCellEditor excelCellEditor, String valueType,
                                String projectName, String featureValue, String cellValue, int positionIndex) {
        // 查找匹配的单元格位置
        List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(
                sheet, valueType, projectName, featureValue);

        if (positions.isEmpty()) {
            log.warn("未找到匹配的位置: {} - {} - {}", valueType, projectName, featureValue);
            return;
        }

        // 设置第一个匹配的位置
        ExcelCellLocator.CellPosition position = positions.get(positionIndex);
        excelCellEditor.setCellValue(sheet, position.getRow(), position.getCol(), cellValue);
    }

    @Override
    public Result<String> deleteTemplate(String templateCode) {
        ExcelTemplate template = this.getOne(new LambdaQueryWrapper<ExcelTemplate>().eq(ExcelTemplate::getTemplateCode, templateCode));
        TaskManagement one = taskManagementService.getOne(new LambdaQueryWrapper<TaskManagement>().eq(TaskManagement::getUseTemplate, template.getId()));
        if (one != null) {
            return Result.fail("此模板正在使用中，请先删除任务！！！");
        }

        LambdaQueryWrapper<ExcelTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExcelTemplate::getTemplateCode, templateCode)
                .notIn(ExcelTemplate::getCategory, TEMPLATE_BUILT_IN.getCode());
        int i = templateMapper.delete(queryWrapper);
        boolean b = i != 0;
        if (b) {
            LambdaQueryWrapper<ExcelDataAsset> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ExcelDataAsset::getTemplateCode, templateCode);
            b = excelDataAssetService.remove(wrapper);
//            Path path = Paths.get(excelSavePath + templateCode + ".xlsx");
//            try {
//                Files.delete(path);
//            } catch (IOException e) {
//                log.error("文件删除失败", e);
//            }
            //这里是删除模板，calibration_workbook_data表中，模板code和taskid相同的就是模板
            //不同的就是各自任务所属的workbook
            LambdaQueryWrapper<WorkbookData> wrapper1 = new LambdaQueryWrapper<>();
            wrapper1.eq(WorkbookData::getTaskId, templateCode);
            boolean remove = workbookDataService.remove(wrapper1);
            if (remove) {
                log.info("删除模板成功");
            }
        }
        return b ? Result.ok() : Result.fail("内置模板，不可删除");
    }

    @Override
    public Result<String> batchDeleteTemplate(List<String> list) {
        List<String> inUseTemplates = new ArrayList<>();
        List<String> builtInTemplates = new ArrayList<>();

        for (String templateCode : list) {
            ExcelTemplate template = this.getOne(new LambdaQueryWrapper<ExcelTemplate>().eq(ExcelTemplate::getTemplateCode, templateCode));
            if (template == null) {
                continue;
            }

            TaskManagement one = taskManagementService.getOne(new LambdaQueryWrapper<TaskManagement>().eq(TaskManagement::getUseTemplate, template.getId()));
            if (one != null) {
                inUseTemplates.add(templateCode);
            }

            if (template.getCategory() != null && template.getCategory().equals("0")) {
                builtInTemplates.add(templateCode);
            }
        }

        if (!inUseTemplates.isEmpty() || !builtInTemplates.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("以下模板无法删除：");
            if (!inUseTemplates.isEmpty()) {
                errorMsg.append("\n正在使用中：").append(String.join(", ", inUseTemplates));
            }
            if (!builtInTemplates.isEmpty()) {
                errorMsg.append("\n内置模板：").append(String.join(", ", builtInTemplates));
            }
            return Result.fail(errorMsg.toString());
        }

        LambdaQueryWrapper<ExcelTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ExcelTemplate::getTemplateCode, list);

        boolean b = this.remove(wrapper);
        return b ? Result.ok("删除成功") : Result.fail("删除失败");
    }


    @Override
    public Result<List<Map<String, String>>> getMaterialOptions(String id) {
//        if (StringUtils.isBlank(id)){
        log.info("选择标准物质");
        List<CalStdCats> list = calStdCatsService.list();
        if (CollectionUtils.isNotEmpty(list)) {
            List<Map<String, String>> collect = list.stream().map(c -> {
                Map<String, String> map = new HashMap<>();
                map.put("id", c.getId());
                map.put("name", c.getName());
                map.put("characteristicValue", c.getCharacteristicValue());
                map.put("standardValue", c.getStandardValue());
                return map;
            }).collect(Collectors.toList());
            return Result.ok(collect);
        }
        return Result.ok();
//        }
//        CalStdCats cats = calStdCatsService.getById(id);
//        if (cats != null) {
//            List<CalArtifacts> list = calArtifactsService.list(new LambdaQueryWrapper<CalArtifacts>().eq(CalArtifacts::getCategoryId, cats.getId()));
//            if (CollectionUtils.isNotEmpty(list)) {
//                List<Map<String, String>> collect = list.stream().map(c -> {
//                    Map<String, String> map = new HashMap<>();
//                    map.put("id", c.getId());
//                    map.put("name", c.getName());
//                    map.put("characteristicValue", c.getCharacteristicValue());
//                    map.put("standardValue", c.getStandardValue());
//                    return map;
//                }).collect(Collectors.toList());
//                Map<String, String> m = new HashMap<>();
//                m.put("id", cats.getId());
//                m.put("name", cats.getName());
//                m.put("characteristicValue", cats.getCharacteristicValue());
//                m.put("standardValue", cats.getStandardValue());
//                collect.add(m);
//                return Result.ok(collect);
//            }
//            if (CollectionUtils.isEmpty(list)) {
//                ArrayList<Map<String, String>> maps = new ArrayList<>();
//                HashMap<String, String> map = new HashMap<>();
//                map.put("id", cats.getId());
//                map.put("name", cats.getName());
//                map.put("characteristicValue", cats.getCharacteristicValue());
//                map.put("standardValue", cats.getStandardValue());
//                maps.add(map);
//                return Result.ok(maps);
//            }
//        }
//        if (cats == null) {
//            CalArtifacts calArtifacts = calArtifactsService.getById(id);
//            if (calArtifacts != null) {
//                ArrayList<Map<String, String>> maps = new ArrayList<>();
//                HashMap<String, String> map = new HashMap<>();
//                map.put("id", calArtifacts.getId());
//                map.put("name", calArtifacts.getName());
//                map.put("characteristicValue", calArtifacts.getCharacteristicValue());
//                map.put("standardValue", calArtifacts.getStandardValue());
//                maps.add(map);
//                LambdaQueryWrapper<CalArtifacts> wrapper = new LambdaQueryWrapper<>();
//                wrapper.eq(CalArtifacts::getCategoryId, calArtifacts.getCategoryId());
//                List<CalArtifacts> list = calArtifactsService.list(wrapper);
//                if (CollectionUtils.isNotEmpty(list)) {
//                    for (CalArtifacts artifact : list) {
//                        HashMap<String, String> temp = new HashMap<>();
//                        temp.put("id", artifact.getId());
//                        temp.put("name", artifact.getName());
//                        temp.put("characteristicValue", artifact.getCharacteristicValue());
//                        temp.put("standardValue", artifact.getStandardValue());
//                        maps.add(temp);
//                    }
//                }
//                return Result.ok(maps);
//            }
//        }
//        return Result.fail(null);
    }

    @Override
    public Result editExcel(MultipartFile file, Map<String, Set<String>> sheetRetainRows) throws IOException {
        InputStream is = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(is);

        try {
            // 1. 删除未指定的Sheet
            List<String> existingSheets = IntStream.range(0, workbook.getNumberOfSheets())
                    .mapToObj(workbook::getSheetName)
                    .collect(Collectors.toList());

            existingSheets.stream()
                    .filter(sheetName -> !sheetRetainRows.containsKey(sheetName))
                    .forEach(sheetName -> workbook.removeSheetAt(workbook.getSheetIndex(sheetName)));

            // 2. 处理保留的Sheet
            for (Map.Entry<String, Set<String>> entry : sheetRetainRows.entrySet()) {
                Sheet sheet = workbook.getSheet(entry.getKey());
                if (sheet == null) continue;

                // 收集需要保留的行索引（标题行+指定行）
                Set<Integer> rowsToKeep = new HashSet<>(Collections.singleton(0)); // 保留标题行(第0行)

                // 处理合并区域和独立行
                collectRetainedRows(sheet, entry.getValue(), rowsToKeep);

                // 重新构建Sheet（解决行不上移和公式引用问题）
                rebuildSheetWithRetainedRows(sheet, rowsToKeep);
            }

            processWorkbookCells(workbook);
//            workbook.setForceFormulaRecalculation(true);
            // 返回编辑后的文件
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            // 获取项目资源目录路径
            String s = IdGeneratorUtil.generateId();
            String excelPath = /*excelSavePath + */"excel" + s + ".xlsx";
//            String xmlPath = xmlSavePath + "xml" + s + ".xml";

            File excelFile = new File(excelPath);
            excelFile.getParentFile().mkdirs(); // 创建父目录（如果不存在）


            try (FileOutputStream fos = new FileOutputStream(excelPath)) {
                out.writeTo(fos);  // 将输出流内容写入文件
            }
            out.close();


            return Result.ok();

        } catch (Exception e) {
            System.out.println(e);
        } finally {
            IOUtils.closeQuietly(workbook);
        }
        return Result.fail("文件处理失败");
    }

    // ... existing code ...
    @Override
    public Result buildResultTemplate(String templateCode, List<Map<String, Object>> rowsNotInTemplate) {
        try {
            //找出新模板的数据
            LambdaQueryWrapper<ExcelDataAsset> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ExcelDataAsset::getTemplateCode, templateCode);
            List<ExcelDataAsset> list = excelDataAssetService.list(wrapper);

            ArrayList<Map<String, Object>> hashMaps = new ArrayList<>();
            // 将JSON字符串转换为Map
            for (ExcelDataAsset asset : list) {
                String structuredData = asset.getStructuredData();
                if (structuredData != null && !structuredData.isEmpty()) {
                    Map<String, Object> dataMap = JSON.parseObject(structuredData, Map.class);
                    hashMaps.add(dataMap);
                }
            }
            // 结构化成结果模板  需要哪些sheet以及哪些检测项目
            Map<String, Set<String>> sheetRetainRows = hashMaps.stream()
                    .filter(map -> map.containsKey(TEST_CATEGORY.getDescription()) && map.containsKey(PROJECT_NAME.getDescription()))
                    .collect(Collectors.groupingBy(
                            map -> map.get(TEST_CATEGORY.getDescription()).toString(),
                            Collectors.mapping(map -> map.get(PROJECT_NAME.getDescription()).toString(), Collectors.toSet())
                    ));

            LambdaQueryWrapper<ExcelTemplate> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ExcelTemplate::getTemplateCode, templateCode);
            ExcelTemplate template = this.getOne(queryWrapper);

            // 【修复】先检查模板是否存在
            if (template == null) {
                log.error("模板不存在，templateCode: {}", templateCode);
                return Result.fail("模板不存在");
            }

            // 【新增】如果是子模板，先从父模板复制黄色高亮列信息
            if (StringUtils.isNotBlank(template.getParentId())) {
                log.info("检测到子模板，开始从父模板{}复制黄色高亮列信息", template.getParentId());
                copyYellowColumnsFromParent(template.getParentId(), templateCode);
            }

            // 直接加载当前模板的WorkbookData（每个模板创建时都有完整的初始数据）
            LambdaQueryWrapper<WorkbookData> workbookQueryWrapper = new LambdaQueryWrapper<>();
            workbookQueryWrapper.eq(WorkbookData::getTaskId, templateCode)
                    .orderByDesc(WorkbookData::getCreateTime)
                    .last("LIMIT 1");

            WorkbookData workbookData = workbookDataService.getOne(workbookQueryWrapper);

            if (workbookData == null || workbookData.getWorkbookData() == null || workbookData.getWorkbookData().length == 0) {
                log.error("模板{}没有有效的WorkbookData", templateCode);
                return Result.fail("模板数据不存在");
            }

            // 从字节数组创建workbook
            Workbook workbook;
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(workbookData.getWorkbookData())) {
                workbook = new XSSFWorkbook(inputStream);
                log.info("成功加载模板{}的WorkbookData，大小: {} bytes，黄色高亮列: {}",
                        templateCode, workbookData.getWorkbookData().length, workbookData.getYellowHighlightedColumns());
            } catch (IOException e) {
                log.error("转换字节数组到Workbook失败，templateCode: {}", templateCode, e);
                return Result.fail("模板数据解析失败");
            }

            // 调用editExcel进行过滤和编辑
            return editExcel(workbook, template, sheetRetainRows, rowsNotInTemplate);

        } catch (Exception e) {
            log.error("构建结果模板失败", e);
            return Result.fail("文件处理失败: " + e.getMessage());
        }
    }


    /**
     * 从父模板复制黄色高亮列信息到子模板（只保留子模板中存在的sheet页）
     *
     * @param parentTemplateCode 父模板编码
     * @param childTemplateCode 子模板编码
     */
    private void copyYellowColumnsFromParent(String parentTemplateCode, String childTemplateCode) {
        try {
            // 查询父模板的WorkbookData
            LambdaQueryWrapper<WorkbookData> parentQueryWrapper = new LambdaQueryWrapper<>();
            parentQueryWrapper.eq(WorkbookData::getTaskId, parentTemplateCode)
                    .orderByDesc(WorkbookData::getCreateTime)
                    .last("LIMIT 1");

            WorkbookData parentWorkbookData = workbookDataService.getOne(parentQueryWrapper);

            if (parentWorkbookData == null || StringUtils.isBlank(parentWorkbookData.getYellowHighlightedColumns())) {
                log.warn("父模板{}没有黄色高亮列信息", parentTemplateCode);
                return;
            }

            // 解析父模板的黄色高亮列信息
            ObjectMapper mapper = new ObjectMapper();
            Map<String, List<String>> parentYellowColumns = mapper.readValue(
                    parentWorkbookData.getYellowHighlightedColumns(),
                    new TypeReference<Map<String, List<String>>>(){});

            if (parentYellowColumns == null || parentYellowColumns.isEmpty()) {
                log.warn("父模板{}的黄色高亮列信息为空", parentTemplateCode);
                return;
            }

            // 加载子模板的workbook，获取实际存在的sheet页
            LambdaQueryWrapper<WorkbookData> childQueryWrapper = new LambdaQueryWrapper<>();
            childQueryWrapper.eq(WorkbookData::getTaskId, childTemplateCode)
                    .orderByDesc(WorkbookData::getCreateTime)
                    .last("LIMIT 1");

            WorkbookData childWorkbookData = workbookDataService.getOne(childQueryWrapper);

            if (childWorkbookData == null || childWorkbookData.getWorkbookData() == null) {
                log.warn("子模板{}没有WorkbookData记录", childTemplateCode);
                return;
            }

            // 从字节数组创建workbook，获取实际的sheet列表
            Set<String> childSheetNames;
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(childWorkbookData.getWorkbookData())) {
                Workbook childWorkbook = new XSSFWorkbook(inputStream);
                childSheetNames = IntStream.range(0, childWorkbook.getNumberOfSheets())
                        .mapToObj(childWorkbook::getSheetName)
                        .collect(Collectors.toSet());
                log.info("子模板{}包含的sheet页: {}", childTemplateCode, childSheetNames);
            } catch (IOException e) {
                log.error("加载子模板workbook失败", e);
                return;
            }

            // 过滤：只保留子模板中存在的sheet页的黄色列信息
            Map<String, List<String>> filteredYellowColumns = parentYellowColumns.entrySet().stream()
                    .filter(entry -> childSheetNames.contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (filteredYellowColumns.isEmpty()) {
                log.info("父模板的黄色高亮列对应的sheet页在子模板中都不存在，不复制");
                return;
            }

            // 将过滤后的黄色列信息转换为JSON
            String filteredYellowColumnsJson = mapper.writeValueAsString(filteredYellowColumns);

            // 更新子模板的WorkbookData
            childWorkbookData.setYellowHighlightedColumns(filteredYellowColumnsJson);
            childWorkbookData.setUpdateTime(LocalDateTime.now());

            boolean updated = workbookDataService.updateById(childWorkbookData);

            if (updated) {
                log.info("成功从父模板{}复制黄色高亮列信息到子模板{}，过滤前: {}个sheet，过滤后: {}个sheet",
                        parentTemplateCode, childTemplateCode, parentYellowColumns.size(), filteredYellowColumns.size());
            } else {
                log.error("复制黄色高亮列信息失败");
            }

        } catch (Exception e) {
            log.error("复制黄色高亮列信息异常，parentTemplateCode: {}, childTemplateCode: {}",
                    parentTemplateCode, childTemplateCode, e);
        }
    }

    /**
     * 处理工作簿中的所有单元格：
     * 有值的单元格保持不变
     * 公式单元格最后计算
     */
    private void processWorkbookCells(Workbook workbook) {
        // 第二步：处理公式单元格并计算
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);

            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.FORMULA) {
                        try {
                            // 计算公式结果
                            CellValue cellValue = evaluator.evaluate(cell);
                            if (cellValue != null) {
                                switch (cellValue.getCellType()) {
                                    case NUMERIC:
                                        cell.setCellValue(cellValue.getNumberValue());
                                        break;
                                    case STRING:
                                        cell.setCellValue(cellValue.getStringValue());
                                        break;
                                    case BOOLEAN:
                                        cell.setCellValue(cellValue.getBooleanValue());
                                        break;
                                    case BLANK:
                                        cell.setCellValue("");
                                        break;
                                    default:
                                        cell.setCellValue("");
                                        break;
                                }
                            } else {
                                cell.setCellValue("");
                            }
                        } catch (Exception e) {
                            // 公式计算异常时设置空字符串
                            cell.setCellValue("");
                        }
                    }
                }
            }
        }
    }


    /**
     * 收集需要保留的行号（合并区域+独立行）
     */
    private void collectRetainedRows(Sheet sheet, Set<String> retainNames, Set<Integer> rowsToKeep) {
        // 处理合并区域
        Map<String, List<Integer>> mergedRegionsMap = getMergedRegionMap(sheet);
        retainNames.forEach(name -> {
            if (mergedRegionsMap.containsKey(name)) {
                rowsToKeep.addAll(mergedRegionsMap.get(name));
            }
        });

        // 处理独立行（跳过标题行和已处理的合并行）
        Set<Integer> mergedRows = mergedRegionsMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            if (mergedRows.contains(i)) continue;

            Row row = sheet.getRow(i);
            if (row == null) continue;

            String rowValue = getCellValueAsString(row.getCell(0));
            if (retainNames.contains(rowValue)) {
                rowsToKeep.add(i);
            }
        }
    }

    /**
     * 核心修复：重新构建Sheet，确保行上移和公式正确
     */
    private void rebuildSheetWithRetainedRows(Sheet originalSheet, Set<Integer> rowsToKeep) {
        Workbook workbook = originalSheet.getWorkbook();
        String sheetName = originalSheet.getSheetName();

        // 创建临时Sheet来存储保留的行
        Sheet tempSheet = workbook.createSheet("TEMP_" + System.currentTimeMillis());

        // 按顺序复制保留的行到临时Sheet
        List<Integer> sortedRows = rowsToKeep.stream()
                .sorted()
                .collect(Collectors.toList());

        for (int newIndex = 0; newIndex < sortedRows.size(); newIndex++) {
            int originalIndex = sortedRows.get(newIndex);
            Row originalRow = originalSheet.getRow(originalIndex);
            if (originalRow == null) continue;

            // 创建新行
            Row newRow = tempSheet.createRow(newIndex);

            // 复制行高和样式
            newRow.setHeightInPoints(originalRow.getHeightInPoints());

            // 复制所有单元格（包括公式）
            copyRowWithFormulas(originalRow, newRow);
        }

        // 复制合并区域（调整行号）
        copyAdjustedMergedRegions(originalSheet, tempSheet, sortedRows);

        // 删除原Sheet并重命名临时Sheet
        int sheetIndex = workbook.getSheetIndex(originalSheet);
        workbook.removeSheetAt(sheetIndex);
        workbook.setSheetName(workbook.getSheetIndex(tempSheet), sheetName);
    }

    /**
     * 复制行并保持公式完整性
     */
    private void copyRowWithFormulas(Row sourceRow, Row targetRow) {
        try {
            for (int i = 0; i <= sourceRow.getLastCellNum(); i++) {
                Cell sourceCell = sourceRow.getCell(i);
                if (sourceCell == null) continue;

                Cell targetCell = targetRow.createCell(i);

                // 复制单元格样式
                CellStyle newStyle = targetRow.getSheet().getWorkbook().createCellStyle();
                newStyle.cloneStyleFrom(sourceCell.getCellStyle());
                targetCell.setCellStyle(newStyle);

                // 根据单元格类型复制内容
                switch (sourceCell.getCellType()) {
                    case FORMULA:
                        // 直接复制公式字符串，保持公式引用
                        //调整公式到合适的位置
                        String formula = adjustFormulaRowReferences(sourceCell.getCellFormula(), sourceCell.getRowIndex(), targetCell.getRowIndex());
                        targetCell.setCellFormula(formula);
                        break;
                    case STRING:
                        targetCell.setCellValue(sourceCell.getStringCellValue());
                        break;
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(sourceCell)) {
                            targetCell.setCellValue(sourceCell.getDateCellValue());
                        } else {
                            targetCell.setCellValue(sourceCell.getNumericCellValue());
                        }
                        break;
                    case BOOLEAN:
                        targetCell.setCellValue(sourceCell.getBooleanCellValue());
                        break;
                    case BLANK:
                        targetCell.setBlank();
                        break;
                    default:
                        targetCell.setCellValue(getCellValueAsString(sourceCell));
                }
            }
        } catch (Exception e) {
            System.out.println(sourceRow.toString());
            System.out.println("777777777777777" + e.toString());
        }
    }

    private String adjustFormulaRowReferences(String formula, int sourceRowNum, int targetRowNum) {
        int rowOffset = targetRowNum - sourceRowNum;

        // 匹配单元格引用模式（支持绝对引用和相对引用）
        Pattern cellRefPattern = Pattern.compile("([$]?[A-Z]+[$]?)(\\d+)");

        Matcher matcher = cellRefPattern.matcher(formula);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String columnPart = matcher.group(1); // 可能包含$符号
            String rowPart = matcher.group(2);

            try {
                int originalRow = Integer.parseInt(rowPart);
                // 调整行号
                int newRow = originalRow + rowOffset;
                matcher.appendReplacement(result, columnPart + newRow);
            } catch (NumberFormatException e) {
                matcher.appendReplacement(result, matcher.group(0));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 复制并调整合并区域的行号映射
     */
    private void copyAdjustedMergedRegions(Sheet sourceSheet, Sheet targetSheet, List<Integer> rowMapping) {
        Map<Integer, Integer> originalToNewRowMap = new HashMap<>();
        for (int i = 0; i < rowMapping.size(); i++) {
            originalToNewRowMap.put(rowMapping.get(i), i);
        }

        for (CellRangeAddress originalRegion : sourceSheet.getMergedRegions()) {
            Integer newFirstRow = originalToNewRowMap.get(originalRegion.getFirstRow());
            Integer newLastRow = originalToNewRowMap.get(originalRegion.getLastRow());

            if (newFirstRow != null && newLastRow != null) {
                CellRangeAddress newRegion = new CellRangeAddress(
                        newFirstRow, newLastRow,
                        originalRegion.getFirstColumn(), originalRegion.getLastColumn()
                );
                targetSheet.addMergedRegion(newRegion);
            }
        }
    }

    /**
     * 获取合并区域映射
     */
    private Map<String, List<Integer>> getMergedRegionMap(Sheet sheet) {
        Map<String, List<Integer>> regionMap = new HashMap<>();

        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.getFirstColumn() != 0 || region.getLastColumn() != 0) continue;

            Row firstRow = sheet.getRow(region.getFirstRow());
            Cell nameCell = (firstRow != null) ? firstRow.getCell(0) : null;
            String projectName = (nameCell != null) ? getCellValueAsString(nameCell) : "";

            if (!projectName.isEmpty()) {
                List<Integer> rows = IntStream.rangeClosed(region.getFirstRow(), region.getLastRow())
                        .boxed()
                        .collect(Collectors.toList());
                regionMap.put(projectName, rows);
            }
        }
        return regionMap;
    }

    /**
     * 安全的单元格值读取
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        try {
            return new DataFormatter().formatCellValue(cell);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getCustomStructuredData(String templateCode, List<String> fields) {
        // 先获取全部结构化数据
        Result<List<Map<String, Object>>> allDataResult = getStructuredData(templateCode);

        if (allDataResult.getCode() != 200) {
            return Result.fail(allDataResult.getMessage());
        }

        List<Map<String, Object>> filteredData = allDataResult.getData().stream()
                .map(row -> row.entrySet().stream()
                        .filter(entry -> fields.contains(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .collect(Collectors.toList());

        return Result.ok(filteredData);
    }

    @Override
    public Result<Page<TemplateResponseDto>> getTemplateList(TemplateQeqDto templateQeqDto) {
        Page<ExcelTemplate> page = new Page<>(templateQeqDto.getPageNum(), templateQeqDto.getPageSize());
        LambdaQueryWrapper<ExcelTemplate> queryWrapper = new LambdaQueryWrapper<>();
        if ("asc".equals(templateQeqDto.getSort())) {
            queryWrapper.like(ExcelTemplate::getTemplateName, templateQeqDto.getQuery())
                    .and(wrapper -> wrapper
                            .like(ExcelTemplate::getCreateBy, templateQeqDto.getUpdateBy())
                            .or()
                            .isNull(ExcelTemplate::getCreateBy)
                    )
                    .orderByAsc(ExcelTemplate::getLastUsed);
        } else {
            queryWrapper.like(ExcelTemplate::getTemplateName, templateQeqDto.getQuery())
                    .and(wrapper -> wrapper
                            .like(ExcelTemplate::getCreateBy, templateQeqDto.getUpdateBy())
                            .or()
                            .isNull(ExcelTemplate::getCreateBy)
                    )
                    .orderByDesc(ExcelTemplate::getLastUsed);
        }
        // 获取分页结果
        Page<ExcelTemplate> pageResult = this.page(page, queryWrapper);
        // 转换为TemplateResponseDto
        Page<TemplateResponseDto> dtoPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        List<TemplateResponseDto> dtoList = pageResult.getRecords().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        return Result.ok(dtoPage);
    }

    /**
     * 将ExcelTemplate转换为TemplateResponseDto
     */
    private TemplateResponseDto convertToDto(ExcelTemplate template) {
        TemplateResponseDto dto = new TemplateResponseDto();
        dto.setTemplateCode(template.getTemplateCode());
        dto.setTemplateName(template.getTemplateName());
        dto.setCategory(template.getCategory());
        dto.setLastUsed(template.getLastUsed());
        dto.setRemark(template.getRemark());
        dto.setUseCount(template.getUseCount());
        return dto;
    }

    @Override
    public Result exportExcel(String templateCode, HttpServletResponse response) {
        try {
            // 查询模板
            QueryWrapper<ExcelTemplate> templateQuery = new QueryWrapper<>();
            templateQuery.eq("template_code", templateCode);
            ExcelTemplate template = getOne(templateQuery);
            if (template == null) {
                return Result.fail("模板不存在");
            }

            // 查询数据
            // 查询数据时按 batchNo 排序
            QueryWrapper<ExcelDataAsset> dataQuery = new QueryWrapper<>();
            dataQuery.eq("template_code", templateCode);
            dataQuery.orderBy(true, true, "CAST(batch_no AS UNSIGNED)");
            List<ExcelDataAsset> dataList = excelDataAssetService.list(dataQuery);
            if (dataList.isEmpty()) {
                return Result.fail("没有数据可以导出");
            }

            // 获取列配置
            List<ExcelTemplate.ColumnConfig> columnConfigs = template.getColumnConfigList();
            if (columnConfigs == null || columnConfigs.isEmpty()) {
                return Result.fail("模板列配置为空");
            }

            // 构建表头
            List<List<String>> headList = new ArrayList<>();
            for (ExcelTemplate.ColumnConfig config : columnConfigs) {
                List<String> head = new ArrayList<>();
                head.add(config.getExcelHeader());
                headList.add(head);
            }

            // 构建数据
            List<List<Object>> dataRows = new ArrayList<>();
            for (ExcelDataAsset asset : dataList) {
                Map<String, Object> dataMap = asset.getDataMap();
                List<Object> row = new ArrayList<>();
                for (ExcelTemplate.ColumnConfig config : columnConfigs) {
                    row.add(dataMap.get(config.getExcelHeader()));
                }
                dataRows.add(row);
            }

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode(template.getTemplateCode(), "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            // 写入Excel
            ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).head(headList).build();
            WriteSheet writeSheet = EasyExcel.writerSheet("数据").build();
            excelWriter.write(dataRows, writeSheet);
            excelWriter.finish();

            return Result.ok("导出成功");
        } catch (Exception e) {
            log.error("导出Excel失败", e);
            return Result.fail("导出失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getStructuredData(String templateCode) {
        try {
            // 查询模板
            QueryWrapper<ExcelTemplate> templateQuery = new QueryWrapper<>();
            templateQuery.eq("template_code", templateCode);
            ExcelTemplate template = getOne(templateQuery);
            if (template == null) {
                return Result.fail("模板不存在");
            }

            // 查询数据
            QueryWrapper<ExcelDataAsset> dataQuery = new QueryWrapper<>();
            dataQuery.eq("template_code", templateCode);
            List<ExcelDataAsset> dataList = excelDataAssetService.list(dataQuery);

            // 转换为结构化数据
            List<Map<String, Object>> structuredData = dataList.stream()
                    .map(ExcelDataAsset::getDataMap)
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(structuredData)) {
                for (Map<String, Object> map : structuredData) {
                    String s = MapUtils.getString(map, "是否为光谱扫描");
                    if (YES.getDescription().equals(s)) {
                        //光谱扫描
                        map.put("measureType", 0);
                    } else {
                        //时间扫描
                        String timeScanCount = MapUtils.getString(map, "扫描个数");
                        if (StringUtils.isNotBlank(timeScanCount)) {
                            map.put("measureType", 1);
                        } else {
                            //光度测量
                            map.put("measureType", 2);
                        }
                    }
                }
            }
            return Result.ok(structuredData);
        } catch (Exception e) {
            log.error("获取结构化数据失败", e);
            return Result.fail("获取结构化数据失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, List<Map<String, Object>>>> getGroupedData(String templateCode, String groupByField, String checkCategory) {
        try {
            List<ExcelTemplate> list = new ArrayList<>();
            if (StringUtils.isBlank(groupByField)) {
                LambdaQueryWrapper<ExcelTemplate> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ExcelTemplate::getTemplateCode, templateCode);
                list = this.list(wrapper);
            } else {
//                LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
//                //查找的是开箱验证任务
//                wrapper.eq(TaskManagement::getTaskType, "2");
//                List<TaskManagement> list1 = taskManagementService.list(wrapper);
//                if (CollectionUtils.isEmpty(list1)) {
//                    log.error("没有找到开箱验证任务");
//                } else {
                LambdaQueryWrapper<ExcelTemplate> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ExcelTemplate::getTemplateCode, templateCode);
                list = this.list(wrapper);
//                    TaskManagement management = list1.get(0);
//                    ExcelTemplate excelTemplate = this.getById(management.getUseTemplate());
//                    list.add(excelTemplate);
//                }
            }

            if (CollectionUtils.isNotEmpty(list)) {
                ExcelTemplate excelTemplate = list.get(0);

                // 获取结构化数据
                Result<List<Map<String, Object>>> structuredDataResult = getStructuredData(excelTemplate.getTemplateCode());
                if (structuredDataResult.getCode() != 200) {
                    return Result.fail("获取数据失败: " + structuredDataResult.getMessage());
                }

                List<Map<String, Object>> dataList = structuredDataResult.getData();

                // 按指定字段分组
                Map<String, List<Map<String, Object>>> groupedData = dataList.stream()
                        .collect(Collectors.groupingBy(
                                map -> String.valueOf(map.getOrDefault(groupByField, "未知")),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));
                List<Map<String, Object>> maps = generateDropdownOptions();
                groupedData.put("dropdownOptions", maps);
                Result<Map<String, List<Map<String, Object>>>> result = new Result<>();
                if (!list.isEmpty()) {
                    result.setObj(new Object[]{list.get(0)});
                }
                result.setData(groupedData);
                result.setCode(200);
                return result;
            } else {
                return Result.fail("未找到模板");
            }
        } catch (Exception e) {
            log.error("获取分组数据失败", e);
            return Result.fail("获取分组数据失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, List<Map<String, Object>>>> getGroupedData(String templateCode) {
        try {
            LambdaQueryWrapper<ExcelTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ExcelTemplate::getTemplateCode, templateCode);
            List<ExcelTemplate> list = this.list(wrapper);


            // 获取结构化数据
            Result<List<Map<String, Object>>> structuredDataResult = getStructuredData(templateCode);
            if (structuredDataResult.getCode() != 200) {
                return Result.fail("获取数据失败: " + structuredDataResult.getMessage());
            }

            List<Map<String, Object>> dataList = structuredDataResult.getData();

            // 按指定字段分组
            Map<String, List<Map<String, Object>>> groupedData = dataList.stream()
                    .collect(Collectors.groupingBy(
                            map -> String.valueOf(map.getOrDefault(TEST_CATEGORY.getDescription(), "未知")),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));
            // 从每个分组的map中移除id字段
            groupedData.replaceAll((key, value) ->
                    value.stream()
                            .map(itemMap -> {
                                Map<String, Object> newMap = new HashMap<>(itemMap);
                                newMap.remove("id");
                                return newMap;
                            })
                            .collect(Collectors.toList())
            );
            // 过滤掉每个分组中的含id的map
//            groupedData.replaceAll((key, value) ->
//                    value.stream()
//                            .filter(itemMap -> !itemMap.containsKey("id"))
//                            .collect(Collectors.toList())
//            );
//            List<Map<String, Object>> maps = generateDropdownOptions();
//            groupedData.put("dropdownOptions", maps);
            Result<Map<String, List<Map<String, Object>>>> result = new Result<>();
            if (!list.isEmpty()) {
                result.setObj(new Object[]{list.get(0)});
            }
            result.setData(groupedData);
            result.setCode(200);
            return result;
        } catch (Exception e) {
            log.error("获取分组数据失败", e);
            return Result.fail("获取分组数据失败: " + e.getMessage());
        }
    }

    /**
     * 生成下拉框选项
     *
     * @return 下拉选项列表
     */
    private List<Map<String, Object>> generateDropdownOptions() {
        List<Map<String, Object>> options = new ArrayList<>();
        Map<String, Object> dropdownMap = new HashMap<>();

        // 配置下拉选项
        dropdownMap.put("光度模式", Arrays.asList("T%", "Abs", "Es"));
        dropdownMap.put("扫描速度", Arrays.asList("极慢", "慢速", "中速", "快速", "极快"));
        dropdownMap.put("光谱带宽", Arrays.asList("0.5", "1.0", "2.0", "4.0"));
        dropdownMap.put("响应时间", Arrays.asList("1", "2", "3"));
        dropdownMap.put("是否为光谱扫描", Arrays.asList("是", "否"));
        dropdownMap.put("分析方法", Arrays.asList("寻峰", "求平均", "多次测量", "最大最小值差", "求半峰宽", "求最大偏移量", "求漂移"));
        options.add(dropdownMap);
        return options;
    }

    private boolean shouldSaveToFile(String fileName) {
        int count = operationCount.getOrDefault(fileName, 0) + 1;
        operationCount.put(fileName, count);
        return count % 5 == 0; // 每5次操作保存一次
    }

    private void calculateFormulas(Workbook workbook) {
        try {
            // 创建公式评估器
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            // 一次性计算所有公式（处理依赖关系）
            evaluator.evaluateAll();

            // 设置强制重新计算标志
            workbook.setForceFormulaRecalculation(true);

        } catch (Exception e) {
            log.error("Formula calculation error", e);
        }
    }
}
