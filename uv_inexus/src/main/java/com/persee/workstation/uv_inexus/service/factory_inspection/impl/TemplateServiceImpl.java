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
import com.persee.workstation.uv_inexus.dao.factory_inspection.TemplateMapper;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateCreationRequest;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateQeqDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateResponseDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateUpdateRequest;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelDataAsset;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelTemplate;
import com.persee.workstation.uv_inexus.service.factory_inspection.ExcelDataAssetService;
import com.persee.workstation.uv_inexus.service.factory_inspection.TemplateService;
import com.persee.workstation.uv_inexus.util.ExcelCellEditor;
import com.persee.workstation.uv_inexus.util.ExcelCellLocator;
import com.persee.workstation.uv_inexus.util.IdGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class TemplateServiceImpl extends ServiceImpl<TemplateMapper, ExcelTemplate> implements TemplateService {
    @Autowired
    private TemplateMapper templateMapper;
    @Autowired
    private ExcelDataAssetService excelDataAssetService;
    @Value("${excel.save.path}")
    private String excelSavePath;
    @Autowired
    private ExcelCellLocator excelCellLocator;
    @Autowired
    private ExcelCellEditor excelCellEditor;

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
    public Result createTemplate(TemplateCreationRequest request) {
        if (StringUtils.equals(request.getType(),"0")) {
            log.info("创建模板开始");
            try {
                String templateCode = "TEMP_" + System.currentTimeMillis();

                // 创建新模板
                ExcelTemplate template = new ExcelTemplate();
                template.setTemplateCode(templateCode);
                template.setTemplateName(request.getTemplateName());
                template.setCreateBy(request.getUpdateBy());
                //非内置模板
                template.setCategory("1");
                template.setRemark(request.getRemark());
                template.setId(IdGeneratorUtil.generateIdWithPrefix("template"));
                template.setParentId(request.getTemplateCode());
                // 保存到数据库
                boolean saved = this.save(template);
                LambdaQueryWrapper<ExcelDataAsset> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ExcelDataAsset::getTemplateCode, request.getTemplateCode());

                List<Map<String, Object>> selectedRows = request.getSelectedRows().values().stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
                int rowIndex = 1; // 从第1行开始计数（跳过表头）
                for (Map<String, Object> dataMap : selectedRows) {
                    ExcelDataAsset asset = new ExcelDataAsset();
                    asset.setId(IdGeneratorUtil.generateIdWithPrefix("asset"));
                    asset.setTemplateCode(templateCode);
                    asset.setBatchNo(String.valueOf(rowIndex++));
                    asset.setStructuredData(JSON.toJSONString(dataMap));
                    asset.setImportStatus("SUCCESS");
                    excelDataAssetService.save(asset);
                }
                if (saved) {
                    return Result.ok("模板创建成功");
                } else {
                    return Result.fail("模板创建失败");
                }
            } catch (Exception e) {
                return Result.fail("模板创建失败: " + e.getMessage());
            }
        }
        if (StringUtils.equals(request.getType(),"0")) {
            log.info("修改模板开始");
            try {
                String templateCode = request.getTemplateCode();
                String remark = request.getRemark();
                String updateBy = request.getUpdateBy();
                ExcelTemplate template = new ExcelTemplate();
                template.setUpdated(LocalDateTime.now());
                template.setRemark(remark);
                template.setUpdateBy(updateBy);
                template.setId(request.getId());
                template.setCategory("1");
                template.setTemplateName(request.getTemplateName());
                template.setTemplateCode(templateCode);
                boolean b = this.updateById(template);
                return null;

//                if () {
//                    return Result.ok("模板修改成功");
//                } else {
//                    return Result.fail("模板修改失败");
//                }
            } catch (Exception e) {
                return Result.fail("模板修改失败: " + e.getMessage());
            }
        }
        return null;
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
     * @param resultTemplate
     * @param sheetRetainRows
     * @return
     * @throws IOException
     */
    public Result editExcel(String resultTemplate, ExcelTemplate template, Map<String, Set<String>> sheetRetainRows) throws IOException {
        System.out.println("88888888888888888888888888888888" + resultTemplate);
        File file = new File(resultTemplate);
        boolean b = file.getParentFile().mkdirs();// 创建父目录（如果不存在）
        InputStream is = Files.newInputStream(Paths.get(resultTemplate));
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
            //获取以 检测类别 分组的数据，检测类别就是sheet名
            Map<String, List<Map<String, Object>>> map = getGroupedData(template.getTemplateCode(), "检测类别").getData();

            for (Map.Entry<String, List<Map<String, Object>>> entry : map.entrySet()) {
                String sheetName = entry.getKey();
                Sheet sheet = workbook.getSheet(sheetName);
                List<Map<String, Object>> value = entry.getValue();

                for (Map<String, Object> temp : value) {
                    System.out.println(temp);

                    // 提前获取常用值
                    Object projectNameObj = temp.get("项目名称");
                    if (projectNameObj == null) {
                        continue;
                    }
                    String projectName = String.valueOf(projectNameObj);

                    // 处理特征值
                    Object featureValueObj = temp.get("特征值");
                    if (featureValueObj != null) {
                        String featureValue = String.valueOf(featureValueObj);
                        updateCellValues(sheet, excelCellLocator, excelCellEditor, "特征值", projectName, featureValue);
                    }
                    updateCellValues(sheet, excelCellLocator, excelCellEditor, "测量值", projectName, "123");
                    // 处理标准值
                    Object standardValueObj = temp.get("标准值");
                    if (standardValueObj != null) {
                        String standardValue = String.valueOf(standardValueObj);
                        updateCellValues(sheet, excelCellLocator, excelCellEditor, "标准值", projectName, standardValue);
                    }
                }
            }


            processWorkbookCells(workbook);

            // 返回编辑后的文件
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            String excelPath = excelSavePath + template.getTemplateCode() + ".xlsx";

            File excelFile = new File(excelPath);
            excelFile.getParentFile().mkdirs(); // 创建父目录（如果不存在）

            try (FileOutputStream fos = new FileOutputStream(excelPath)) {
                out.writeTo(fos);  // 将输出流内容写入文件
            }
            out.close();
            return Result.ok();
        } catch (Exception e) {
            log.error("文件处理失败", e);
        } finally {
            IOUtils.closeQuietly(workbook);
        }
        return Result.fail("文件处理失败");
    }

    // 提取的公共方法
    private void updateCellValues(Sheet sheet, ExcelCellLocator excelCellLocator,
                                  ExcelCellEditor excelCellEditor, String valueType,
                                  String projectName, String cellValue) {
        List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, valueType, projectName);
        if (cellValue.contains(",")) {
            String[] values = cellValue.split(",");
            for (int i = 0; i < positions.size() && i < values.length; i++) {
                ExcelCellLocator.CellPosition position = positions.get(i);
                excelCellEditor.setCellValue(sheet, position.getRow(), position.getCol(), values[i].trim());
            }
        } else {
            for (ExcelCellLocator.CellPosition position : positions) {
                excelCellEditor.setCellValue(sheet, position.getRow(), position.getCol(), cellValue);
            }
        }
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
            String excelPath = excelSavePath + "excel" + s + ".xlsx";
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

    @Override
    public Result buildResultTemplate(String templateCode) {
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
                    .filter(map -> map.containsKey("检测类别") && map.containsKey("项目名称"))
                    .collect(Collectors.groupingBy(
                            map -> map.get("检测类别").toString(),
                            Collectors.mapping(map -> map.get("项目名称").toString(), Collectors.toSet())
                    ));
            LambdaQueryWrapper<ExcelTemplate> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ExcelTemplate::getTemplateCode, templateCode);
            ExcelTemplate template = getOne(queryWrapper);
            // 如果是子模板，则获取父模板   Category=1是子模板
            if (StringUtils.equals(template.getCategory(), "1")) {
                ExcelTemplate excelTemplate = getById(template.getParentId());
                editExcel(excelSavePath + excelTemplate.getTemplateCode() + ".xlsx", template, sheetRetainRows);
            }
            return Result.ok();
        } catch (IOException e) {
            log.error("文件处理失败", e);
            return Result.fail();
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
    public Result getTemplateList(TemplateQeqDto templateQeqDto) {
        Page<ExcelTemplate> page = new Page<>(templateQeqDto.getPageNum(), templateQeqDto.getPageSize());
        LambdaQueryWrapper<ExcelTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(ExcelTemplate::getTemplateName, templateQeqDto.getQuery());

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

            return Result.ok(structuredData);
        } catch (Exception e) {
            log.error("获取结构化数据失败", e);
            return Result.fail("获取结构化数据失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, List<Map<String, Object>>>> getGroupedData(String templateCode, String groupByField) {
        try {
            // 获取结构化数据
            Result<List<Map<String, Object>>> structuredDataResult = getStructuredData(templateCode);
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

            return Result.ok(groupedData);
        } catch (Exception e) {
            log.error("获取分组数据失败", e);
            return Result.fail("获取分组数据失败: " + e.getMessage());
        }
    }
}
