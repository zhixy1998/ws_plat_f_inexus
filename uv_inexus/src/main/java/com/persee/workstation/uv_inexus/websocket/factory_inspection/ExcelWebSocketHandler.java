package com.persee.workstation.uv_inexus.websocket.factory_inspection;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.component.WorkbookCacheManager;
import com.persee.workstation.uv_inexus.dto.factory_inspection.*;
import com.persee.workstation.uv_inexus.po.factory_inspection.*;
import com.persee.workstation.uv_inexus.service.factory_inspection.*;
import com.persee.workstation.uv_inexus.util.*;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.MessageUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.*;

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
    @Autowired
    private TaskExecutionDetailService detailService;
    @Autowired
    private CalArtifactsService calArtifactsService;
    @Autowired
    private CalStdCatsService catsService;
    @Autowired
    private ExcelCellComparator excelCellComparator;
    @Autowired
    private DevMessageUtil devMessageUtil;
    @Autowired
    private MessageUtil messageUtil;
    @Autowired
    private ExcelDataAssetService dataAssetService;
    @Autowired
    private TaskManagementService taskService;

    // 会话信息类
    @Data
    private static class SessionInfo {
        private String fileName;
        private String sessionId;
        private long connectTime;
    }

    /**
     * 从会话URL中提取taskId参数
     */
    private String getTaskIdFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("taskId=")) {
                    return param.substring(7); // "taskId=".length()
                }
            }
        }
        return null;
    }

    private String getUpdateByFromUrl(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query != null) {
                MultiValueMap<String, String> parameters =
                        UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams();
                List<String> userIdList = parameters.get("updateBy");
                if (userIdList != null && !userIdList.isEmpty()) {
                    return userIdList.get(0);
                }
            }
        } catch (Exception e) {
            log.warn("解析URL参数失败", e);
        }
        return null;
    }


    //这里的文件名应该是对应着使用模板的文件名  也就是说结果模板名和使用模板名一致
    //注意这里说的模板名是模板code值
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            String sessionId = session.getId();
            // 从URL参数或握手信息中获取文件名
            String fileName = getFileNameFromSession(session);
            String taskId = getTaskIdFromSession(session);
            String updateBy = getUpdateByFromUrl(session);
            if (StringUtils.isBlank(fileName)) {
                TaskManagement management = taskService.getOne(new LambdaQueryWrapper<>(TaskManagement.class).eq(TaskManagement::getContentCategory, TEMPLATE_BUILT_IN_TASK.getCode()));
//                TaskManagement management = taskService.getById(taskId);
                fileName = management.getResultTemplate();
            }
            //初始进入检定页面
            if (StringUtils.equals(taskId, DEFAULT_TASK.getCode())) {
                LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(TaskManagement::getContentCategory, TEMPLATE_BUILT_IN_TASK.getCode());
                //这里查询出内置任务
                TaskManagement one = taskService.getOne(wrapper);
                taskId = one.getId();
                fileName = one.getResultTemplate();
            }
            boolean b = workbookCacheManager.acquireEditLock(taskId, updateBy);
            System.out.println("564645151545" + b);

            // 为会话创建专属Workbook
            Workbook workbook = workbookCacheManager.createTaskWorkbook(taskId, fileName);
            log.info("WebSocket连接建立，会话ID: {}, 文件: {}", sessionId, fileName);

            //如果是选择过标准物的任务，则需要修改工作簿的特征值和标准值
            LambdaQueryWrapper<TaskExecutionDetail> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TaskExecutionDetail::getTaskId, taskId);
            //这是修改过标准物的任务
            List<TaskExecutionDetail> list = detailService.list(queryWrapper);
            if (CollectionUtils.isNotEmpty(list)) {
                for (TaskExecutionDetail detail : list) {
                    String standardId = detail.getStandardId();
                    String itemId = detail.getItemId();
                    //看看这个 标准物是哪个检测项的
                    ExcelDataAsset item = dataAssetService.getById(itemId);
                    String data = item.getStructuredData();
                    ObjectMapper mapper = new ObjectMapper();
                    //获取到检测项的所有数据
                    Map<String, String> dataMap = mapper.readValue(data, new TypeReference<Map<String, String>>() {
                    });
                    String groupName = MapUtils.getString(dataMap, "检测类别");
                    String itemName = MapUtils.getString(dataMap, "项目名称");
                    //得到标准物质实体，可以进行修改了
                    String characteristicValue = "";
                    String standardValue = "";
                    CalArtifacts artifacts = calArtifactsService.getById(standardId);
                    if (artifacts == null) {
                        CalStdCats cats = catsService.getById(standardId);
                        characteristicValue = cats.getCharacteristicValue();
                        standardValue = cats.getStandardValue();
                    }
                    if (artifacts != null) {
                        characteristicValue = artifacts.getCharacteristicValue();
                        standardValue = artifacts.getStandardValue();
                    }
                    if (StringUtils.isNotBlank(characteristicValue)) {
                        CellUpdDto cellUpdDto = new CellUpdDto();
                        cellUpdDto.setCellValue(characteristicValue);
                        cellUpdDto.setSheetName(groupName);
                        cellUpdDto.setValueType(CHARACTERISTIC.getDescription());
                        cellUpdDto.setProjectName(itemName);
                        updateWorkbookCells(workbook, cellUpdDto);
                    }
                    if (StringUtils.isNotBlank(standardValue)) {
                        CellUpdDto cellUpdDto = new CellUpdDto();
                        cellUpdDto.setCellValue(standardValue);
                        cellUpdDto.setSheetName(groupName);
                        cellUpdDto.setValueType(STANDARD.getDescription());
                        cellUpdDto.setProjectName(itemName);
                        updateWorkbookCells(workbook, cellUpdDto);
                    }

                }
            }
            this.pushEntireWorkbook(session, taskId, fileName);
            super.afterConnectionEstablished(session);
        } catch (Exception e) {
            log.error("WebSocket连接建立失败", e);
        }
    }

    private void pushEntireWorkbook(WebSocketSession session, String taskId, String fileName) throws IOException {
        Workbook workbook = workbookCacheManager.getTaskWorkbook(taskId);
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

    //有问题还没改
    public void pushUpdatedData(WebSocketSession session, CellUpdDto cellUpdDto) throws IOException {
        String sessionId = session.getId();
        // 获取会话专属Workbook
        Workbook workbook = workbookCacheManager.getTaskWorkbook(sessionId);
        if (workbook != null) {
            // 执行更新操作
            updateWorkbookCell(workbook, cellUpdDto);

            // 更新缓存
            workbookCacheManager.updateTaskWorkbook(sessionId, workbook);

//            // 推送更新后的数据给前端
            Sheet sheet = workbook.getSheet(cellUpdDto.getSheetName());

            sendSheetUpdate(session, sheet);
            // 定期保存到文件  执行五次就保存一次
//            SessionInfo sessionInfo = sessionMap.get(sessionId);
//            if (shouldSaveToFile(sessionInfo.getFileName())) {
//                workbookCacheManager.saveWorkbookToFile(sessionInfo.getFileName(), workbook);
//            }
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
        String updateBy = root.get("updateBy").asText();
        String taskId = root.get("taskId").asText();

        if (CLOSE.getCode().equals(type)) {
            log.info("收到关闭指令");
            taskId = root.get("taskId").asText();
            workbookCacheManager.cleanupTask(taskId, updateBy);
            session.close();
        }
        if (SINGLE_ITEM.getCode().equals(type)) {
            log.info("收到单项测试指令");
            InspectionRequestDto request = mapper.readValue(payload, InspectionRequestDto.class);
            handleSingleItemInspection(session, request, taskId);
        }
        if (AUTO.getCode().equals(type)) {
            log.info("收到自动测试指令");
            AutoInspectionRequestDto autoRequest = mapper.readValue(payload, AutoInspectionRequestDto.class);
            handleAutoInspection(session, autoRequest.getItems());
        }
        if (SET_STANDARD.getCode().equals(type)) {
            /*
            * {
  "type": "2",  // 对应SET_STANDARD.getCode()
  "standardId": "std_001",  // 标准物质ID
  "itemId": "item_001",     // 检测项ID
  "standardType": "ZERO"    // 标准物质类型
}*/
            WebSocketMessageDto<StdMaterialSelectionDto> messageWrapper =
                    mapper.readValue(payload, new TypeReference<WebSocketMessageDto<StdMaterialSelectionDto>>() {
                    });
            StdMaterialSelectionDto selectionDto = messageWrapper.getData();
            taskId = selectionDto.getTaskId();
            String groupName = selectionDto.getGroupName();
            String itemName = selectionDto.getItemName();
            String standardId = selectionDto.getStandardId();
            String standardType = selectionDto.getStandardType();
            String itemId = selectionDto.getItemId();
            log.info("收到设置检测项: {} 的标准物指令", itemName);

            //判断任务是否正在被编辑中
            boolean edited = workbookCacheManager.isTaskBeingEditedByOthers(taskId, updateBy);
            if (edited) {
                session.sendMessage(new TextMessage("任务正在被编辑中"));
            }
            if (DEFAULT_TASK.getCode().equals(taskId)) {
                LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(TaskManagement::getContentCategory, TEMPLATE_BUILT_IN_TASK.getCode());
                TaskManagement one = taskService.getOne(wrapper);
                taskId = one.getId();
            }
            if (!edited) {

                //如果之前这个任务有设置过这个检测项的标准物，再次设置
                LambdaQueryWrapper<TaskExecutionDetail> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TaskExecutionDetail::getItemId, itemId)
                        .eq(TaskExecutionDetail::getStandardType, standardType)
                        .eq(TaskExecutionDetail::getTaskId, taskId);
                TaskExecutionDetail detail = new TaskExecutionDetail();

                boolean acquired = workbookCacheManager.acquireEditLock(taskId, updateBy);
                log.info("尝试获取编辑锁");

                if (acquired) {
                    log.info("获取编辑锁成功");
                }
                detail.setStandardId(standardId);
                boolean b = detailService.update(detail, queryWrapper);
                if (!b) {
                    //这个任务之前没有设置过标准物
                    detail.setId(IdGeneratorUtil.generateId());
                    detail.setTaskId(taskId);
                    detail.setItemId(itemId);
                    detail.setStandardId(standardId);
                    detail.setStandardType(standardType);
                    detailService.save(detail);
                }
                //如果是测量物质的话
                if (MEASUREMENT.getCode().equals(standardType)) {
                    log.info("正在设置测量标准物");
                    Workbook workbook = workbookCacheManager.getTaskWorkbook(taskId);
                    //得到标准物质实体，可以进行修改了
                    String characteristicValue = "";
                    String standardValue = "";
                    CalArtifacts artifacts = calArtifactsService.getById(standardId);
                    if (artifacts == null) {
                        CalStdCats cats = catsService.getById(standardId);
                        characteristicValue = cats.getCharacteristicValue();
                        standardValue = cats.getStandardValue();
                    }
                    if (artifacts != null) {
                        characteristicValue = artifacts.getCharacteristicValue();
                        standardValue = artifacts.getStandardValue();
                    }
                    if (StringUtils.isNotBlank(characteristicValue)) {
                        CellUpdDto cellUpdDto = new CellUpdDto();
                        cellUpdDto.setCellValue(characteristicValue);
                        cellUpdDto.setSheetName(groupName);
                        cellUpdDto.setValueType(CHARACTERISTIC.getDescription());
                        cellUpdDto.setProjectName(itemName);
                        updateWorkbookCells(workbook, cellUpdDto);
                    }
                    if (StringUtils.isNotBlank(standardValue)) {
                        CellUpdDto cellUpdDto = new CellUpdDto();
                        cellUpdDto.setCellValue(standardValue);
                        cellUpdDto.setSheetName(groupName);
                        cellUpdDto.setValueType(STANDARD.getDescription());
                        cellUpdDto.setProjectName(itemName);
                        updateWorkbookCells(workbook, cellUpdDto);
                    }
//                List<CellDifference> list = excelCellComparator.compareWorkbooks(workbook, workbookCacheManager.getTaskWorkbook(taskId));
//                if (!CollectionUtils.isEmpty(list)) {
//                    for (CellDifference difference : list) {
//                        System.out.println(difference);
//                    }
//                }
                    workbookCacheManager.updateTaskWorkbook(taskId, workbook);
                    sendSheetUpdate(session, workbook.getSheet(groupName));
                }
            }
            log.info("检测项标准物设置指令处理完毕 - 数据更新成功, 任务ID: {}, 检测项: {}", taskId, itemName);
        }
        //根据前端传输的内容进行处理  自动测试就按照模板进行测
        //单项测试就按照用户传过来的测试项id按照模板进行测试
        //后续得加上从机器获取数据，然后填充到实体类，进行修改并推送到前端
//        CellUpdDto cellUpdDto = new CellUpdDto();
//        cellUpdDto.setPosition(0);
//        cellUpdDto.setCellValue("215");
//        cellUpdDto.setFileName("TEMP_1765349747321");
//        cellUpdDto.setSheetName("透射比示值误差与重复性校验");
//        cellUpdDto.setValueType("测量值");
//        cellUpdDto.setProjectName("紫外分光光度计标准物质GBW(E)130066");
//        pushUpdatedData(session, cellUpdDto);
        super.handleTextMessage(session, message);

    }

    private void handleAutoInspection(WebSocketSession session, List<InspectionRequestDto> items) {

    }

    private void handleSingleItemInspection(WebSocketSession session, InspectionRequestDto request, String taskId) {
        //先做暗电流
        //在做校零
        //再做测量
        try {
            //获取检测项id
            String itemId = request.getItemId();
            ExcelDataAsset asset = dataAssetService.getById(itemId);
            //获取检测项的具体内容
            Map<String, Object> map = (Map<String, Object>) JSON.parse(asset.getStructuredData());
            //重复检测次数
            Integer repeatCount = MapUtils.getInteger(map, REPEAT_COUNT.getDescription());
            //检测项类别
            String category = MapUtils.getString(map, TEST_CATEGORY.getDescription());
            //
//            String measurementValue = MapUtils.getString(map,MEASUREMENT_VALUE.getDescription());
            //项目名称
            String projectName = MapUtils.getString(map,PROJECT_NAME.getDescription());
            //时间扫描
//            sendBatchDataPoints(session,"");
            for (Integer i = 0; i < repeatCount; i++) {
                //光谱扫描
                sendBatchDataPoints(session, "spectrum");
                Workbook workbook = workbookCacheManager.getTaskWorkbook(taskId);
                //发送sheet图，也就是绘制一幅波形图就要发送一个测量值给前端
                CellUpdDto cellUpdDto = new CellUpdDto();
                Random random = new Random();
                int result = random.nextInt(301) + 300; // 生成300-600的随机整数

                cellUpdDto.setPosition(i);
                cellUpdDto.setCellValue(String.valueOf(result));
                cellUpdDto.setSheetName(category);
                cellUpdDto.setValueType(MEASUREMENT_VALUE.getDescription());
                cellUpdDto.setProjectName(projectName);
                //修改工作簿
                updateWorkbookCell(workbook, cellUpdDto);
                //修改缓存中的工作簿
                workbookCacheManager.updateTaskWorkbook(taskId, workbook);
                //发送特定的sheet页
                sendSheetUpdate(session,workbook.getSheet(category));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendDataPoint(WebSocketSession session, BigDecimal xValue, BigDecimal yValue,
                               String sampleId, String waveLength, String yType, Integer digit) throws IOException {
        JSONObject payload = new JSONObject();
        JSONArray points = new JSONArray();
        JSONObject point = new JSONObject();

        point.put("x", xValue);

        Double decimalY = yValue.doubleValue();
        Double value = DataCountUtil.formatValue(digit, decimalY);
        String valueY = DataCountUtil.addZero(value, digit);

        point.put("y", decimalY);
        point.put("roundOffY", valueY);
        point.put("nm", waveLength);
        point.put("xType", "s");
        point.put("yType", yType);

        points.add(point);
        payload.put("data", points);
        payload.put("sampleId", sampleId);
        session.sendMessage(new TextMessage(payload.toJSONString()));
    }

    private void sendBatchDataPoints(WebSocketSession session, String scanType) throws IOException, InterruptedException {
        int totalPoints = 500;
        String xType = "s"; // 默认为时间扫描
        String yType = "Es"; // 默认y类型
        String waveLength = "660nm"; // 默认波长
        Integer digit = 4; // 默认小数位数

        // 根据扫描类型设置参数
        if ("spectrum".equals(scanType)) {
            xType = "nm";
            yType = "Er";
            waveLength = "496nm";
        }

        for (int i = 0; i < totalPoints; i++) {
            // 生成模拟数据
            BigDecimal xValue = new BigDecimal(i + 1); // 从1开始递增
            BigDecimal yValue = generateSimulatedYValue(i); // 生成模拟y值

            // 发送单个数据点
            JSONObject payload = new JSONObject();
            JSONArray points = new JSONArray();
            JSONObject point = new JSONObject();

            point.put("x", xValue);

            Double decimalY = yValue.doubleValue();
            Double value = DataCountUtil.formatValue(digit, decimalY);
            String valueY = DataCountUtil.addZero(value, digit);

            point.put("y", decimalY);
            point.put("roundOffY", valueY);
            point.put("nm", waveLength);
            point.put("xType", xType);
            point.put("yType", yType);

            points.add(point);
            payload.put("data", points);
            payload.put("sampleId", "");

            session.sendMessage(new TextMessage(payload.toJSONString()));

            // 间隔1秒发送下一个数据点
//            Thread.sleep(1000);
        }
    }

    // 生成模拟Y值的方法
    private BigDecimal generateSimulatedYValue(int index) {
        // 使用正弦波生成模拟数据，模拟实际测量值
        double baseValue = Math.sin(index * 0.1) * 10 + 50; // 基础正弦波
        double noise = (Math.random() - 0.5) * 2; // 添加随机噪声
        double finalValue = baseValue + noise;
        return new BigDecimal(finalValue);
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

    private void updateWorkbookCells(Workbook workbook, CellUpdDto cellUpdDto) {
        Sheet sheet = workbook.getSheet(cellUpdDto.getSheetName());

        // 先更新单元格值
        templateService.updateCellValues(sheet, excelCellLocator, excelCellEditor,
                cellUpdDto.getValueType(), cellUpdDto.getProjectName(),
                cellUpdDto.getCellValue());

        // 计算所有公式
        calculateFormulas(workbook);
    }


    /**
     * 发送Sheet更新数据（结构化传输）
     */
    private void sendSheetUpdate(WebSocketSession session, Sheet sheet) throws IOException {
        log.debug("推送Sheet更新数据");
        Workbook workbook = sheet.getWorkbook();

        // 构造完整的Sheet结构化数据
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("sheetName", sheet.getSheetName());
        updateData.put("sheetData", extractSheetData(sheet, workbook)); // 传递workbook用于公式计算

        ObjectMapper mapper = new ObjectMapper();
        String jsonData = mapper.writeValueAsString(updateData);
        session.sendMessage(new TextMessage(jsonData));
        log.debug("推送Sheet完毕");

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
            case "spectrum1":
                return 0;
            case "spectrum2":
                return 1;
            case "spectrum3":
                return 2;
            default:
                return 0;
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
        log.info("ExcelWebSocketHandler连接关闭，会话ID: {}", session.getId());
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
