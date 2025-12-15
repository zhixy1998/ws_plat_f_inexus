package com.persee.workstation.uv_inexus.websocket.factory_inspection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.component.WorkbookCacheManager;
import com.persee.workstation.uv_inexus.dto.factory_inspection.*;
import com.persee.workstation.uv_inexus.service.factory_inspection.TemplateService;
import com.persee.workstation.uv_inexus.util.ExcelCellEditor;
import com.persee.workstation.uv_inexus.util.ExcelCellLocator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.AUTO;
import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.SINGLE_ITEM;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

/**
 * Excel缓存和会话存储，包括会话信息、文件名、会话ID、连接时间
 */
/*会话管理
每个WebSocket连接创建独立的Excel工作簿缓存
通过WorkbookCacheManager管理各会话的工作簿实例
实时编辑
接收前端发送的单元格更新指令(CellUpdDto)
调用TemplateService更新指定单元格数据
自动计算Excel公式并更新工作簿
数据推送
更新完成后将变更的Sheet数据序列化并推送给前端
支持二进制格式传输，提升效率
持久化机制
操作一定次数后自动保存到文件
连接断开时清理会话并持久化最终数据
* */
@Component
@Slf4j
public class ExcelWebSocketHandler extends TextWebSocketHandler {
    @Autowired
    private WorkbookCacheManager workbookCacheManager;

    // 会话存储：sessionId -> SessionInfo
    private final Map<String, SessionInfo> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> operationCount = new ConcurrentHashMap<>();
    @Autowired
    private TemplateService templateService;
    @Autowired
    private ExcelCellLocator excelCellLocator;
    @Autowired
    private ExcelCellEditor excelCellEditor;

    // 会话信息类
    @Data
    private static class SessionInfo {
        private String fileName;
        private String sessionId;
        private long connectTime;
    }

    //这里的文件名应该是对应着使用模板的文件名  也就是说结果模板名和使用模板名一致
    //注意这里说的模板名是模板code值
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            String sessionId = session.getId();
            // 从URL参数或握手信息中获取文件名
            String fileName = getFileNameFromSession(session);

            if (fileName != null) {
                // 为会话创建专属Workbook
                Workbook workbook = workbookCacheManager.createSessionWorkbook(sessionId, fileName);

                // 存储会话信息
                SessionInfo sessionInfo = new SessionInfo();
                sessionInfo.setFileName(fileName);
                sessionInfo.setSessionId(sessionId);
                sessionInfo.setConnectTime(System.currentTimeMillis());
                sessionMap.put(sessionId, sessionInfo);

                log.info("WebSocket连接建立，会话ID: {}, 文件: {}", sessionId, fileName);
            }
            this.pushEntireWorkbook(session, fileName);
            super.afterConnectionEstablished(session);
        } catch (Exception e) {
            log.error("WebSocket连接建立失败", e);
        }
    }

    private void pushEntireWorkbook(WebSocketSession session, String fileName) throws IOException {
        String sessionId = session.getId();
        Workbook workbook = workbookCacheManager.getSessionWorkbook(sessionId);

        if (workbook == null) return;

        WorkbookDto workbookDto = new WorkbookDto();
        workbookDto.setWorkbookName(fileName);
        workbookDto.setSheets(extractAllSheetsData(workbook));

        ObjectMapper mapper = new ObjectMapper();
        String jsonData = mapper.writeValueAsString(workbookDto);
        session.sendMessage(new TextMessage(jsonData));
    }

    private List<SheetDto> extractAllSheetsData(Workbook workbook) {
        List<SheetDto> sheets = new ArrayList<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            SheetDto sheetDto = extractSheetData(sheet, workbook);
            sheets.add(sheetDto);
        }
        return sheets;
    }

    public void pushUpdatedData(WebSocketSession session, CellUpdDto cellUpdDto) throws IOException {
        String sessionId = session.getId();
        // 获取会话专属Workbook
        Workbook workbook = workbookCacheManager.getSessionWorkbook(sessionId);
        if (workbook != null) {
            // 执行更新操作
            updateWorkbookCell(workbook, cellUpdDto);

            // 更新缓存
            workbookCacheManager.updateSessionWorkbook(sessionId, workbook);

//            // 推送更新后的数据给前端
            Sheet sheet = workbook.getSheet(cellUpdDto.getSheetName());

            sendSheetUpdate(session, sheet, cellUpdDto);
            // 定期保存到文件  执行五次就保存一次
            SessionInfo sessionInfo = sessionMap.get(sessionId);
            if (shouldSaveToFile(sessionInfo.getFileName())) {
                workbookCacheManager.saveWorkbookToFile(sessionInfo.getFileName(), workbook);
            }
        }
    }

    private String formatSheetAsTable(Sheet sheet) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sheet: ").append(sheet.getSheetName()).append("\n");
        sb.append(String.join("", java.util.Collections.nCopies(50, "="))).append("\n");


        for (int rowNum = sheet.getFirstRowNum(); rowNum <= sheet.getLastRowNum(); rowNum++) {
            org.apache.poi.ss.usermodel.Row row = sheet.getRow(rowNum);
            if (row != null) {
                sb.append("Row ").append(rowNum).append(": ");
                for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(cellNum);
                    if (cell != null) {
                        sb.append("[").append(getCellValueAsString(cell)).append("] ");
                    } else {
                        sb.append("[ ] ");
                    }
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return "=" + cell.getCellFormula();
            default:
                return "";
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(payload);
        String type = root.get("type").asText();

        if (SINGLE_ITEM.getCode().equals(type)){
            log.info("收到单项测试指令");
            InspectionRequestDto request = mapper.readValue(payload, InspectionRequestDto.class);
            handleSingleItemInspection(session, request);
        }
        if (AUTO.getCode().equals(type)){
            log.info("收到自动测试指令");
            AutoInspectionRequestDto autoRequest = mapper.readValue(payload, AutoInspectionRequestDto.class);
            handleAutoInspection(session, autoRequest.getItems());
        }
        //根据前端传输的内容进行处理  自动测试就按照模板进行测
        //单项测试就按照用户传过来的测试项id按照模板进行测试
        //后续得加上从机器获取数据，然后填充到实体类，进行修改并推送到前端
        CellUpdDto cellUpdDto = new CellUpdDto();
        cellUpdDto.setPosition(0);
        cellUpdDto.setCellValue("215");
        cellUpdDto.setFileName("TEMP_1765349747321");
        cellUpdDto.setSheetName("透射比示值误差与重复性校验");
        cellUpdDto.setValueType("测量值");
        cellUpdDto.setProjectName("紫外分光光度计标准物质GBW(E)130066");
        pushUpdatedData(session, cellUpdDto);
        super.handleTextMessage(session, message);
    }

    private void handleAutoInspection(WebSocketSession session, List<InspectionRequestDto> items) {

    }

    private void handleSingleItemInspection(WebSocketSession session, InspectionRequestDto request) {

    }

    private boolean shouldSaveToFile(String fileName) {
        int count = operationCount.getOrDefault(fileName, 0) + 1;
        operationCount.put(fileName, count);
        return count % 5 == 0; // 每5次操作保存一次
    }

    private void calculateFormulas(Workbook workbook) {
        try {
            // 使用简单直接的方式计算所有公式
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            // 执行多次计算确保依赖关系正确处理
            for (int i = 0; i < 3; i++) {
                evaluator.evaluateAll();
            }

            workbook.setForceFormulaRecalculation(true);
        } catch (Exception e) {
            log.error("Formula calculation error", e);
        }
    }


    private void evaluateByDependencyLevel(Workbook workbook, FormulaEvaluator evaluator, int level) {
        log.debug("开始第 {} 层公式计算", level);

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.FORMULA) {
                        if (shouldCalculateAtLevel(cell, level)) {
                            try {
                                evaluator.evaluateFormulaCell(cell);
                                log.debug("计算公式 {}: {}", cell.getAddress(), cell.getCellFormula());
                            } catch (Exception e) {
                                log.warn("第{}层公式计算失败 {}: {}",
                                        level, cell.getAddress(), e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean shouldCalculateAtLevel(Cell cell, int level) {
        String formula = cell.getCellFormula();

        switch (level) {
            case 1: // 基础层：简单计算公式
                return isBasicFormula(formula);
            case 2: // 中间层：依赖基础层的公式
                return isIntermediateFormula(formula);
            case 3: // 顶层：复杂判定公式
                return isComplexJudgmentFormula(formula);
            default:
                return true;
        }
    }

    private boolean isBasicFormula(String formula) {
        // 基础公式：不包含其他公式计算结果的引用
        return formula.contains("AVERAGE") || formula.contains("SUM") ||
                formula.contains("MAX") || formula.contains("MIN");
    }

    private boolean isIntermediateFormula(String formula) {
        // 中间层公式：包含基础计算结果但不包含判定结果
        return (formula.contains("ABS") || formula.contains("ROUND")) &&
                !formula.contains("IF(") && !formula.startsWith("IF");
    }

    private boolean isComplexJudgmentFormula(String formula) {
        // 复杂判定公式：主要是IF嵌套判断
        return formula.startsWith("IF(") || formula.contains("IF(") && formula.contains("AND(");
    }

    private void forceRecalculateJudgmentCells(Workbook workbook) {
        try {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        if (cell.getCellType() == CellType.FORMULA) {
                            String formula = cell.getCellFormula();
                            // 强制重新计算判定类公式
                            if (formula.startsWith("IF(") && formula.contains("AND(")) {
                                try {
                                    evaluator.evaluateFormulaCell(cell);
                                    log.debug("强制重新计算判定公式 {}: {}",
                                            cell.getAddress(), formula);
                                } catch (Exception e) {
                                    log.warn("强制计算判定公式失败 {}: {}",
                                            cell.getAddress(), e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("强制重新计算判定单元格失败", e);
        }
    }


    private Map<String, Object> captureFormulaValues(Workbook workbook, FormulaEvaluator evaluator) {
        Map<String, Object> values = new HashMap<>();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.FORMULA) {
                        try {
                            // 强制计算每个公式单元格
                            CellValue cellValue = evaluator.evaluate(cell);
                            if (cellValue != null) {
                                Object value = getResultFromCellValue(cellValue);
                                values.put(cell.getAddress().formatAsString(), value);
                                log.debug("捕获公式值 {}: {}", cell.getAddress().formatAsString(), value);
                            } else {
                                values.put(cell.getAddress().formatAsString(), "NULL");
                                log.debug("捕获公式值 {}: NULL", cell.getAddress().formatAsString());
                            }
                        } catch (Exception e) {
                            values.put(cell.getAddress().formatAsString(), "ERROR");
                            log.debug("捕获公式值 {} 异常: {}", cell.getAddress().formatAsString(), e.getMessage());
                        }
                    }
                }
            }
        }

        return values;
    }


    private Object getResultFromCellValue(CellValue cellValue) {
        switch (cellValue.getCellType()) {
            case NUMERIC:
                return cellValue.getNumberValue();
            case STRING:
                return cellValue.getStringValue();
            case BOOLEAN:
                return cellValue.getBooleanValue();
            default:
                return "";
        }
    }


    /**
     * 更新Workbook中的单元格
     */
    private void updateWorkbookCell(Workbook workbook, CellUpdDto cellUpdDto) {
        Sheet sheet = workbook.getSheet(cellUpdDto.getSheetName());

        // 先更新单元格值
        templateService.updateCellValue(sheet, excelCellLocator, excelCellEditor,
                cellUpdDto.getValueType(), cellUpdDto.getProjectName(),
                cellUpdDto.getPosition(), cellUpdDto.getCellValue());

        // 计算所有公式
        calculateFormulas(workbook);
    }




    /**
     * 发送Sheet更新数据（结构化传输）
     */
    private void sendSheetUpdate(WebSocketSession session, Sheet sheet, CellUpdDto cellUpdDto) throws IOException {
        Workbook workbook = sheet.getWorkbook();

        // 构造完整的Sheet结构化数据
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("sheetName", sheet.getSheetName());
        updateData.put("sheetData", extractSheetData(sheet, workbook)); // 传递workbook用于公式计算
        updateData.put("updatedCell", createCellUpdateInfo(cellUpdDto)); // 更新的单元格信息

        ObjectMapper mapper = new ObjectMapper();
        String jsonData = mapper.writeValueAsString(updateData);
        session.sendMessage(new TextMessage(jsonData));
    }
    /**
     * 发送波形图更新消息
     */
    private void sendSpectrumUpdate(WebSocketSession session, SpectrumUpdateDto dto) throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "spectrum");
        data.put("chartId", dto.getChartId());
        data.put("xValues", dto.getXValues());
        data.put("yValues", dto.getYValues());
        data.put("isComplete", dto.isComplete());

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(data);
        session.sendMessage(new TextMessage(json));
    }

    /**
     * 根据图表ID获取对应表格列位置
     */
    private int getPositionByChartId(String chartId) {
        switch (chartId) {
            case "spectrum1": return 0;
            case "spectrum2": return 1;
            case "spectrum3": return 2;
            default: return 0;
        }
    }

    /**
     * 创建单元格更新信息
     */
    private Map<String, Object> createCellUpdateInfo(CellUpdDto cellUpdDto) {
        Map<String, Object> updateInfo = new HashMap<>();
        updateInfo.put("position", cellUpdDto.getPosition());
        updateInfo.put("value", cellUpdDto.getCellValue());
        updateInfo.put("valueType", cellUpdDto.getValueType());
        updateInfo.put("projectName", cellUpdDto.getProjectName());
        return updateInfo;
    }

    private SheetDto extractSheetData(Sheet sheet, Workbook workbook) {
        SheetDto sheetDto = new SheetDto();
        sheetDto.setSheetName(sheet.getSheetName());
        sheetDto.setFirstRowNum(sheet.getFirstRowNum());
        sheetDto.setLastRowNum(sheet.getLastRowNum());

        // 为每次提取数据创建新的公式评估器
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        evaluator.clearAllCachedResultValues();

        // 提取合并区域
        List<MergedRegionDto> mergedRegions = new ArrayList<>();

        for (CellRangeAddress region : sheet.getMergedRegions()) {
            MergedRegionDto merged = new MergedRegionDto();
            merged.setFirstRow(region.getFirstRow());
            merged.setLastRow(region.getLastRow());
            merged.setFirstCol(region.getFirstColumn());
            merged.setLastCol(region.getLastColumn());

            // 获取左上角单元格的值
            org.apache.poi.ss.usermodel.Row row = sheet.getRow(region.getFirstRow());
            if (row != null) {
                org.apache.poi.ss.usermodel.Cell cell = row.getCell(region.getFirstColumn());
                if (cell != null) {
                    merged.setValue(String.valueOf(getCellValue(cell, evaluator)));
                }
            }
            mergedRegions.add(merged);
        }
        sheetDto.setMergedRegions(mergedRegions);

        // 提取普通单元格数据
        Map<String, Map<String, Object>> rowData = new HashMap<>();
        for (int rowNum = sheet.getFirstRowNum(); rowNum <= sheet.getLastRowNum(); rowNum++) {
            org.apache.poi.ss.usermodel.Row row = sheet.getRow(rowNum);
            if (row != null) {
                Map<String, Object> cellData = new HashMap<>();
                for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(cellNum);
                    if (cell != null) {
                        cellData.put(String.valueOf(cellNum), getCellValue(cell, evaluator));
                    }
                }
                if (!cellData.isEmpty()) {
                    rowData.put(String.valueOf(rowNum), cellData);
                }
            }
        }
        sheetDto.setRows(rowData);

        return sheetDto;
    }


    /**
     * 获取单元格的值（带公式计算器）
     */
    private Object getCellValue(Cell cell, FormulaEvaluator evaluator) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                try {
                    CellValue cellValue = evaluator.evaluate(cell);
                    if (cellValue == null) {
                        return "";
                    }

                    switch (cellValue.getCellType()) {
                        case NUMERIC:
                            return cellValue.getNumberValue();
                        case STRING:
                            return cellValue.getStringValue();
                        case BOOLEAN:
                            return cellValue.getBooleanValue();
                        case ERROR:
                            return "#ERROR";
                        default:
                            return "";
                    }
                } catch (Exception e) {
                    // 如果公式计算失败，尝试返回缓存值
                    try {
                        return getFallbackCellValue(cell);
                    } catch (Exception ex) {
                        return "#ERROR";
                    }
                }
            default:
                return "";
        }
    }

    private Object getFallbackCellValue(Cell cell) {
        // 尝试从单元格获取已缓存的值
        switch (cell.getCachedFormulaResultType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                return cell.getStringCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            default:
                return "";
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        SessionInfo sessionInfo = sessionMap.remove(sessionId);

        if (sessionInfo != null) {
            // 会话结束时持久化数据
            workbookCacheManager.cleanupSession(sessionId);
            log.info("WebSocket连接关闭，会话ID: {}", sessionId);
        }

        super.afterConnectionClosed(session, status);
    }

    /**
     * 从会话中提取文件名
     */
    private String getFileNameFromSession(WebSocketSession session) {
        // 从URL参数中获取文件名
        String query = session.getUri().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("fileName=")) {
                    return param.substring(9); // "fileName=".length()
                }
            }
        }
        return null;
    }
}
