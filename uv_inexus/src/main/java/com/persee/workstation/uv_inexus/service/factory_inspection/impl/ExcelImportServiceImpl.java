package com.persee.workstation.uv_inexus.service.factory_inspection.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.persee.workstation.uv_inexus.dao.factory_inspection.TaskExecutionDetailMapper;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.CalStdCats;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelDataAsset;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelTemplate;
import com.persee.workstation.uv_inexus.po.factory_inspection.TaskExecutionDetail;
import com.persee.workstation.uv_inexus.service.factory_inspection.*;
import com.persee.workstation.uv_inexus.util.IdGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.*;

@Slf4j
@DS("factory_inspection")
@Service
public class ExcelImportServiceImpl implements ExcelImportService {

    @Autowired
    private TemplateService templateService;

    @Autowired
    private ExcelDataAssetService excelDataAssetService;
    @Autowired
    private CalStdCatsService calStdCatsService;
    @Autowired
    private TaskExecutionDetailMapper taskExecutionDetailMapper;
    @Autowired
    private WorkbookDataService workbookDataService;

    public static List<Map<String, Object>> extractDataWithHeaders(Sheet sheet, byte[] colorRgb) {
        List<Map<String, Object>> result = new ArrayList<>();
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return result;

        // 获取黄色列索引
        List<Integer> columnIndices = findHeaderColumnsByColor(sheet, colorRgb);

        // 遍历数据行（从第1行开始）
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            // 获取第一列值（处理合并单元格）
            String firstColumnValue = getFirstColumnValue(row);

            // 如果第一列为空，跳过
            if (StringUtils.isBlank(firstColumnValue)) continue;

            Map<String, Object> rowData = new HashMap<>();
            rowData.put(PROJECT_NAME.getDescription(), firstColumnValue);

            // 提取黄色列数据
            for (int colIndex : columnIndices) {
                Cell cell = row.getCell(colIndex);
                String value = getCellValue(cell);
                Cell headerCell = headerRow.getCell(colIndex);
                String headerName = getCellValue(headerCell);
                rowData.put(headerName, value);
            }

            result.add(rowData);
        }
        //这里的list做一下去重，有一样的项就去掉多余的，留一个
// 按 "项目名称" 去重，保留第一个
        result = new ArrayList<>(result.stream()
                .filter(item -> item.get(PROJECT_NAME.getDescription()) != null)
                .collect(Collectors.toMap(
                        item -> (String) item.get(PROJECT_NAME.getDescription()),
                        item -> item,
                        (e1, e2) -> e1 // 保留第一个
                ))
                .values());
        return result;
    }

    /**
     * 获取第一列的值，支持合并单元格
     *
     * @param row 行对象
     * @return 第一列的值
     */
    private static String getFirstColumnValue(Row row) {
        Sheet sheet = row.getSheet();

        // 查找合并区域中包含当前行的第一列
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress mergedRegion = sheet.getMergedRegion(i);
            if (mergedRegion.getFirstColumn() == 0 &&
                    mergedRegion.getLastColumn() >= 0 &&
                    mergedRegion.getFirstRow() <= row.getRowNum() &&
                    mergedRegion.getLastRow() >= row.getRowNum()) {
                // 找到合并区域，获取左上角单元格的值
                Cell cell = sheet.getRow(mergedRegion.getFirstRow()).getCell(0);
                return getCellValue(cell);
            }
        }

        // 如果没有合并单元格，直接获取第一列的值
        Cell cell = row.getCell(0);
        return getCellValue(cell);
    }


    /**
     * 查找表头中背景为指定颜色的列索引（支持RGB颜色匹配）
     *
     * @param sheet    工作表
     * @param colorRgb 目标颜色的RGB值，格式为 [R, G, B]
     * @return 指定颜色列的索引列表
     */
    private static List<Integer> findHeaderColumnsByColor(Sheet sheet, byte[] colorRgb) {
        List<Integer> indices = new ArrayList<>();
        Row headerRow = sheet.getRow(0);

        if (headerRow != null) {
            for (Cell cell : headerRow) {
                CellStyle style = cell.getCellStyle();
                Color fillForegroundColor = style.getFillForegroundColorColor();

                if (fillForegroundColor != null &&
                        fillForegroundColor instanceof XSSFColor) {
                    XSSFColor xssfColor = (XSSFColor) fillForegroundColor;
                    byte[] rgb = xssfColor.getRGB();

                    if (rgb != null && rgb.length >= 3) {
                        // 转换为无符号整数进行比较
                        int r1 = rgb[0] & 0xFF;
                        int g1 = rgb[1] & 0xFF;
                        int b1 = rgb[2] & 0xFF;

                        int r2 = colorRgb[0] & 0xFF;
                        int g2 = colorRgb[1] & 0xFF;
                        int b2 = colorRgb[2] & 0xFF;

                        // 颜色相等判断
                        if (r1 == r2 && g1 == g2 && b1 == b2) {
                            indices.add(cell.getColumnIndex());
                        }
                    }
                }
            }
        }

        return indices;
    }


    /**
     * 获取单元格的字符串值，支持公式、数值、文本、布尔、合并单元格等所有类型
     *
     * @param cell 单元格对象
     * @return 字符串表示的单元格值
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // 处理日期和常规数字
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // 尝试获取公式的计算结果
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (IllegalStateException e) {
                    // 如果无法计算，返回公式本身
                    return cell.getCellFormula();
                }
            case ERROR:
                return "ERROR";
            default:
                return "";
        }
    }

    //找一下高亮的列名
    public static List<String> findHighlightedHeaders(Sheet sheet) {
        List<String> result = new ArrayList<>();

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            return result;
        }

        // 获取工作簿对象，用于解析颜色
        Workbook workbook = sheet.getWorkbook();

        // 找到第一个非空单元格作为基准样式（避免第一列本身就是高亮）
        CellStyle referenceStyle = null;
        for (Cell cell : headerRow) {
            if (cell != null && cell.getStringCellValue() != null && !cell.getStringCellValue().trim().isEmpty()) {
                referenceStyle = cell.getCellStyle();
                break;
            }
        }

        if (referenceStyle == null) {
            return result;
        }

        // 遍历所有表头单元格
        for (Cell cell : headerRow) {
            if (cell == null || cell.getStringCellValue() == null) continue;

            CellStyle currentStyle = cell.getCellStyle();

            // 比较填充颜色和填充模式
            if (!isSameFill(currentStyle, referenceStyle, workbook)) {
                result.add(cell.getStringCellValue());
            }
        }

        return result;
    }

    private static boolean isSameFill(CellStyle s1, CellStyle s2, Workbook workbook) {
        // 比较填充模式
        if (s1.getFillPattern() != s2.getFillPattern()) {
            return false;
        }

        // 比较前景色（需要转换为实际颜色）
        short fg1 = s1.getFillForegroundColor();
        short fg2 = s2.getFillForegroundColor();

        // 如果都是自动色或无填充，视为相同
        if ((fg1 == IndexedColors.AUTOMATIC.getIndex() || fg1 == IndexedColors.WHITE.getIndex()) &&
                (fg2 == IndexedColors.AUTOMATIC.getIndex() || fg2 == IndexedColors.WHITE.getIndex())) {
            return true;
        }

        // 如果前景色不同，则样式不同
        if (fg1 != fg2) {
            return false;
        }

        // 可选：比较背景色
        short bg1 = s1.getFillBackgroundColor();
        short bg2 = s2.getFillBackgroundColor();
        if (bg1 != bg2) {
            return false;
        }

        return true;
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result importExcel(MultipartFile file) {
        try {
            Workbook ww = new XSSFWorkbook(file.getInputStream());

//
//            // 看一下高亮的rgb是多少
//            Cell cell = ss.getRow(0).getCell(8);
//
//            CellStyle style = cell.getCellStyle();
//            Color fillForegroundColor = style.getFillForegroundColorColor();
//
//            if (fillForegroundColor != null) {
//                byte[] rgb = null;
//                XSSFColor xssfColor = (XSSFColor) fillForegroundColor;
//                // 尝试使用 getRGB() 方法
//                try {
//                    rgb = xssfColor.getRGB();
//                } catch (Exception e) {
//                    // 如果 getRGB() 不可用，尝试其他方法
//                    try {
//                        // 获取 ARGB 值然后转换为 RGB
//                        byte[] argb = xssfColor.getARGB(); // 修正：getARGB() 返回 byte[]
//                        if (argb != null && argb.length >= 4) {
//                            rgb = new byte[]{
//                                    argb[1], // R (跳过alpha通道)
//                                    argb[2], // G
//                                    argb[3]  // B
//                            };
//                        }
//                    } catch (Exception ex) {
//                        log.warn("无法获取颜色RGB值: {}", ex.getMessage());
//                    }
//                }
//
//                if (rgb != null) {
//                    log.info("RGB: {}", Arrays.toString(rgb));
//                }
//            }

            //先搞一个结果模板
            List<String> sheetNames = Arrays.asList(
                    WAVELENGTH_CALIBRATION.getDescription(),
                    TRANSMITTANCE_CALIBRATION.getDescription(),
                    STRAY_LIGHT_CALIBRATION.getDescription(),
                    BASELINE_STRAIGHTNESS_INSPECTION.getDescription(),
                    BASELINE_DARK_NOISE_INSPECTION.getDescription(),
                    SPECTRAL_BANDWIDTH_CALIBRATION.getDescription(),
                    NOISE_INSPECTION.getDescription(),
                    WAVELENGTH_EDGE_NOISE.getDescription(),
                    DRIFT_INSPECTION.getDescription()
            );
//            sheetNames.add("Report");

            Workbook workbook = extractSheetsToNewWorkbook(file, sheetNames);

            for (Sheet sheet : workbook) {
                List<String> list = findHighlightedHeaders(sheet);
                if (!list.isEmpty()) {
                    log.info("找到高亮列名：{}", list);
                }
            }
//            /*构建要在根目录创建的文件名，路径*/
//            String projectRoot = System.getProperty("user.dir");
//            // 生成唯一文件名并保存
//            String originalFilename = file.getOriginalFilename();
//            String fileExtension = originalFilename != null && originalFilename.contains(".") ?
//                    originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
//            String uniqueFilename = IdGeneratorUtil.generateIdWithPrefix("IE") + fileExtension;
//            String fullPath = projectRoot+"/"+excelSavePath + uniqueFilename;
//
            ExcelTemplate template = new ExcelTemplate();
            template.setId(IdGeneratorUtil.generateIdWithPrefix("template"));
            /*保存模板信息*/
            // 生成模板编码
            String templateCode = template.getId();
            boolean b = workbookDataService.uploadExcel(workbook, templateCode);
            if (!b) {
                log.error("保存模板信息失败");
            }
            if (b) {
                log.info("保存模板信息成功");
            }

            String sheeetName = "检测项信息";
            // 读取Excel文件，获取表头信息
            List<List<String>> headList = new ArrayList<>();
            try (InputStream inputStream = file.getInputStream()) {
                ExcelReader excelReader = EasyExcel.read(inputStream).build();
                ReadSheet readSheet = EasyExcel.readSheet(sheeetName).headRowNumber(0).registerReadListener(new HeadReadListener(headList)).build();
                excelReader.read(readSheet);
            }

            if (headList.isEmpty() || headList.get(0).isEmpty()) {
                return Result.fail("Excel文件没有表头信息");
            }

            // 创建模板

//            template.setTemplateCode(templateCode);
            //设置文件名
            String originalFilename = file.getOriginalFilename();
            String filenameWithoutExtension = originalFilename;
            if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
                filenameWithoutExtension = originalFilename.substring(0, originalFilename.lastIndexOf("."));
            }
            template.setTemplateName(filenameWithoutExtension);
            template.setStatus(1); // 模板为检定校准模板
            // 构建列配置
            List<ExcelTemplate.ColumnConfig> columnConfigs = new ArrayList<>();
            List<String> headers = headList.get(0);
            for (int i = 0; i < headers.size(); i++) {
                ExcelTemplate.ColumnConfig config = new ExcelTemplate().new ColumnConfig();
                config.setOrder(i);
                config.setExcelHeader(headers.get(i));
                config.setDataType("STRING");
                columnConfigs.add(config);
            }
            columnConfigs.sort(Comparator.comparingInt(ExcelTemplate.ColumnConfig::getOrder));
            template.setColumnConfig(JSON.toJSONString(columnConfigs));

            // 保存模板
            templateService.save(template);

            /*保存数据信息*/
            List<LinkedHashMap<String, Object>> dataList = new ArrayList<>();
            try (InputStream inputStream = file.getInputStream()) {
                ExcelReader excelReader = EasyExcel.read(inputStream).build();
                ReadSheet readSheet = EasyExcel.readSheet(sheeetName).headRowNumber(1).registerReadListener(new DataReadListener(dataList, headers)).build();
                excelReader.read(readSheet);
            }


            // 新增标准物质种类
            Set<String> standardMaterials = new HashSet<>();
            Map<String, Map<String, Object>> assetStandardsMap = new HashMap<>();
            //标准物质的特征值和标准值
            Map<String, Map<String, Object>> standardMaterialsMap = new HashMap<>();
            // 保存数据资产
            int rowIndex = 1; // 从第1行开始计数（跳过表头）
            ArrayList<ExcelDataAsset> list = new ArrayList<>();
            for (LinkedHashMap<String, Object> dataMap : dataList) {
                String s = JSON.toJSONString(dataMap);
                ExcelDataAsset asset = new ExcelDataAsset();
                asset.setId(IdGeneratorUtil.generateIdWithPrefix("asset"));
                asset.setTemplateCode(templateCode);
                asset.setBatchNo(String.valueOf(rowIndex++));
                asset.setStructuredData(s);
                String type = PHOTOMETRIC_SCAN.getCode();
                if (s.contains("WlRange")) {
                    type = SPECTRAL_SCAN.getCode();
                }
                if (s.contains("ResponseTime")) {
                    type = TIME_SCAN.getCode();
                }
                asset.setImportStatus(type);
                list.add(asset);

                String zeroMaterial = MapUtils.getString(dataMap, ZERO.getDescription());
                String measurementMaterial = MapUtils.getString(dataMap, MEASUREMENT.getDescription());
                String stdValue = MapUtils.getString(dataMap, STANDARD.getDescription());
                String characteristicValue = MapUtils.getString(dataMap, CHARACTERISTIC.getDescription());

                standardMaterialsMap.put(measurementMaterial, new HashMap<String, Object>() {{
                    put("s", stdValue);
                    put("c", characteristicValue);
                }});
                standardMaterials.add(zeroMaterial);
                standardMaterials.add(measurementMaterial);

                Map<String, Object> standards = new HashMap<>();
                standards.put(ZERO.getDescription(), zeroMaterial);
                standards.put(MEASUREMENT.getDescription(), measurementMaterial);
                assetStandardsMap.put(asset.getId(), standards);
            }
            // 创建 Map
            Map<String, ExcelDataAsset> assetMap = list.stream()
                    .collect(Collectors.toMap(ExcelDataAsset::getId, item -> item));

            // 批量保存标准物质种类
            List<CalStdCats> standardCategories = standardMaterials.stream()
                    .filter(Objects::nonNull)
                    .map(material -> {
                        CalStdCats cat = new CalStdCats();
                        if (standardMaterialsMap.containsKey(material) /*&& !BLACK_BLOCK.getDescription().equals(material) && !AIR.getDescription().equals(material)*/) {
                            Map<String, Object> map = standardMaterialsMap.get(material);
                            cat.setStandardValue(MapUtils.getString(map, "s"));
                            cat.setCharacteristicValue(MapUtils.getString(map, "c"));
                        }
                        if (BLACK_BLOCK.getDescription().equals(material) || AIR.getDescription().equals(material)) {
                            cat.setStandardValue(null);
                            cat.setCharacteristicValue(null);
                        }
                        cat.setId(IdGeneratorUtil.generateIdWithPrefix("cal"));
                        cat.setName(material);
                        cat.setRemark("导入的默认标准物质种类");
                        cat.setMaterialType(BASE_FIXED_SUBSTANCE.getCode());
                        return cat;
                    })
                    .collect(Collectors.toList());

            Map<String, CalStdCats> standardCategoryMap = standardCategories.stream()
                    .collect(Collectors.toMap(CalStdCats::getId, item -> item));


            if (CollectionUtils.isNotEmpty(standardCategories)) {
                calStdCatsService.saveBatch(standardCategories);
            }

            // 建立标准物质名称到ID的映射，减少数据库查询次数
            Map<String, String> materialNameToIdMap = standardCategories.stream()
                    .collect(Collectors.toMap(CalStdCats::getName, CalStdCats::getId));

            // 保存任务执行详情
            List<TaskExecutionDetail> taskDetails = new ArrayList<>();
            for (Map.Entry<String, Map<String, Object>> entry : assetStandardsMap.entrySet()) {
                String assetId = entry.getKey();
                Map<String, Object> standards = entry.getValue();


                // 校零物质任务详情
                String zeroMaterial = MapUtils.getString(standards, ZERO.getDescription());
                if (zeroMaterial != null) {
                    TaskExecutionDetail zeroDetail = new TaskExecutionDetail();
                    zeroDetail.setId(IdGeneratorUtil.generateId());
                    zeroDetail.setItemId(assetId);
                    zeroDetail.setStandardId(materialNameToIdMap.get(zeroMaterial));
                    zeroDetail.setStandardType(ZERO.getCode());
                    taskDetails.add(zeroDetail);

                    ExcelDataAsset excelDataAsset = assetMap.get(assetId);
                    Map<String, Object> dataMap = excelDataAsset.getDataMap();
                    String z = MapUtils.getString(dataMap, ZERO.getDescription());
                    JSONObject js = new JSONObject();
                    CalStdCats cats = standardCategoryMap.get(zeroDetail.getStandardId());
                    js.put("id", zeroDetail.getStandardId());
                    js.put("name", z);
                    js.put("characteristicValue", StringUtils.isBlank(cats.getCharacteristicValue()) ? "" : cats.getCharacteristicValue());
                    js.put("standardValue", StringUtils.isBlank(cats.getStandardValue()) ? "" : cats.getStandardValue());
                    dataMap.put(ZERO.getDescription(), js);
                    excelDataAsset.setStructuredData(JSON.toJSONString(dataMap));
                    assetMap.put(assetId, excelDataAsset);
                }

                // 测量物质任务详情
                String measurementMaterial = MapUtils.getString(standards, MEASUREMENT.getDescription());
                if (measurementMaterial != null) {
                    TaskExecutionDetail measurementDetail = new TaskExecutionDetail();
                    measurementDetail.setId(IdGeneratorUtil.generateId());
                    measurementDetail.setItemId(assetId);
                    measurementDetail.setStandardId(materialNameToIdMap.get(measurementMaterial));
                    measurementDetail.setStandardType(MEASUREMENT.getCode());
                    taskDetails.add(measurementDetail);

                    ExcelDataAsset excelDataAsset = assetMap.get(assetId);
                    Map<String, Object> dataMap = excelDataAsset.getDataMap();
                    String z = MapUtils.getString(dataMap, MEASUREMENT.getDescription());
                    JSONObject js = new JSONObject();
                    CalStdCats cats = standardCategoryMap.get(measurementDetail.getStandardId());
                    js.put("id", measurementDetail.getStandardId());
                    js.put("name", z);
                    js.put("characteristicValue", StringUtils.isBlank(cats.getCharacteristicValue()) ? "" : cats.getCharacteristicValue());
                    js.put("standardValue", StringUtils.isBlank(cats.getStandardValue()) ? "" : cats.getStandardValue());
                    dataMap.put(MEASUREMENT.getDescription(), js);
                    excelDataAsset.setStructuredData(JSON.toJSONString(dataMap));
                    assetMap.put(assetId, excelDataAsset);

                }
            }
            //高亮的rgb值
            byte[] rgb = new byte[]{-1, -64, 0};
            List<ExcelDataAsset> collect = new ArrayList<>(assetMap.values());
            if (CollectionUtils.isNotEmpty(collect)) {
                for (ExcelDataAsset asset : collect) {
                    Map<String, Object> dataMap = asset.getDataMap();
                    //检测类别
                    String testCategory = MapUtils.getString(dataMap, TEST_CATEGORY.getDescription());
                    //获取这个检测类别结果的 sheet
                    Sheet wwSheet = ww.getSheet(testCategory);
                    String projectName = MapUtils.getString(dataMap, PROJECT_NAME.getDescription());

                    List<Map<String, Object>> maps = extractDataWithHeaders(wwSheet, rgb);
                    if (CollectionUtils.isNotEmpty(maps)) {
                        for (Map<String, Object> map : maps) {
                            if (StringUtils.equals(MapUtils.getString(map, PROJECT_NAME.getDescription()), projectName)) {
                                //把项目名称删除，只留下高亮部分的表头名
                                Set<String> key = map.keySet();
                                key.remove(PROJECT_NAME.getDescription());
//                                JSONObject jsonObject = new JSONObject();
                                if (CollectionUtils.isNotEmpty(key)) {
                                    for (String s : key) {
                                        dataMap.put(s, map.get(s));
//                                    jsonObject.put(s, map.get(s));
                                    }
                                }
                            }
                        }
                    }
                    asset.setStructuredData(JSON.toJSONString(dataMap));
                    //检测项名称
                }
                excelDataAssetService.saveBatch(collect);
            }

            // 批量插入任务执行详情
            if (CollectionUtils.isNotEmpty(taskDetails)) {
                taskDetails.forEach(taskExecutionDetailMapper::insert);
            }
//            // 保存文件
//            File destFile = new File(fullPath);
//            file.transferTo(destFile);


            Map<String, Object> result = new HashMap<>();
            result.put("templateCode", templateCode);
            result.put("dataCount", dataList.size());

            return Result.ok(result);
        } catch (Exception e) {
            log.error("导入Excel失败", e);
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /**
     * 从原始Excel中提取指定Sheet并生成新工作簿
     *
     * @param file       原始Excel文件
     * @param sheetNames 要提取的Sheet名称列表
     * @return 新的工作簿对象
     */
    public Workbook extractSheetsToNewWorkbook(MultipartFile file, List<String> sheetNames)
            throws IOException {
        // 读取原始工作簿
        Workbook sourceWorkbook = new XSSFWorkbook(file.getInputStream());

        // 创建新的工作簿
        Workbook newWorkbook = new XSSFWorkbook();

        for (String sheetName : sheetNames) {
            Sheet sourceSheet = sourceWorkbook.getSheet(sheetName);
            if (sourceSheet != null) {
                // 在新工作簿中创建同名Sheet
                Sheet newSheet = newWorkbook.createSheet(sourceSheet.getSheetName());

                // 复制Sheet内容
                copySheetContent(sourceSheet, newSheet);
            }
        }

        // 关闭原始工作簿，保留新工作簿供后续使用
        sourceWorkbook.close();

        return newWorkbook;
    }

    /**
     * 复制Sheet的所有内容
     */
    /**
     * 复制Sheet的所有内容
     */
    private void copySheetContent(Sheet sourceSheet, Sheet targetSheet) {
        // 复制行和单元格
        for (Row sourceRow : sourceSheet) {
            Row targetRow = targetSheet.createRow(sourceRow.getRowNum());

            for (Cell sourceCell : sourceRow) {
                Cell targetCell = targetRow.createCell(sourceCell.getColumnIndex());

                // 复制单元格值和类型
                copyCellData(sourceCell, targetCell);
            }
        }

        // 复制合并区域
        copyMergedRegions(sourceSheet, targetSheet);

        // 复制列宽
        for (int i = 0; i < sourceSheet.getRow(0).getLastCellNum(); i++) {
            targetSheet.setColumnWidth(i, sourceSheet.getColumnWidth(i));
        }
    }

    /**
     * 复制合并区域
     */
    private void copyMergedRegions(Sheet sourceSheet, Sheet targetSheet) {
        for (int i = 0; i < sourceSheet.getNumMergedRegions(); i++) {
            CellRangeAddress mergedRegion = sourceSheet.getMergedRegion(i);
            targetSheet.addMergedRegion(mergedRegion);
        }
    }


    /**
     * 复制单元格数据
     */
    private void copyCellData(Cell sourceCell, Cell targetCell) {
        if (sourceCell == null) {
            return;
        }

        switch (sourceCell.getCellType()) {
            case FORMULA:
                targetCell.setCellFormula(sourceCell.getCellFormula());
                break;
            case STRING:
                targetCell.setCellValue(sourceCell.getStringCellValue());
                break;
            case NUMERIC:
                targetCell.setCellValue(sourceCell.getNumericCellValue());
                break;
            case BOOLEAN:
                targetCell.setCellValue(sourceCell.getBooleanCellValue());
                break;
            case BLANK:
                targetCell.setBlank();
                break;
            default:
                targetCell.setCellType(sourceCell.getCellType());
                break;
        }
    }


    /**
     * 表头读取监听器
     */
    private static class HeadReadListener implements ReadListener<Map<Integer, String>> {
        private final List<List<String>> headList;

        public HeadReadListener(List<List<String>> headList) {
            this.headList = headList;
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            log.info("表头: {}", data);
            List<String> headers = new ArrayList<>();
            Set<String> seenHeaders = new HashSet<>();
            for (int i = 0; i < data.size(); i++) {
                String header = data.get(i);
                // 如果表头已存在，添加序号区分
                if (seenHeaders.contains(header)) {
                    int suffix = 1;
                    String newHeader = header + "_" + suffix;
                    while (seenHeaders.contains(newHeader)) {
                        suffix++;
                        newHeader = header + "_" + suffix;
                    }
                    header = newHeader;
                }
                headers.add(header);
                seenHeaders.add(header);
            }
            headList.add(headers);
        }


        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // 处理完成
        }
    }

    /**
     * 数据读取监听器
     */
    private static class DataReadListener implements ReadListener<Map<Integer, String>> {
        private final List<LinkedHashMap<String, Object>> dataList;
        private final List<String> headers;

        public DataReadListener(List<LinkedHashMap<String, Object>> dataList, List<String> headers) {
            this.dataList = dataList;
            this.headers = headers;
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            LinkedHashMap<String, Object> rowData = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                rowData.put(headers.get(i), data.get(i));
            }
            dataList.add(rowData);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // 处理完成
        }
    }
}