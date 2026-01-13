package com.persee.workstation.uv_inexus.websocket.factory_inspection;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.util.DateUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.component.*;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.controller.guide.instrumentController.DeviceController;
import com.persee.workstation.uv_inexus.dao.factory_inspection.TaskExecutionDetailMapper;
import com.persee.workstation.uv_inexus.dto.factory_inspection.*;
import com.persee.workstation.uv_inexus.enums.T600ComIdEnums;
import com.persee.workstation.uv_inexus.enums.factoryInspection.AalysisMethodEnum;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.DeviceCalibrationInfoEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.EnergyLampEnums;
import com.persee.workstation.uv_inexus.enums.measurementEnums.ScanSpeedEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.exception.BusinessException;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.factory_inspection.*;
import com.persee.workstation.uv_inexus.service.factory_inspection.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemAlgorithmConfService;
import com.persee.workstation.uv_inexus.util.*;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.MessageUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.*;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.SetParam;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
import javafx.util.Pair;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.persee.workstation.uv_inexus.enums.Cache.CacheEnums.DEFAULT_CACHE;
import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.*;
import static com.persee.workstation.uv_inexus.util.deviceUtil.param.ActionCmdEnum.*;

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
    @Autowired
    private CalStdCatsService calStdCatsService;
    // 使用线程池执行独立的测量任务
    @Autowired
    @Qualifier("measurementTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;
    @Autowired
    private EhcacheUtil ehcacheUtil;
    //波形图持久化机制
    @Autowired
    private SpectrumDataService spectrumDataService;
    @Autowired
    private DevOperationUtilService devOperationUtilService;
    @Autowired
    private GetMaxDistanceUVTestTool getMaxDistanceUVTestTool;
    @Autowired
    private FindHalfPeakUVTestTool findHalfPeakUVTestTool;
    @Autowired
    private DeviceManageService deviceManageService;
    @Autowired
    private DeviceInfoService deviceInfoService;
    @Autowired
    private PythonPathConstant pythonPathConstant;
    @Autowired
    private DeviceController deviceController;
    @Autowired
    private TaskExecutionDetailMapper taskExecutionDetailMapper;
    @Autowired
    private SystemAlgorithmConfService systemAlgorithmConfService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private StandardMaterialManager materialManager;
    @Autowired
    private FindMaxOffsetUVTestTool findMaxOffsetUVTestTool;
    // 在类中添加缓存存储
    private final Map<String, List<JSONObject>> waveformCache = new ConcurrentHashMap<>();
    // 在类中添加任务管理Map
    private final Map<String, Future<Boolean>> taskFutures = new ConcurrentHashMap<>();

    // 测量状态管理
    private final Map<String, Boolean> measurementStatusMap = new ConcurrentHashMap<>();
    // 测量任务对应的session  key taskid value session
    private final Map<String, WebSocketSession> sessionTaskMap = new ConcurrentHashMap<>();

    // 心跳调度器
    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newScheduledThreadPool(5, r -> {
                Thread t = new Thread(r, "calibrationCheckWebSocket-Heartbeat-Thread");
                t.setDaemon(true);
                return t;
            });

    // 记录每个连接的最后活动时间
    private final Map<String, Long> lastActivityTime = new ConcurrentHashMap<>();
    // 记录心跳任务
    private final Map<String, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();

    private static final long HEARTBEAT_INTERVAL = 30000; // 30秒心跳间隔
    private static final long HEARTBEAT_TIMEOUT = 60000;   // 60秒超时


    // 在类中添加暂停控制类
    @Data
    public static class TaskPauseControl {
        private final Semaphore semaphore = new Semaphore(0);
        /*
          标识任务是否处于暂停状态
          true: 任务当前处于暂停状态
          false: 任务正常运行
        * */
        private volatile boolean paused = false;
    }

    // 在类中添加暂停控制映射
    private final Map<String, TaskPauseControl> pauseControls = new ConcurrentHashMap<>();


    //    //所有的任务和检测项的缓存
//    private static List<MultiLineSpectrumData> multiLineSpectrumDataList = new ArrayList<>();

    // 获取测量状态的方法
    public boolean isTaskMeasuring(String taskId) {
        return measurementStatusMap.getOrDefault(taskId, false);
    }

    // 修改 setTaskMeasuring 方法
    public void setTaskMeasuring(String taskId, boolean isMeasuring) {
        Boolean previousStatus = measurementStatusMap.get(taskId);
        measurementStatusMap.put(taskId, isMeasuring);

        // 只有当状态发生改变时才发送消息
        if (previousStatus == null || !previousStatus.equals(isMeasuring)) {
            sendMeasurementStatusToClient(taskId, isMeasuring);
        }
    }

    // 创建一个专门发送测量状态的方法
    private void sendMeasurementStatusToClient(String taskId, boolean isMeasuring) {
        WebSocketSession webSocketSession = sessionTaskMap.get(taskId);
        if (webSocketSession != null && webSocketSession.isOpen()) {
            JSONObject statusMessage = new JSONObject();
            statusMessage.put("isMeasuring", isMeasuring);

            try {
                webSocketSession.sendMessage(new TextMessage(statusMessage.toJSONString()));
                log.info("发送测量状态更新: taskId={}, isMeasuring={}", taskId, isMeasuring);
            } catch (IOException e) {
                log.error("发送测量状态失败: taskId={}", taskId, e);
            }
        } else {
            log.info("无法发送测量状态，会话不可用: taskId={}", taskId);
        }
    }


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
//                LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
//                wrapper.eq(TaskManagement::getId, taskId);
                //这里查询出内置任务
                TaskManagement management = taskService.getById(taskId);
                if (management != null) {
                    fileName = management.getResultTemplate();
                }
            }
            //初始进入检定页面
//            if (StringUtils.equals(taskId, DEFAULT_TASK.getCode())) {
//                LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
//                wrapper.eq(TaskManagement::getContentCategory, TEMPLATE_BUILT_IN_TASK.getCode())
//                        .eq(TaskManagement::getCompletionStatus, TASK_NOT_STARTED.getCode());
//                //这里查询出内置任务
//                List<TaskManagement> list = taskService.list(wrapper);
//                if(CollectionUtils.isNotEmpty(list)) {
//                    TaskManagement one = list.get(0);
//                    taskId = one.getId();
//                    fileName = one.getResultTemplate();
//                }
//            }
            boolean b = workbookCacheManager.acquireEditLock(taskId, updateBy);
//            System.out.println("564645151545" + b);

            // 为会话创建专属Workbook
            Workbook workbook = workbookCacheManager.createTaskWorkbook(taskId, fileName);
            log.info("WebSocket连接建立，会话ID: {}, 文件: {}", sessionId, fileName);
            //记录连接时间
            lastActivityTime.put(sessionId, System.currentTimeMillis());
            //这个任务有存活的session
            sessionTaskMap.put(taskId, session);
            JSONObject statusMessage = new JSONObject();
            statusMessage.put("isMeasuring", isTaskMeasuring(taskId));


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
                    Map<String, Object> dataMap = mapper.readValue(data, new TypeReference<Map<String, Object>>() {
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
            session.sendMessage(new TextMessage(statusMessage.toString()));
            // 发送缓存的波形图数据给新会话
            sendCachedWaveformData(session, taskId);
            // 启动心跳任务
            startHeartbeatTask(session);
            super.afterConnectionEstablished(session);
        } catch (Exception e) {
            log.error("WebSocket连接建立失败", e);
        }
    }

    private void startHeartbeatTask(WebSocketSession session) {
        String sessionId = session.getId();

        // 取消旧任务
        ScheduledFuture<?> oldTask = heartbeatTasks.get(sessionId);
        if (oldTask != null && !oldTask.isDone()) {
            oldTask.cancel(false);
        }

        // 创建新心跳任务
        ScheduledFuture<?> newTask = heartbeatScheduler.scheduleAtFixedRate(
                () -> sendHeartbeat(session),
                HEARTBEAT_INTERVAL,
                HEARTBEAT_INTERVAL,
                TimeUnit.MILLISECONDS
        );

        heartbeatTasks.put(sessionId, newTask);
    }

    private void sendHeartbeat(WebSocketSession session) {
        try {
            if (!session.isOpen()) {
                cancelHeartbeatTask(session.getId());
                return;
            }

            String sessionId = session.getId();
            long currentTime = System.currentTimeMillis();
            Long lastActivity = lastActivityTime.get(sessionId);

            // 检查连接超时
//            if (lastActivity != null && currentTime - lastActivity > HEARTBEAT_TIMEOUT) {
//                session.close();
//                cancelHeartbeatTask(sessionId);
//                return;
//            }

            // 发送心跳包
            session.sendMessage(new TextMessage("ping"));

        } catch (IOException e) {
            try {
                session.close();
            } catch (IOException ignored) {
            }
            cancelHeartbeatTask(session.getId());
        }
    }

    private void pushEntireWorkbook(WebSocketSession session, String taskId, String fileName) throws IOException {
        Workbook workbook = workbookCacheManager.getTaskWorkbook(taskId);

//        // 创建工作簿副本
//        XSSFWorkbook outputWorkbook = copyWorkbook(workbook);
//
//        // 从副本中移除Report页
//        this.removeSheetByName(outputWorkbook, "Report");

        WorkbookDto workbookDto = new WorkbookDto();
        workbookDto.setWorkbookName(fileName);
        workbookDto.setSheets(extractAllSheetsData(workbook));

        ObjectMapper mapper = new ObjectMapper();
        String jsonData = mapper.writeValueAsString(workbookDto);
        session.sendMessage(new TextMessage(jsonData));
    }

    private XSSFWorkbook copyWorkbook(Workbook originalWorkbook) throws IOException {
        // 将原工作簿写入字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        originalWorkbook.write(baos);
        baos.flush();

        // 从字节数组创建新的工作簿
        byte[] bytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

        return new XSSFWorkbook(bais);
    }

    /**
     * 删除指定名称的 Sheet 页
     *
     * @param workbook  Excel 工作簿对象
     * @param sheetName 要删除的 sheet 名称
     */
    public void removeSheetByName(Workbook workbook, String sheetName) {
        if (workbook == null || StringUtils.isBlank(sheetName)) {
            return;
        }

        // 获取所有 sheet 的索引
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet.getSheetName().equals(sheetName)) {
                workbook.removeSheetAt(i);
                break;
            }
        }
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
//            updateWorkbookCell(workbook, cellUpdDto);

            // 更新缓存
//            workbookCacheManager.updateTaskWorkbook(sessionId, workbook);

//            // 推送更新后的数据给前端
            Sheet sheet = workbook.getSheet(cellUpdDto.getSheetName());

//            sendSheetUpdate(session, sheet);
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
        sb.append(String.join("", Collections.nCopies(50, "="))).append("\n");


        for (int rowNum = sheet.getFirstRowNum(); rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row != null) {
                sb.append("Row ").append(rowNum).append(": ");
                for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
                    Cell cell = row.getCell(cellNum);
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

    private String getCellValueAsString(Cell cell) {
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
        // 更新最后活动时间
        lastActivityTime.put(session.getId(), System.currentTimeMillis());

        String payload = message.getPayload();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(payload);
        String type = root.get("type").asText();
        String updateBy = root.get("updateBy").asText();
        String taskId = root.get("taskId").asText();

        if (CLOSE.getCode().equals(type)) {
            log.info("收到停止指令");
            taskId = root.get("taskId").asText();
            R<String> device = deviceManageService.getConnectDevice(updateBy);
            String data = device.getData();
            DevOperationInputParam param = new DevOperationInputParam();
            param.setDeviceSerialNumber(data);
            param.setOperationType(OperationTypeEnums.Stop.getType());
            R<OperationResultEntity> stop = devOperationUtilService.operation(param);
            if (stop.getSuccess()) {
                log.info("停止成功");
            }
            setTaskMeasuring(taskId, false);
            //停止任务
            stopTask(taskId);
//            workbookCacheManager.cleanupTask(taskId, updateBy);
//            session.close();
        }
        if (SINGLE_ITEM.getCode().equals(type)) {

            //判断一下是单样品池还是多样品池

            log.info("收到单项测试指令");
            TaskManagement management = new TaskManagement();
            management.setId(taskId);
            management.setUpdateBy(updateBy);
            management.setStartTime(new Date());
            management.setCompletionStatus(IN_PROGRESS.getCode());
//            management.set
            taskService.updateById(management);
            InspectionRequestDto request = mapper.readValue(payload, InspectionRequestDto.class);
            handleSingleItemInspection(session, request, taskId, updateBy, false);
        }
        if (AUTO.getCode().equals(type)) {
            log.info("收到自动测试指令");
            TaskManagement management = new TaskManagement();
            management.setId(taskId);
            management.setUpdateBy(updateBy);
            management.setStartTime(new Date());
            management.setCompletionStatus(IN_PROGRESS.getCode());
//            management.set
            taskService.updateById(management);
            handleAutoInspection(session, taskId, updateBy);
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
                    workbookCacheManager.updateTaskWorkbook(taskId, workbook, updateBy);
                    sendSheetUpdate(session, workbook.getSheet(groupName), taskId);
                }
            }
            log.info("检测项标准物设置指令处理完毕 - 数据更新成功, 任务ID: {}, 检测项: {}", taskId, itemName);
        }
        //这里是查找之前的历史数据
        if (HISTORY_WAVEFORM.getCode().equals(type)) {
            log.info("收到查找历史波形图指令");
            WebSocketMessageDto<StdMaterialSelectionDto> messageWrapper =
                    mapper.readValue(payload, new TypeReference<WebSocketMessageDto<StdMaterialSelectionDto>>() {
                    });
            StdMaterialSelectionDto selectionDto = messageWrapper.getData();
            taskId = selectionDto.getTaskId();
            String itemId = selectionDto.getItemId();

//            String finalTaskId = taskId;
//            List<MultiLineSpectrumData> collect = multiLineSpectrumDataList.stream().filter(data -> data.getTaskId().equals(finalTaskId)).filter(data -> data.getItemId().equals(itemId)).collect(Collectors.toList());
//            String s = JSON.toJSONString(collect);
//            session.sendMessage(new TextMessage(s));
            // 从数据库查询历史数据
            List<MultiLineSpectrumData> historyData = spectrumDataService.findByTaskIdAndItemId(taskId, itemId);
            //只给4个历史波长，多了就不给了，避免超长
            if (historyData != null && historyData.size() > 4) {
                historyData = historyData.subList(0, 4);
            }
            String s = JSON.toJSONString(historyData);
            session.sendMessage(new TextMessage(s));
        }
        // 处理用户操作确认
        if (RESUME.getCode().equals(type)) { // 您定义的恢复命令类型
            resumeTask(taskId);
            JSONObject response = new JSONObject();
            response.put("resumed", true);
            session.sendMessage(new TextMessage(response.toJSONString()));
            return;
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

    private void handleAutoInspection(WebSocketSession session, String taskId, String updateBy) {
        InspectionRequestDto request = new InspectionRequestDto();
        TaskManagement management = taskService.getById(taskId);
        ExcelTemplate excelTemplate = templateService.getById(management.getUseTemplate());
        LambdaQueryWrapper<ExcelDataAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExcelDataAsset::getTemplateCode, excelTemplate.getTemplateCode());

        List<ExcelDataAsset> list = dataAssetService.list(wrapper);
        //得到所有的检测项
        if (CollectionUtils.isNotEmpty(list)) {
            for (ExcelDataAsset asset : list) {
                //告诉前端走到了哪个检测项
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("itemId", asset.getId());
                sendToActiveSession(null, taskId, jsonObject);


                request.setItemId(asset.getId());
                try {
                    sessionTaskMap.put(taskId, session);
                    this.handleSingleItemInspection(session, request, taskId, updateBy, true);

                    //同一个任务，同一时间只有一个检测项在跑
                    Future<Boolean> future = taskFutures.get(taskId);
                    if (future == null) {
                        log.error("检测项: {},检测失败，进行下一个检测项测量>>>>>>>>>", asset.getId());
                    }
//                    Future<?> future = taskFutures.get(taskId);
                    if (future != null) {
                        try {
                            // 设置合理的超时时间，避免无限等待
                            Boolean b = future.get();
                            JSONObject object = new JSONObject();
                            object.put("isCompleted", b);
                            sendToActiveSession(null, taskId, object);
                            if (b) {
                                log.info("检测项: {},检测成功，进行下一个检测项测量>>>>>>>>>", asset.getId());
                            } else {
                                log.error("检测项: {},检测失败，进行下一个检测项测量>>>>>>>>>", asset.getId());
                            }

                        } catch (Exception e) {
                            log.error("任务执行异常: taskId={}", taskId, e.getCause());
                            log.error("错误信息为{}", String.valueOf(e));
                            continue;
                        }
                    } else {
                        log.error("任务执行失败,future为空: taskId={}", taskId);
                    }

                } catch (Exception e) {
                    //检测项。。。跳过，进行下一项测量
                    log.error("检测项: {},检测失败，进行下一个检测项测量>>>>>>>>>", asset.getId(), e);
                }
            }
        }
    }

    // 停止特定任务
//    public void stopTask(String taskId) {
//        Future<?> future = taskFutures.get(taskId);
//        if (future != null && !future.isDone()) {
//            boolean cancel = future.cancel(true);
//
//            log.info("任务已取消: {}", cancel);

    /// /            log.info("4444444444444444444444444444" + future);
//            taskFutures.remove(taskId);
//        }
//    }

    @Data
    public static class MultiLineSpectrumData implements Serializable {
        private static final long serialVersionUID = 1L;
        private String taskId;           // 任务ID
        private String itemId;          // 检测项ID
        //key是时间  value是线的数据
        private Map<String, List<List<JSONObject>>> data = new HashMap<>();

        public void setData(List<List<JSONObject>> lines) {
            data.putIfAbsent(DateUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"), lines);
        }

        public void setData(Map<String, List<List<JSONObject>>> data) {
            this.data = data;
        }
    }

//    /**
//     * 暂停任务
//     */
//    public void pauseTask(String taskId) {
//        TaskPauseControl control = pauseControls.get(taskId);
//        if (control != null) {
//            control.setPaused(true);
//            log.info("任务 {} 已标记为暂停", taskId);
//
//            // 发送暂停状态给前端
//            WebSocketSession webSocketSession = sessionTaskMap.get(taskId);
//            if (webSocketSession != null && webSocketSession.isOpen()) {
//                JSONObject statusMessage = new JSONObject();
//                statusMessage.put("isPaused", true);
//                try {
//                    webSocketSession.sendMessage(new TextMessage(statusMessage.toJSONString()));
//                    log.info("发送暂停状态: taskId={}", taskId);
//                } catch (IOException e) {
//                    log.error("发送暂停状态失败: taskId={}", taskId, e);
//                }
//            }
//        }
//    }

//    /**
//     * 恢复任务
//     */
//    public void resumeTask(String taskId) {
//        TaskPauseControl control = pauseControls.get(taskId);
//        if (control != null) {
//            control.setPaused(false);
//            // 释放一个信号量，让任务继续执行
//            control.semaphore.release();
//            log.info("任务 {} 已恢复", taskId);
//
//            // 发送恢复状态给前端
//            WebSocketSession webSocketSession = sessionTaskMap.get(taskId);
//            if (webSocketSession != null && webSocketSession.isOpen()) {
//                JSONObject statusMessage = new JSONObject();
//                statusMessage.put("isPaused", false);
//                statusMessage.put("isMeasuring", true); // 恢复后仍处于测量状态
//                try {
//                    webSocketSession.sendMessage(new TextMessage(statusMessage.toJSONString()));
//                    log.info("发送恢复状态: taskId={}", taskId);
//                } catch (IOException e) {
//                    log.error("发送恢复状态失败: taskId={}", taskId, e);
//                }
//            }
//        }
//    }

    /**
     * 暂停任务
     */
    public void pauseTask(String taskId) {
        TaskPauseControl control = pauseControls.get(taskId);
        if (control != null) {
            control.paused = true;
            try {
                control.semaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            log.info("任务 {} 已标记为暂停", taskId);
        }
    }

    /**
     * 恢复任务
     */
    public void resumeTask(String taskId) {
        TaskPauseControl control = pauseControls.get(taskId);
        if (control != null) {
            control.paused = false;
            // 释放一个信号量，让任务继续执行
            control.semaphore.release();
            log.info("任务 {} 已恢复", taskId);
        }
    }

    /**
     * 停止任务
     */
    public void stopTask(String taskId) {
        TaskPauseControl control = pauseControls.get(taskId);
        if (control != null) {
            control.setPaused(false);
            control.semaphore.release(); // 唤醒等待的任务
            pauseControls.remove(taskId); // 清理控制对象
        }

        Future<?> future = taskFutures.get(taskId);
        if (future != null && !future.isDone()) {
            boolean cancel = future.cancel(true);
            log.info("任务已取消: {}", cancel);
            taskFutures.remove(taskId);
        }

        // 更新测量状态
        setTaskMeasuring(taskId, false);
        log.info("任务 {} 已停止", taskId);


//        Future<?> future = taskFutures.get(taskId);
//        if (future != null && !future.isDone()) {
//            boolean cancel = future.cancel(true);
//
//            log.info("任务已取消: {}", cancel);
////            log.info("4444444444444444444444444444" + future);
//            taskFutures.remove(taskId);
//        }
    }

    private void handleSingleItemInspection(WebSocketSession session, InspectionRequestDto request, String taskId, String updateBy, Boolean isAutoInspection) {
        //先做暗电流
        //在做校零
        //再做测量
        try {
            if (isAutoInspection) {
                //自动测试
                log.info("自动测试，开始测试>>>>>>>>>>>>>>>");
            }
            Future<Boolean> submit = taskExecutor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    // 获取或创建暂停控制对象
                    TaskPauseControl control = pauseControls.computeIfAbsent(taskId, k -> new TaskPauseControl());
                    //这个项的执行结果
                    boolean futureResult = false;
                    //正在测量
                    setTaskMeasuring(taskId, true);
                    //获取检测项id
                    String itemId = request.getItemId();
                    //检测物id
                    String matId = request.getMatId();
                    //获取他的标准值
                    String characteristicValue = "";
//                CalStdCats cats = calStdCatsService.getById(matId);
//                if (cats == null) {
//                    CalArtifacts calArtifacts = calArtifactsService.getById(matId);
//                    if (calArtifacts != null) {
//                        characteristicValue = calArtifacts.getCharacteristicValue();
//                    }
//                } else {
//                    characteristicValue = cats.getCharacteristicValue();
//                }
                    ExcelDataAsset dataAsset = dataAssetService.getById(itemId);
                    Map<String, Object> dataMap = dataAsset.getDataMap();
                    //检测类别，也就是sheet页的名称
                    String category = MapUtils.getString(dataMap, TEST_CATEGORY.getDescription());
                    //检测项名称
                    String projectName = MapUtils.getString(dataMap, PROJECT_NAME.getDescription());
                    //获取工作簿
                    Workbook taskWorkbook = workbookCacheManager.getTaskWorkbook(taskId);
                    Sheet sheet = taskWorkbook.getSheet(category);
                    List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, "特征值", projectName);
                    List<String> list = new ArrayList<>();

                    if (positions != null && !positions.isEmpty()) {
                        for (ExcelCellLocator.CellPosition position : positions) {
                            if (StringUtils.isNotBlank(position.getValue())) {
                                list.add(position.getValue());
                            }
                        }
                    }
//                if (!StringUtils.isBlank(characteristicValue)) {
//                    //没有空值的特征值列表
//                    list = Arrays.stream(characteristicValue.split(","))
//                            .filter(StringUtils::isNotBlank)
//                            .collect(Collectors.toList());
//                }
////
//                ExcelDataAsset asset = dataAssetService.getById(itemId);
//                //获取检测项的具体内容
//                Map<String, Object> map = (Map<String, Object>) JSON.parse(asset.getStructuredData());
                    //重复检测次数
                    Integer repeatCount = MapUtils.getInteger(dataMap, REPEAT_COUNT.getDescription());
//                //检测项类别
//                String category = MapUtils.getString(map, TEST_CATEGORY.getDescription());
//                //
////            String measurementValue = MapUtils.getString(map,MEASUREMENT_VALUE.getDescription());
//                //项目名称
//                String projectName = MapUtils.getString(map, PROJECT_NAME.getDescription());
                    //时间扫描
//            sendBatchDataPoints(session,"");
                    ArrayList<List<JSONObject>> lists = new ArrayList<>();
                    log.info("开始进行测量");
                    log.info("list:{}", list.size());
                    if (list != null) {
                        log.info("开始进行多特征值测量");


//                    String deviceSerialNumber = management.getOperationInstrument();
                        //更新任务对应的序列号为当前连接的仪器
                        R<String> device = deviceManageService.getConnectDevice(updateBy);

                        String deviceSerialNumber = device.getData();
                        TaskManagement management = taskService.getById(taskId);
                        //如果任务没有绑定设备号的时候，绑定第一次遇到的那个设备
                        if (management != null && StringUtils.isBlank(management.getOperationInstrument())) {
                            LambdaUpdateWrapper<TaskManagement> updateWrapper = new LambdaUpdateWrapper<>();
                            updateWrapper.eq(TaskManagement::getId, taskId)  // 根据taskId匹配
                                    .set(TaskManagement::getOperationInstrument, deviceSerialNumber); // 设置新序列号

                            // 执行更新
                            taskService.update(updateWrapper);
                        }
                        //获取具体的样品池信息
                        DeviceAttachmentTypeVO vo = deviceInfoService.getAttachmentInfo(deviceSerialNumber);
                        Integer samplePoolNum = vo.getSamplePoolNum();
                        if (samplePoolNum <= 0) {
                            throw new RuntimeException("样品池未识别");
                        }
                        if (samplePoolNum != 1) {
                            //样品池的检测
                            log.info("开始进行样品池的检测");
                            try {
                                samplePoolDetection(taskId, itemId);
                            } catch (Exception e) {
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("error", e.getMessage());
                                sendToActiveSession(null, taskId, jsonObject);
                                setTaskMeasuring(taskId, false);
                                throw new RuntimeException(e);
                            }
                        }
                        if (samplePoolNum == 1) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("notice", "单样品池，后续请根据提示放入相应物质!!!");
                            sendToActiveSession(null, taskId, jsonObject);
                        }

                        log.info("样品池检测完成");
                        log.info("测量次数：" + repeatCount);
                        //每个特征值测几次
                        for (Integer i = 0; i < repeatCount; i++) {
                            if (Thread.currentThread().isInterrupted()) {
                                log.info("线程中断");
                                try {
                                    throw new Exception("停止，线程中断！！！");
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            //光谱扫描
                            //测一条线的数据
//                            List<JSONObject> spectrum = sendBatchDataPoints(session, "spectrum", taskId);
                            Map<String, List<JSONObject>> map = null;
                            try {
                                Boolean needSetSit = deviceInfoService.isNeedSetSit(deviceSerialNumber);
                                //狭缝，光谱带宽
                                Double narrow = MapUtils.getDouble(dataMap, SPECTRAL_BANDWIDTH.getDescription());

                                log.info("是否为可变狭缝：{}", needSetSit);
                                log.info("开始进行扫描");
                                if (!needSetSit) {
                                    if (Double.compare(narrow, 2.0) != 0) {
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("error", "仪器为固定狭缝，此项跳过");
                                        sendToActiveSession(null, taskId, jsonObject);
                                        setTaskMeasuring(taskId, false);
                                        return futureResult;
                                    }
                                }
                                map = sendBatchDataPoints(session, taskId, itemId, dataMap, updateBy, deviceSerialNumber, list, samplePoolNum);
                            } catch (Exception e) {
                                setTaskMeasuring(taskId, false);
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("error", "测量失败");
                                sendToActiveSession(null, taskId, jsonObject);
                                throw new RuntimeException(e);
                            }
                            if (CollectionUtils.isNotEmpty(map.get("line"))) {
                                lists.add(map.get("line"));
                            }
                            Workbook workbook = workbookCacheManager.getTaskWorkbook(taskId);
                            //发送sheet图，也就是绘制一幅波形图就要发送一个测量值给前端
                            CellUpdDto cellUpdDto = new CellUpdDto();
                            Random random = new Random();

                            //把list按照特征值进行排序
//                        Collections.sort(list);
                            if (CollectionUtils.isNotEmpty(list)) {
                                //修改工作簿，需要把第一个测量值的列填满，然后第二个，第三个
                                for (int k = 0; k < list.size(); k++) {
                                    if (Thread.currentThread().isInterrupted()) {
                                        log.info("线程中断");
                                        try {
                                            throw new Exception("停止，线程中断！！！");
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                    cellUpdDto.setPosition(i);
                                    //测量值，后期替换这里的测量值，测量值是经过暗电流矫正，校零，测量得出的
//                                int result = random.nextInt(301) + 300; // 生成300-600的随机整数
                                    List<JSONObject> jsonObjects = map.get("res");

                                    if (CollectionUtils.isNotEmpty(jsonObjects)) {
                                        log.info("得到的测量结果为:{}", jsonObjects);
                                        ArrayList<String> strings = new ArrayList<>();
                                        jsonObjects.forEach(jsonObject -> {
                                            strings.add(jsonObject.getString("peak"));
                                        });
                                        String s = findClosestNumber(strings, list.get(k));
                                        cellUpdDto.setCellValue(s);
                                    }
                                    cellUpdDto.setSheetName(category);
                                    cellUpdDto.setValueType(MEASUREMENT_VALUE.getDescription());
                                    cellUpdDto.setProjectName(projectName);
                                    //设置他的特征值
                                    cellUpdDto.setFeatureValue(list.get(k));
                                    if (CollectionUtils.isNotEmpty(list)) {
                                        cellUpdDto.setIsMultiple(list.size() > 1);
                                    }
                                    log.info("修改工作簿");
                                    //修改工作簿
                                    updateWorkbookCell(workbook, cellUpdDto);
                                    log.info("修改完成");
                                }
                            } else {
                                cellUpdDto.setPosition(i);
                                //测量值，后期替换这里的测量值，测量值是经过暗电流矫正，校零，测量得出的
//                                int result = random.nextInt(301) + 300; // 生成300-600的随机整数
                                List<JSONObject> jsonObjects = map.get("res");

                                if (CollectionUtils.isNotEmpty(jsonObjects)) {
                                    cellUpdDto.setCellValue(jsonObjects.get(0).getString("peak"));
                                }
                                cellUpdDto.setSheetName(category);
                                cellUpdDto.setValueType(MEASUREMENT_VALUE.getDescription());
                                cellUpdDto.setProjectName(projectName);
                                //设置他的特征值
//                            cellUpdDto.setFeatureValue(list.get(0));
//                            if (CollectionUtils.isNotEmpty(list)) {
//                                cellUpdDto.setIsMultiple(list.size() > 1);
//                            }
//                            if (CollectionUtils.isNotEmpty(list)) {
//                                cellUpdDto.setIsMultiple(list.size() > 1);
//                            }
                                log.info("修改工作簿");
                                //修改工作簿
                                updateWorkbookCell(workbook, cellUpdDto);
                                log.info("修改完成");
                            }

                            try {
                                log.info("修改缓存中的工作簿，并异步更新数据库");
                                //修改缓存中的工作簿
                                workbookCacheManager.updateTaskWorkbook(taskId, workbook, updateBy);
                                //发送特定的sheet页
                                log.info("发送{},sheet页数据", category);
                                sendSheetUpdate(session, workbook.getSheet(category), taskId);
                                futureResult = true;
//                                return futureResult;
                                //结束测量
//                        setTaskMeasuring(taskId, false);
                            } catch (Exception e) {
                                log.error("发送sheet页数据失败", e);
                            }
                        }

                    } else {
                        log.info("开始进行单特征值测量");
                        //每个特征值测几次
                        for (Integer i = 0; i < repeatCount; i++) {
                            try {
                                //光谱扫描
                                //一条线的数据
                                List<JSONObject> spectrum = sendBatchDataPoints(session, "spectrum", taskId);
                                lists.add(spectrum);
                                Workbook workbook = workbookCacheManager.getTaskWorkbook(taskId);
                                //发送sheet图，也就是绘制一幅波形图就要发送一个测量值给前端
                                CellUpdDto cellUpdDto = new CellUpdDto();
                                Random random = new Random();
                                int result = random.nextInt(301) + 300; // 生成300-600的随机整数

                                TaskManagement management = taskService.getById(taskId);

                                String deviceSerialNumber = management.getOperationInstrument();
                                //构建校零入参
                                DevOperationInputParam param = new DevOperationInputParam();
                                param.setDeviceSerialNumber(deviceSerialNumber);
                                param.setAction(Zero);
                                R<OperationResultEntity> res =
                                        devOperationUtilService.operation(param);
                                log.info("校零结果1：{}", res);
                                //构建测量前置设置参数入参
                                DevOperationInputParam inputParam = new DevOperationInputParam();
                                inputParam.setDeviceSerialNumber(deviceSerialNumber);
                                SetParam setParam = new SetParam();
                                //先搞一波假数据，后续增加正式数据
                                setParam.setPhotoMode(2);
                                setParam.setScanSpeed(2);
                                setParam.setStartWl(660.6);
                                setParam.setEndWl(650.0);
                                setParam.setScanWlInterval(0.02);
                                setParam.setGain(4);
                                setParam.setSlit1(2.0);
                                setParam.setLampPos(1);

                                inputParam.setParam(setParam);
                                R<OperationResultEntity> resultParam =
                                        devOperationUtilService.operation(inputParam);
                                log.info("设置参数结果1：{}", resultParam);
                                if (!resultParam.getSuccess()) {
                                    log.info("设置参数出错");
                                    continue;
                                }
                                DevOperationInputParam param1 = new DevOperationInputParam();
                                param1.setDeviceSerialNumber(deviceSerialNumber);
                                param1.setAction(Scan);
                                R<OperationResultEntity> resultEntityR =
                                        devOperationUtilService.operation(param);
                                log.info("测量结果1：{}", resultEntityR);


                                cellUpdDto.setPosition(i);
                                cellUpdDto.setCellValue(String.valueOf(result));
                                cellUpdDto.setSheetName(category);
                                cellUpdDto.setValueType(MEASUREMENT_VALUE.getDescription());
                                cellUpdDto.setProjectName(projectName);
                                if (CollectionUtils.isNotEmpty(list)) {
                                    cellUpdDto.setIsMultiple(list.size() > 1);
                                }
                                //修改工作簿
                                updateWorkbookCell(workbook, cellUpdDto);
                                //修改缓存中的工作簿
                                workbookCacheManager.updateTaskWorkbook(taskId, workbook, updateBy);
                                //发送特定的sheet页

                                sendSheetUpdate(session, workbook.getSheet(category), taskId);
                                //结束测量
//                        setTaskMeasuring(taskId, false);
                            } catch (Exception e) {
                                log.error("发送sheet页数据失败", e);
                            }
                        }
                    }
                    // 修改数据存储部分
                    MultiLineSpectrumData data = new MultiLineSpectrumData();
                    data.setTaskId(taskId);
                    data.setItemId(itemId);
                    data.setData(lists);

                    // 将数据转换为数据库实体并保存
                    SpectrumData spectrumData = new SpectrumData();
                    spectrumData.setId(IdGeneratorUtil.generateIdWithPrefix("spec"));
                    spectrumData.setTaskId(taskId);
                    spectrumData.setItemId(itemId);
                    spectrumData.setSpectrumData(JSON.toJSONString(data.getData())); // 序列化数据
                    spectrumData.setMeasurementTime(new Date());
                    spectrumData.setCreateTime(new Date());

                    spectrumDataService.save(spectrumData);
                    setTaskMeasuring(taskId, false);
                    WebSocketSession webSocketSession = sessionTaskMap.get(taskId);
                    if (webSocketSession != null && webSocketSession.isOpen()) {
                        JSONObject statusMessage = new JSONObject();
                        statusMessage.put("isMeasuring", isTaskMeasuring(taskId));

                        try {
                            sendToActiveSession(webSocketSession, taskId, statusMessage);

//                            webSocketSession.sendMessage(new TextMessage(statusMessage.toJSONString()));
                        } catch (Exception e) {
                            log.error("发送测量状态失败", e);
                        }
                    }
                    return futureResult;
                }

            });


//            Future<Boolean> future = taskExecutor.submit(() -> {
//                //正在测量
//                setTaskMeasuring(taskId, true);
//                //获取检测项id
//                String itemId = request.getItemId();
//                //检测物id
//                String matId = request.getMatId();
//                //获取他的标准值
//                String characteristicValue = "";
////                CalStdCats cats = calStdCatsService.getById(matId);
////                if (cats == null) {
////                    CalArtifacts calArtifacts = calArtifactsService.getById(matId);
////                    if (calArtifacts != null) {
////                        characteristicValue = calArtifacts.getCharacteristicValue();
////                    }
////                } else {
////                    characteristicValue = cats.getCharacteristicValue();
////                }
//                ExcelDataAsset dataAsset = dataAssetService.getById(itemId);
//                Map<String, Object> dataMap = dataAsset.getDataMap();
//                //检测类别，也就是sheet页的名称
//                String category = MapUtils.getString(dataMap, TEST_CATEGORY.getDescription());
//                //检测项名称
//                String projectName = MapUtils.getString(dataMap, PROJECT_NAME.getDescription());
//                //获取工作簿
//                Workbook taskWorkbook = workbookCacheManager.getTaskWorkbook(taskId);
//                Sheet sheet = taskWorkbook.getSheet(category);
//                List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, "特征值", projectName);
//                List<String> list = new ArrayList<>();
//
//                if (positions != null && !positions.isEmpty()) {
//                    for (ExcelCellLocator.CellPosition position : positions) {
//                        if (StringUtils.isNotBlank(position.getValue())) {
//                            list.add(position.getValue());
//                        }
//                    }
//                }
////                if (!StringUtils.isBlank(characteristicValue)) {
////                    //没有空值的特征值列表
////                    list = Arrays.stream(characteristicValue.split(","))
////                            .filter(StringUtils::isNotBlank)
////                            .collect(Collectors.toList());
////                }
//////
////                ExcelDataAsset asset = dataAssetService.getById(itemId);
////                //获取检测项的具体内容
////                Map<String, Object> map = (Map<String, Object>) JSON.parse(asset.getStructuredData());
//                //重复检测次数
//                Integer repeatCount = MapUtils.getInteger(dataMap, REPEAT_COUNT.getDescription());
////                //检测项类别
////                String category = MapUtils.getString(map, TEST_CATEGORY.getDescription());
////                //
//////            String measurementValue = MapUtils.getString(map,MEASUREMENT_VALUE.getDescription());
////                //项目名称
////                String projectName = MapUtils.getString(map, PROJECT_NAME.getDescription());
//                //时间扫描
////            sendBatchDataPoints(session,"");
//                ArrayList<List<JSONObject>> lists = new ArrayList<>();
//                log.info("开始进行测量");
//                log.info("list:{}", list.size());
//                if (list != null) {
//                    log.info("开始进行多特征值测量");
//
//
////                    String deviceSerialNumber = management.getOperationInstrument();
//                    //更新任务对应的序列号为当前连接的仪器
//                    R<String> device = deviceManageService.getConnectDevice(updateBy);
//
//                    String deviceSerialNumber = device.getData();
//                    TaskManagement management = taskService.getById(taskId);
//                    //如果任务没有绑定设备号的时候，绑定第一次遇到的那个设备
//                    if (management != null && StringUtils.isBlank(management.getOperationInstrument())) {
//                        LambdaUpdateWrapper<TaskManagement> updateWrapper = new LambdaUpdateWrapper<>();
//                        updateWrapper.eq(TaskManagement::getId, taskId)  // 根据taskId匹配
//                                .set(TaskManagement::getOperationInstrument, deviceSerialNumber); // 设置新序列号
//
//                        // 执行更新
//                        taskService.update(updateWrapper);
//                    }
//                    //样品池的检测
//                    log.info("开始进行样品池的检测");
//                    try {
//                        samplePoolDetection(taskId, itemId);
//                    } catch (Exception e) {
//                        JSONObject jsonObject = new JSONObject();
//                        jsonObject.put("error", e.getMessage());
//                        sendToActiveSession(null,taskId,jsonObject);
//                        setTaskMeasuring(taskId, false);
//                        throw new RuntimeException(e);
//                    }
//                    log.info("样品池检测完成");
//                    log.info("测量次数："+repeatCount);
//                    //每个特征值测几次
//                    for (Integer i = 0; i < repeatCount; i++) {
//                        if (Thread.currentThread().isInterrupted()) {
//                            log.info("线程中断");
//                            try {
//                                throw new Exception("停止，线程中断！！！");
//                            } catch (Exception e) {
//                                throw new RuntimeException(e);
//                            }
//                        }
//
//                        //光谱扫描
//                        //测一条线的数据
////                            List<JSONObject> spectrum = sendBatchDataPoints(session, "spectrum", taskId);
//                        Map<String, List<JSONObject>> map = null;
//                        try {
//                            Boolean needSetSit = deviceInfoService.isNeedSetSit(deviceSerialNumber);
//                            log.info("是否为可变狭缝：{}", needSetSit);
//                            log.info("开始进行扫描");
//                            map = sendBatchDataPoints(session, taskId, itemId, dataMap, updateBy, deviceSerialNumber, list);
//                        } catch (Exception e) {
//                            setTaskMeasuring(taskId, false);
//                            JSONObject jsonObject = new JSONObject();
//                            jsonObject.put("error", "测量失败");
//                            sendToActiveSession(null,taskId,jsonObject);
//                            throw new RuntimeException(e);
//                        }
//                        if (CollectionUtils.isNotEmpty(map.get("line"))) {
//                            lists.add(map.get("line"));
//                        }
//                        Workbook workbook = workbookCacheManager.getTaskWorkbook(taskId);
//                        //发送sheet图，也就是绘制一幅波形图就要发送一个测量值给前端
//                        CellUpdDto cellUpdDto = new CellUpdDto();
//                        Random random = new Random();
//
//                        //把list按照特征值进行排序
////                        Collections.sort(list);
//                        if (CollectionUtils.isNotEmpty(list)) {
//                            //修改工作簿，需要把第一个测量值的列填满，然后第二个，第三个
//                            for (int k = 0; k < list.size(); k++) {
//                                if (Thread.currentThread().isInterrupted()) {
//                                    log.info("线程中断");
//                                    try {
//                                        throw new Exception("停止，线程中断！！！");
//                                    } catch (Exception e) {
//                                        throw new RuntimeException(e);
//                                    }
//                                }
//                                cellUpdDto.setPosition(i);
//                                //测量值，后期替换这里的测量值，测量值是经过暗电流矫正，校零，测量得出的
////                                int result = random.nextInt(301) + 300; // 生成300-600的随机整数
//                                List<JSONObject> jsonObjects = map.get("res");
//
//                                if (CollectionUtils.isNotEmpty(jsonObjects)) {
//                                    log.info("得到的测量结果为:{}", jsonObjects);
//                                    ArrayList<String> strings = new ArrayList<>();
//                                    jsonObjects.forEach(jsonObject -> {
//                                        strings.add(jsonObject.getString("peak"));
//                                    });
//                                    String s = findClosestNumber(strings, list.get(k));
//                                    cellUpdDto.setCellValue(s);
//                                }
//                                cellUpdDto.setSheetName(category);
//                                cellUpdDto.setValueType(MEASUREMENT_VALUE.getDescription());
//                                cellUpdDto.setProjectName(projectName);
//                                //设置他的特征值
//                                cellUpdDto.setFeatureValue(list.get(k));
//                                if (CollectionUtils.isNotEmpty(list)) {
//                                    cellUpdDto.setIsMultiple(list.size() > 1);
//                                }
//                                log.info("修改工作簿");
//                                //修改工作簿
//                                updateWorkbookCell(workbook, cellUpdDto);
//                                log.info("修改完成");
//                            }
//                        } else {
//                            cellUpdDto.setPosition(i);
//                            //测量值，后期替换这里的测量值，测量值是经过暗电流矫正，校零，测量得出的
////                                int result = random.nextInt(301) + 300; // 生成300-600的随机整数
//                            List<JSONObject> jsonObjects = map.get("res");
//
//                            if (CollectionUtils.isNotEmpty(jsonObjects)) {
//                                cellUpdDto.setCellValue(jsonObjects.get(0).getString("peak"));
//                            }
//                            cellUpdDto.setSheetName(category);
//                            cellUpdDto.setValueType(MEASUREMENT_VALUE.getDescription());
//                            cellUpdDto.setProjectName(projectName);
//                            //设置他的特征值
////                            cellUpdDto.setFeatureValue(list.get(0));
////                            if (CollectionUtils.isNotEmpty(list)) {
////                                cellUpdDto.setIsMultiple(list.size() > 1);
////                            }
////                            if (CollectionUtils.isNotEmpty(list)) {
////                                cellUpdDto.setIsMultiple(list.size() > 1);
////                            }
//                            log.info("修改工作簿");
//                            //修改工作簿
//                            updateWorkbookCell(workbook, cellUpdDto);
//                            log.info("修改完成");
//                        }
//
//                        try {
//                            log.info("修改缓存中的工作簿，并异步更新数据库");
//                            //修改缓存中的工作簿
//                            workbookCacheManager.updateTaskWorkbook(taskId, workbook, updateBy);
//                            //发送特定的sheet页
//                            log.info("发送{},sheet页数据", category);
//                            sendSheetUpdate(session, workbook.getSheet(category), taskId);
//                            //结束测量
////                        setTaskMeasuring(taskId, false);
//                        } catch (Exception e) {
//                            log.error("发送sheet页数据失败", e);
//                        }
//                    }
//
//                } else {
//                    log.info("开始进行单特征值测量");
//                    //每个特征值测几次
//                    for (Integer i = 0; i < repeatCount; i++) {
//                        try {
//                            //光谱扫描
//                            //一条线的数据
//                            List<JSONObject> spectrum = sendBatchDataPoints(session, "spectrum", taskId);
//                            lists.add(spectrum);
//                            Workbook workbook = workbookCacheManager.getTaskWorkbook(taskId);
//                            //发送sheet图，也就是绘制一幅波形图就要发送一个测量值给前端
//                            CellUpdDto cellUpdDto = new CellUpdDto();
//                            Random random = new Random();
//                            int result = random.nextInt(301) + 300; // 生成300-600的随机整数
//
//                            TaskManagement management = taskService.getById(taskId);
//
//                            String deviceSerialNumber = management.getOperationInstrument();
//                            //构建校零入参
//                            DevOperationInputParam param = new DevOperationInputParam();
//                            param.setDeviceSerialNumber(deviceSerialNumber);
//                            param.setAction(Zero);
//                            R<OperationResultEntity> res =
//                                    devOperationUtilService.operation(param);
//                            log.info("校零结果1：{}", res);
//                            //构建测量前置设置参数入参
//                            DevOperationInputParam inputParam = new DevOperationInputParam();
//                            inputParam.setDeviceSerialNumber(deviceSerialNumber);
//                            SetParam setParam = new SetParam();
//                            //先搞一波假数据，后续增加正式数据
//                            setParam.setPhotoMode(2);
//                            setParam.setScanSpeed(2);
//                            setParam.setStartWl(660.6);
//                            setParam.setEndWl(650.0);
//                            setParam.setScanWlInterval(0.02);
//                            setParam.setGain(4);
//                            setParam.setSlit1(2.0);
//                            setParam.setLampPos(1);
//
//                            inputParam.setParam(setParam);
//                            R<OperationResultEntity> resultParam =
//                                    devOperationUtilService.operation(inputParam);
//                            log.info("设置参数结果1：{}", resultParam);
//                            if (!resultParam.getSuccess()) {
//                                log.info("设置参数出错");
//                                continue;
//                            }
//                            DevOperationInputParam param1 = new DevOperationInputParam();
//                            param1.setDeviceSerialNumber(deviceSerialNumber);
//                            param1.setAction(Scan);
//                            R<OperationResultEntity> resultEntityR =
//                                    devOperationUtilService.operation(param);
//                            log.info("测量结果1：{}", resultEntityR);
//
//
//                            cellUpdDto.setPosition(i);
//                            cellUpdDto.setCellValue(String.valueOf(result));
//                            cellUpdDto.setSheetName(category);
//                            cellUpdDto.setValueType(MEASUREMENT_VALUE.getDescription());
//                            cellUpdDto.setProjectName(projectName);
//                            if (CollectionUtils.isNotEmpty(list)) {
//                                cellUpdDto.setIsMultiple(list.size() > 1);
//                            }
//                            //修改工作簿
//                            updateWorkbookCell(workbook, cellUpdDto);
//                            //修改缓存中的工作簿
//                            workbookCacheManager.updateTaskWorkbook(taskId, workbook, updateBy);
//                            //发送特定的sheet页
//
//                            sendSheetUpdate(session, workbook.getSheet(category), taskId);
//                            return;
//                            //结束测量
////                        setTaskMeasuring(taskId, false);
//                        } catch (Exception e) {
//                            log.error("发送sheet页数据失败", e);
//                        }
//                    }
//                }
//                // 修改数据存储部分
//                MultiLineSpectrumData data = new MultiLineSpectrumData();
//                data.setTaskId(taskId);
//                data.setItemId(itemId);
//                data.setData(lists);
//
//                // 将数据转换为数据库实体并保存
//                SpectrumData spectrumData = new SpectrumData();
//                spectrumData.setId(IdGeneratorUtil.generateIdWithPrefix("spec"));
//                spectrumData.setTaskId(taskId);
//                spectrumData.setItemId(itemId);
//                spectrumData.setSpectrumData(JSON.toJSONString(data.getData())); // 序列化数据
//                spectrumData.setMeasurementTime(new Date());
//                spectrumData.setCreateTime(new Date());
//
//                spectrumDataService.save(spectrumData);
//                setTaskMeasuring(taskId, false);
//                WebSocketSession webSocketSession = sessionTaskMap.get(taskId);
//                if (webSocketSession != null && webSocketSession.isOpen()) {
//                    JSONObject statusMessage = new JSONObject();
//                    statusMessage.put("isMeasuring", isTaskMeasuring(taskId));
//
//                    try {
//                        webSocketSession.sendMessage(new TextMessage(statusMessage.toJSONString()));
//                    } catch (IOException e) {
//                        log.error("发送测量状态失败", e);
//                    }
//                }
//            });
            // 保存任务引用，key为taskId
            taskFutures.put(taskId, submit);

        } catch (Exception e) {
            log.error("单项测量处理异常", e);
            setTaskMeasuring(taskId, false);
        }
    }

    //找出最接近的值
    public static String findClosestNumber(List<String> numbers, String targetStr) {
        if (numbers == null || numbers.isEmpty() || targetStr == null) {
            return null;
        }

        try {
            double target = Double.parseDouble(targetStr);
            String closest = null;
            double minDifference = Double.MAX_VALUE;

            for (String numberStr : numbers) {
                try {
                    double value = Double.parseDouble(numberStr);
                    double difference = Math.abs(value - target);

                    if (difference < minDifference) {
                        minDifference = difference;
                        closest = numberStr;
                    }
                } catch (NumberFormatException e) {
                    // 跳过无效数字字符串
                    continue;
                }
            }

            return closest;
        } catch (NumberFormatException e) {
            // 目标字符串无法解析为数字
            return null;
        }
    }

    private void sendBlackBlockNotFoundError(String taskId, String blackBlockDesc) {
        log.info("样品池中未找到：{}", blackBlockDesc);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("error", "样品池中未找到：" + blackBlockDesc);
        sendToActiveSession(null, taskId, jsonObject);
    }

    /**
     * 样品池检测
     */
    private void samplePoolDetection(String taskId, String itemId) throws BusinessException {

        //关于样品池
        Object cachedObject = ehcacheUtil.get(DEFAULT_CACHE.getCacheName(), taskId, String.class, Object.class);
        log.info("1111111111111111>>>>>>>>>>>>");
        List<SetSamplePoolDto> dtoList = new ArrayList<>();
        if (cachedObject instanceof List) {
            // 进行类型检查和转换
            dtoList = (List<SetSamplePoolDto>) cachedObject;
        }
        Map<String, String> standardMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(dtoList)) {
//                        List<TaskCheckItemDto.Standard> standardList = dtoList.stream()
//                                .map(SetSamplePoolDto::getStandard)
//                                .collect(Collectors.toList());
            standardMap = dtoList.stream()
                    .map(SetSamplePoolDto::getStandard)  // 获取 Standard 对象
                    .filter(Objects::nonNull)            // 过滤 null 值
                    .collect(Collectors.toMap(
                            TaskCheckItemDto.Standard::getStandardId,   // key: standardId
                            TaskCheckItemDto.Standard::getStandardName  // value: standardName
                    ));
        } else {
            throw new BusinessException("样品池为空");
        }

        log.info("222222222222222222>>>>>>>>>>>>");

        List<TaskExecutionDetail> detailList = taskExecutionDetailMapper.initTaskCal(taskId);
        if (detailList != null) {
            //获取当前检测项的所有标准物
            List<TaskExecutionDetail> collect = detailList.stream().filter(detail -> detail.getItemId().equals(itemId)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(collect)) {
                List<TaskExecutionDetail> measurementlist = collect.stream().filter(detail -> detail.getStandardType().equals(MEASUREMENT.getCode())).collect(Collectors.toList());
                List<TaskExecutionDetail> zerolist = collect.stream().filter(detail -> detail.getStandardType().equals(ZERO.getCode())).collect(Collectors.toList());

                //看一下需不需要暗电流矫正
                ExcelDataAsset asset = dataAssetService.getById(itemId);
                Map<String, Object> dataMap = asset.getDataMap();
                String s = MapUtils.getString(dataMap, LUMINOSITY_MODE.getDescription());
                // 优化后的代码
                WaveLengthEnum anEnum = WaveLengthEnum.getByName(s);
                if ((anEnum == WaveLengthEnum.abs || anEnum == WaveLengthEnum.tPercent)) {
                    String blackBlockDesc = BLACK_BLOCK.getDescription();

                    if (CollectionUtils.isNotEmpty(dtoList)) {
                        for (int i = 0; i < dtoList.size(); i++) {


                            String id = dtoList.get(i).getStandard().getStandardId();
                            if (id == null) return;

                            // 检查 CalStdCats
                            CalStdCats cats = calStdCatsService.getById(id);
                            if (cats != null && !cats.getName().contains(blackBlockDesc)) {
                                //如果是最后一次循环
                                if (i == dtoList.size() - 1) {
                                    sendBlackBlockNotFoundError(taskId, blackBlockDesc);
                                }
                            }

                            if (cats != null && cats.getName().contains(blackBlockDesc)) {
                                materialManager.addBlackBlockMaterial(taskId, id);
                                materialManager.setActionPosition(taskId, StandardMaterialManager.MaterialType.BLACK_BLOCK, dtoList.get(i).getPoolNum());
                                break;
                            }
                            // 检查 CalArtifacts
                            if (cats == null) {
                                CalArtifacts artifacts = calArtifactsService.getById(id);
                                if (artifacts != null) {
                                    String categoryId = artifacts.getCategoryId();
                                    CalStdCats categoryCats = catsService.getById(categoryId);
                                    if (categoryCats != null && !categoryCats.getName().contains(blackBlockDesc)) {
                                        if (i == dtoList.size() - 1) {
                                            sendBlackBlockNotFoundError(taskId, blackBlockDesc);
                                        }
                                    }
                                    if (categoryCats != null && categoryCats.getName().contains(blackBlockDesc)) {
                                        materialManager.addBlackBlockMaterial(taskId, id);
                                        materialManager.setActionPosition(taskId, StandardMaterialManager.MaterialType.BLACK_BLOCK, dtoList.get(i).getPoolNum());
                                        break;
                                    }
                                } else {
                                    log.warn("找不到ID为 {} 的标准物", id);
                                }
                            }
                        }

                    } else {
                        sendBlackBlockNotFoundError(taskId, blackBlockDesc);
                    }

                }

                log.info("333333333333333333>>>>>>>>>>>>");

                //
                if (!zerolist.isEmpty()) {
                    //校零的标准物
                    TaskExecutionDetail zero = zerolist.get(0);
                    //测量的标准物
                    if (!standardMap.containsKey(zero.getStandardId())) {
                        String standardName = "无标准物";
                        CalStdCats cats = catsService.getById(zero.getStandardId());
                        if (cats != null) {
                            standardName = cats.getName();
                        }
                        if (cats == null) {
                            CalArtifacts artifacts = calArtifactsService.getById(zero.getStandardId());
                            standardName = artifacts.getName();
                        }
                        log.info("样品池中未找到：" + standardName);
//                        JSONObject jsonObject = new JSONObject();
//                        jsonObject.put("error", "样品池中未找到：" + standardName);
//                        sendToActiveSession(null, taskId, jsonObject);
                        throw new BusinessException("样品池中未找到：" + standardName);
                    } else {
                        materialManager.addZeroMaterial(taskId, zero.getStandardId());
                        for (SetSamplePoolDto sample : dtoList) {
                            if (StringUtils.equals(sample.getStandard().getStandardId(), zero.getStandardId())) {
                                materialManager.setActionPosition(taskId, StandardMaterialManager.MaterialType.ZERO, sample.getPoolNum());
                                break;
                            }
                        }
                    }
                }
                log.info("4444444444444444444>>>>>>>>>>>>");

                if (!measurementlist.isEmpty()) {
                    //测量的标准物
                    TaskExecutionDetail measurement = measurementlist.get(0);
                    if (!standardMap.containsKey(measurement.getStandardId())) {
                        String standardName = "无标准物";
                        CalStdCats cats = catsService.getById(measurement.getStandardId());
                        if (cats != null) {
                            standardName = cats.getName();
                        }
                        if (cats == null) {
                            CalArtifacts artifacts = calArtifactsService.getById(measurement.getStandardId());
                            standardName = artifacts.getName();
                        }
                        log.info("样品池中未找到：" + standardName);
//                        JSONObject jsonObject = new JSONObject();
//                        jsonObject.put("error", "样品池中未找到：" + standardName);
//                        sendToActiveSession(null, taskId, jsonObject);
                        throw new BusinessException("样品池中未找到：" + standardName);
                    } else {
                        materialManager.addMeasureMaterial(taskId, measurement.getStandardId());
                        for (SetSamplePoolDto sample : dtoList) {
                            if (StringUtils.equals(sample.getStandard().getStandardId(), measurement.getStandardId())) {
                                materialManager.setActionPosition(taskId, StandardMaterialManager.MaterialType.MEASURE, sample.getPoolNum());
                                break;
                            }
                        }
                    }
                }
                log.info("555555555555555555>>>>>>>>>>>>");


            }
        }
    }

    public List<JSONObject> analysisProcess(List<Double> xList, List<Double> yList, List<Double> featurePoints, String methodType) {
        ArrayList<JSONObject> res = new ArrayList<>();
        AalysisMethodEnum anEnum = AalysisMethodEnum.getByMsg(methodType);
        if (anEnum != null) {
            log.info("分析方法是：{}", anEnum.getMsg());
            switch (anEnum) {
                case FIND_PEAK:
                    //寻峰找到点
                    SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
                    Map<String, Object> body1 = new HashMap<>();
                    body1.put("x_data", xList);
                    body1.put("y_data", yList);
                    double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
                    body1.put("height", height);
                    body1.put("distance", confVO.getPeakInterval());
                    log.info("寻峰入参：{}", body1);
                    Map<String, Object> resultMap;
                    try {
                        resultMap = callPythonService(
                                pythonPathConstant.getPeakSearching(),
                                body1
                        );
                        log.info("寻峰结果：{}", resultMap);
                        JSONObject jsonObject = new JSONObject();

                        List<Double> xPeak = (List<Double>) resultMap.get("peak_x");
                        List<String> yPeak = (List<String>) resultMap.get("peak_y");
                        for (int j = 0; j < xPeak.size(); j++) {
                            jsonObject.put("peak", xPeak.get(j).toString());
                            res.add(jsonObject);
                        }

                    } catch (RuntimeException e) {
                        log.error("Python服务调用异常：", e);
                    }
                    break;
                case FIND_HALF_PEAK_WIDTH:
                    //寻峰找到点
                    SystemAlgorithmConfVO confVO1 = systemAlgorithmConfService.queryAlgorithmConf();
                    Map<String, Object> body2 = new HashMap<>();
                    body2.put("x_data", xList);
                    body2.put("y_data", yList);
                    double height1 = Double.parseDouble(confVO1.getPeakValleyDiff()) / 100;
                    body2.put("height", height1);
                    body2.put("distance", confVO1.getPeakInterval());
                    log.info("寻峰入参：{}", body2);
                    Map<String, Object> resultMap1;
                    try {
                        resultMap1 = callPythonService(
                                pythonPathConstant.getPeakSearching(),
                                body2
                        );
                        log.info("寻峰结果：{}", resultMap1);
                        JSONObject jsonObject = new JSONObject();

                        List<Double> xPeak = (List<Double>) resultMap1.get("peak_x");
                        List<String> yPeak = (List<String>) resultMap1.get("peak_y");
                        for (int j = 0; j < xPeak.size(); j++) {
                            jsonObject.put("peak", xPeak.get(j).toString());
                            res.add(jsonObject);
                        }

                    } catch (RuntimeException e) {
                        log.error("Python服务调用异常：", e);
                    }
                    break;
                case FIND_MAX_OFFSET:
                    List<Double> calcResult = getMaxDistanceUVTestTool.calcResult(xList, yList, featurePoints);
                    if (CollectionUtils.isNotEmpty(calcResult)) {
                        for (Double aDouble : calcResult) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("peak", aDouble.toString());
                            res.add(jsonObject);
                        }
                    }
                case FIND_MAX_MIN_DIFFERENCE:
                    List<Double> doubles = findMaxOffsetUVTestTool.calcResult(xList, yList, featurePoints);
                    if (CollectionUtils.isNotEmpty(doubles)) {
                        for (Double aDouble : doubles) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("peak", aDouble.toString());
                            res.add(jsonObject);
                        }
                    }
                default:
                    break;
            }
        }

        return res;
    }

    /**
     * 光谱扫描
     *
     * @param
     * @param taskId
     * @param dataMap
     * @param deviceSerialNumber
     * @param stringList
     * @param itemId
     * @return
     * @throws Exception
     */

    //光谱扫描
    public Map<String, List<JSONObject>> spectrumScan(String taskId, Map<String, Object> dataMap, String deviceSerialNumber, List<String> stringList, boolean isMultiProductPool, String itemId) throws Exception {

        //图的原始list
        ArrayList<JSONObject> list = new ArrayList<>();
        //数值的list
        List<JSONObject> res = new ArrayList<>();
        //x轴list
        ArrayList<Double> xList = new ArrayList<>();
        //y轴list
        ArrayList<Double> yList = new ArrayList<>();
        HashMap<String, List<JSONObject>> map = new HashMap<>();

        //光度模式
        String luminosityMode = MapUtils.getString(dataMap, LUMINOSITY_MODE.getDescription());
        //分析方法
        String methodType = MapUtils.getString(dataMap, METHOD_TYPE.getDescription());
        //获取扫描速度
        String scanSpeed = MapUtils.getString(dataMap, SCAN_SPEED.getDescription());
        //获取起始波长
        Double startWavelength = MapUtils.getDouble(dataMap, START_WAVELENGTH.getDescription());
        //获取终止波长
        Double endWavelength = MapUtils.getDouble(dataMap, END_WAVELENGTH.getDescription());
        //扫描间隔
        Double scanInterval = MapUtils.getDouble(dataMap, SCAN_INTERVAL.getDescription());
        //增益
        Integer gain = MapUtils.getInteger(dataMap, GAIN.getDescription());
        //狭缝，光谱带宽
        Double narrow = MapUtils.getDouble(dataMap, SPECTRAL_BANDWIDTH.getDescription());
        //灯状态，灯位置
        Integer lampStatus = null;
        try {
            lampStatus = Integer.valueOf(Objects.requireNonNull(EnergyLampEnums.getIdentityByName(MapUtils.getString(dataMap, LAMP_STATUS.getDescription()))));
        } catch (Exception e) {
            log.error("无效的灯状态");
        }
        boolean b = false;
        WaveLengthEnum anEnum = WaveLengthEnum.getByName(luminosityMode);
        if ((anEnum == WaveLengthEnum.abs || anEnum == WaveLengthEnum.tPercent)) {

//        log.info("开始暗电流矫正>>>>>>>>>>>>>>>>>>>>");
            //切换样品池到黑挡块
            //看看是否为多样品池
            if (isMultiProductPool) {
                log.info("切换样品池到黑挡块-光谱>>>>>>>>>>>>>>>>>>>>");
                DevOperationInputParam operationInputParamDark = new DevOperationInputParam();
                operationInputParamDark.setDeviceSerialNumber(deviceSerialNumber);
                operationInputParamDark.setOperationType(OperationTypeEnums.SetParam.getType());
                SetParam sampleParamDark = new SetParam();
                Integer positionDark = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.BLACK_BLOCK);
                //设置样品池位置
                sampleParamDark.setCellPos(positionDark);
                operationInputParamDark.setParam(sampleParamDark);

                R<OperationResultEntity> operationDark1 = devOperationUtilService.operation(operationInputParamDark);
                if (operationDark1.getSuccess()) {
                    log.info("切换样品池到黑挡块成功-光谱>>>>>>>>>>>>>>>>");
                }
                if (!operationDark1.getSuccess()) {
                    setTaskMeasuring(taskId, false);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("error", operationDark1.getMessage());
                    sendToActiveSession(null, taskId, jsonObject);
                    stopTask(taskId);
                    return map;
                }
            } else {
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("notice","请放入黑挡块");
//                sendToActiveSession(null,taskId,jsonObject);
                promptUserToPlaceMaterial(taskId, "黑挡块");
            }
            log.info("开始暗电流矫正-光谱>>>>>>>>>>>>>>>>>>>>");
            DevOperationInputParam darkParam = new DevOperationInputParam();
            SetParam setParam1 = new SetParam();
            darkParam.setDeviceSerialNumber(deviceSerialNumber);
//        darkParam.setParam(setParam1);
            darkParam.setCalibrationType(DeviceCalibrationInfoEnum.Dark.getType());
            darkParam.setOperationType(OperationTypeEnums.Calibration.getType());
//        darkParam.setUpdateBy();
            R<OperationResultEntity> operationDark =
                    devOperationUtilService.operation(darkParam);
            b = operationDark.getSuccess();
            log.info("暗电流矫正结果-光谱：{}", operationDark);

        }

        //构建测量前置设置参数入参
        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        SetParam setParam = new SetParam();
        //先搞一波假数据，后续增加正式数据
        String code = WaveLengthEnum.getCode(luminosityMode);
        //设置光度模式
        setParam.setPhotoMode(Integer.valueOf(Objects.requireNonNull(code)));
        //设置扫描速度
        String speedCode = ScanSpeedEnum.getCodeByName(scanSpeed);
        setParam.setScanSpeed(Integer.valueOf(Objects.requireNonNull(speedCode)));
        //设置起始波长
        setParam.setStartWl(startWavelength);
        //换灯波长
//        setParam.setChangeLampWl(361.0);
        //设置终止波长
        setParam.setEndWl(endWavelength);
        //设置扫描间隔
        setParam.setScanWlInterval(scanInterval);
        //增益
        setParam.setGain(gain);
        //狭缝  光谱带宽
        setParam.setSlit1(narrow);
        //灯状态 灯位置
        setParam.setLampPos(lampStatus);
        //这里放起始波长
        setParam.setSetWl(startWavelength);
//                    inputParam.setOperationType(OperationTypeEnums.SetParam.getType());
        try {
            inputParam.setParam(setParam);
            inputParam.setOperationType(OperationTypeEnums.SetParam.getType());
//        param.setDeviceSerialNumber(deviceSerialNumber);

            log.info("入参：》》》》》》》" + inputParam);
            R<OperationResultEntity> opera =
                    devOperationUtilService.operation(inputParam);

//            resultParam = deviceController.setParam(inputParam);
            b = opera.getSuccess();
            if (!opera.getSuccess()) {
                log.warn("设备测量失败-光谱: {}, 错误: {}", deviceSerialNumber, opera.getMessage());
                setTaskMeasuring(taskId, false);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", "设置参数失败-光谱" + opera.getMessage());
                sendToActiveSession(null, taskId, jsonObject);
                throw new Exception("设置参数失败-光谱");
            }
        } catch (Exception e) {
            log.error("设置参数失败-光谱：{}", e.getMessage());
        }

//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        UnknowCountMessageQueue queue = MessageOnTimeDeal.unknowCountMessageQueue;
        //光度模式是abs,T%就基线,Es,Er就不用基线
        if (setParam.getPhotoMode() == 1 || setParam.getPhotoMode() == 2) {
            //构建基线入参


            if (b) {
//                Thread.sleep(5000);
                DevOperationInputParam inputParam1 = new DevOperationInputParam();
                inputParam1.setDeviceSerialNumber(deviceSerialNumber);
                inputParam1.setAction(BaseLine);

                inputParam1.setOperationType(OperationTypeEnums.ActionCmdEnum.getType());

                R<OperationResultEntity> operation =
                        devOperationUtilService.operation(inputParam1);
                b = operation.getSuccess();

                if (!operation.getSuccess()) {
                    log.warn("设备基线失败-光谱: {}, 错误: {}", deviceSerialNumber, operation.getMessage());
                    setTaskMeasuring(taskId, false);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("error", "设备基线失败-光谱");
                    sendToActiveSession(null, taskId, jsonObject);
                    stopTask(taskId);
                }

//            R<OperationResultEntity> action = deviceManageService.Action(param);
                long startTime = System.currentTimeMillis();
                while (true) {
                    if (Thread.currentThread().isInterrupted()) {
                        log.info("线程中断-光谱");
                        setTaskMeasuring(taskId, false);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("error", "停止");
                        sendToActiveSession(null, taskId, jsonObject);
                        throw new InterruptedException("任务被中断-光谱");
                    }
                    try {
                        Object o = queue.pollMessage(deviceSerialNumber, BaseLine.getCmdId());
                        if (o instanceof SpectrumPoint || o == null) {
                            SpectrumPoint point = (SpectrumPoint) o;
                            if (point != null) {
                                log.info("基线结果：{}", point);
                            }
                        } else if (o instanceof EndMarker) {
                            EndMarker marker = (EndMarker) o;
                            if (marker.getReason() == EndMarker.Reason.UN_NORMAL) {
                                log.error("基线数据获取错误：{}", o);
                                setTaskMeasuring(taskId, false);
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("error", "基线数据获取错误" + o);
                                sendToActiveSession(null, taskId, jsonObject);
                            }
                            break;
                        }
                    } catch (Exception e) {
                        log.error("基线数据获取错误：", e);
                    }
                }
            } else {
                log.warn("暗电流矫正失败-光谱: {}", deviceSerialNumber);
                setTaskMeasuring(taskId, false);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", "暗电流矫正失败-光谱");
                sendToActiveSession(null, taskId, jsonObject);
            }
        }
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        if (Thread.currentThread().isInterrupted()) {
            log.info("线程中断");
            setTaskMeasuring(taskId, false);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", "停止");
            sendToActiveSession(null, taskId, jsonObject);
            throw new InterruptedException("任务被中断");

        }
        if (b) {
            if (isMultiProductPool) {
                //切换样品池到扫描物质
                log.info("切换样品池到扫描物质-光谱>>>>>>>>>>>>>>>>>>>>");
                DevOperationInputParam operationInputParam = new DevOperationInputParam();
                operationInputParam.setDeviceSerialNumber(deviceSerialNumber);
                operationInputParam.setOperationType(OperationTypeEnums.SetParam.getType());
                SetParam sampleParam = new SetParam();
                Integer position = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.MEASURE);
                //设置样品池位置
                sampleParam.setCellPos(position);
                operationInputParam.setParam(sampleParam);

                R<OperationResultEntity> operation1 = devOperationUtilService.operation(operationInputParam);
                if (operation1.getSuccess()) {
                    log.info("切换样品池到测量物质成功-光谱>>>>>>>>>>>>>>>>");
                }
                b = operation1.getSuccess();
            } else {
                b=true;
                List<TaskExecutionDetail> detailList = taskExecutionDetailMapper.initTaskCal(taskId);
                //获取当前检测项的所有标准物
                List<TaskExecutionDetail> collect = detailList.stream().filter(detail -> detail.getItemId().equals(itemId)).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(collect)) {
                    String standardName="";
                    for (TaskExecutionDetail detail : collect) {
                        if (detail.getStandardType().equals(MEASUREMENT.getCode())){

                            CalStdCats cats = calStdCatsService.getById(detail.getStandardId());
                            if (cats==null){
                                CalArtifacts calArtifacts = calArtifactsService.getById(detail.getStandardId());
                                standardName=calArtifacts.getName();
                            }else {
                                standardName=cats.getName();
                            }
                        }
                    }
                    promptUserToPlaceMaterial(taskId, standardName);

                }else {
                    log.warn("当前检测项没有标准物");
                }
            }
            if (b) {
                //光谱扫描入参
                DevOperationInputParam param1 = new DevOperationInputParam();
                param1.setDeviceSerialNumber(deviceSerialNumber);
                param1.setAction(Scan);
//        param1.setOperationType(OperationTypeEnums.ActionCmdEnum.getType());

                param1.setOperationType(OperationTypeEnums.ActionCmdEnum.getType());

                R<OperationResultEntity> operation =
                        devOperationUtilService.operation(param1);
                log.info("光谱扫描方法返回：{}", operation);
                if (!operation.getSuccess()) {
                    log.warn("光谱扫描失败: {}, 错误: {}", deviceSerialNumber, operation.getMessage());
                    setTaskMeasuring(taskId, false);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("error", "光谱扫描失败");
                    sendToActiveSession(null, taskId, jsonObject);
                }
                int i = 0;
                while (true) {
                    try {
                        if (Thread.currentThread().isInterrupted()) {
                            log.info("线程中断");
                            setTaskMeasuring(taskId, false);
                            throw new InterruptedException("任务被中断");
                        }
                        Object o = queue.pollMessage(deviceSerialNumber, Scan.getCmdId());
                        if (o instanceof SpectrumPoint || o == null) {
                            SpectrumPoint p = (SpectrumPoint) o;
                            if (p != null) {
                                i++;
                                // 获取或创建波形图数据缓存
                                List<JSONObject> cachedDataPoints = waveformCache.computeIfAbsent(taskId, k -> new ArrayList<>());


                                getPointsByLuminosityMode(luminosityMode, p, xList, yList, taskId, i, cachedDataPoints, list, dataMap);
                                log.info("扫描结果：{}", p);
                            }
                        } else {
                            break;
                        }
                    } catch (Exception e) {
                        log.error("错误111：", e);
                        setTaskMeasuring(taskId, false);
                        throw new InterruptedException("任务被中断");
                    }
                }
            }
            if (!b) {
                log.warn("扫描物质切换失败-光谱: {}", deviceSerialNumber);
                setTaskMeasuring(taskId, false);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", "扫描物质切换失败-光谱");
                sendToActiveSession(null, taskId, jsonObject);
                stopTask(taskId);
            }
        } else {
            log.warn("光谱扫描失败-光谱: {}", deviceSerialNumber);
            setTaskMeasuring(taskId, false);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", "光谱扫描失败-光谱");
            sendToActiveSession(null, taskId, jsonObject);
            stopTask(taskId);
        }
        List<Double> doubleList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(stringList)) {
            for (String str : stringList) {
                if (str != null) {
                    try {
                        doubleList.add(Double.parseDouble(str));
                    } catch (NumberFormatException e) {
                        log.warn("无法解析为数字: {}", str);
                    }
                }
            }
        }

        res = analysisProcess(xList, yList, doubleList, methodType);
        if (Thread.currentThread().isInterrupted()) {
            log.info("线程中断");
            throw new InterruptedException("任务被中断");
        }
        map.put("line", list);
        map.put("res", res);
        return map;
    }

    /**
     * 提示用户放入物质并等待确认
     */
    private void promptUserToPlaceMaterial(String taskId, String stdMaterialName) {
        // 设置等待状态
        pauseTask(taskId);
        // 发送提示信息
        JSONObject noticeMsg = new JSONObject();
        noticeMsg.put("notice", "请放入 " + stdMaterialName);
        sendToActiveSession(null, taskId, noticeMsg);
    }

    //根据不同的光度模式得到点
    private void getPointsByLuminosityMode(String luminosityMode, SpectrumPoint p, List<Double> xList, List<Double> yList, String taskId, Integer i, List<JSONObject> cachedDataPoints, List<JSONObject> list, Map<String, Object> dataMap) {
        //获取枚举，看看是哪个
        WaveLengthEnum anEnum = WaveLengthEnum.getByName(luminosityMode);
        log.info("光度模式：{}", anEnum.getName());
//获取起始波长
        Double startWavelength = MapUtils.getDouble(dataMap, START_WAVELENGTH.getDescription());
        //获取终止波长
        Double endWavelength = MapUtils.getDouble(dataMap, END_WAVELENGTH.getDescription());
        //扫描间隔
        Double scanInterval = MapUtils.getDouble(dataMap, SCAN_INTERVAL.getDescription());
        Double scanCount = MapUtils.getDouble(dataMap, SCAN_COUNT.getDescription());
        if (anEnum == WaveLengthEnum.abs) {
            if (ObjectUtils.isNotEmpty(p)) {
                JSONObject payload = new JSONObject();
                JSONArray points = new JSONArray();
                JSONObject point = new JSONObject();

                if (p.getTimestamp() != null) {
                    payload.put("timestamp", p.getTimestamp());
                    payload.put("totalPoints", scanCount);
                    point.put("yType", "波长(nm)");
                    point.put("xType", "abs");
                    double wavelength = p.getAbsResult().getWavelength();
                    point.put("y", wavelength);
                    xList.add(wavelength);
                    Double aDouble = new Double(p.getAbsResult().getAbs());
                    point.put("x", aDouble);
                    yList.add(aDouble);
                } else {
                    payload.put("totalPoints", (startWavelength - endWavelength) / scanInterval);
                    point.put("xType", "波长(nm)");
                    point.put("yType", "abs");
                    double wavelength = p.getAbsResult().getWavelength();
                    point.put("x", wavelength);
                    xList.add(wavelength);
                    Double aDouble = new Double(p.getAbsResult().getAbs());
                    point.put("y", aDouble);
                    yList.add(aDouble);
                }
//                    point.put("roundOffY", valueY);
//                    point.put("nm", waveLength);


                points.add(point);
                payload.put("data", points);
                payload.put("sampleId", "");
                payload.put("taskId", taskId);
                payload.put("pointIndex", i);
//                payload.put("totalPoints", (startWavelength - endWavelength) / scanInterval);


                // 将数据点添加到缓存
                cachedDataPoints.add(payload);

                // 尝试发送到活跃session
                sendToActiveSession(null, taskId, payload);
                list.add(payload);
            }

        }
        if (anEnum == WaveLengthEnum.tPercent) {
            if (ObjectUtils.isNotEmpty(p)) {
                JSONObject payload = new JSONObject();
                JSONArray points = new JSONArray();
                JSONObject point = new JSONObject();
                if (p.getTimestamp() != null) {
                    payload.put("timestamp", p.getTimestamp());
                    payload.put("totalPoints", scanCount);
                    point.put("yType", "波长(nm)");
                    point.put("xType", "T%");
                    double wavelength = p.getAbsResult().getWavelength();
                    point.put("y", wavelength);
                    xList.add(wavelength);
                    Double aDouble = new Double(p.getAbsResult().getAbs());
                    point.put("x", aDouble);
                    yList.add(aDouble);
                } else {
                    payload.put("totalPoints", (startWavelength - endWavelength) / scanInterval);
                    point.put("xType", "波长(nm)");
                    point.put("yType", "T%");
                    double wavelength = p.getAbsResult().getWavelength();
                    point.put("x", wavelength);
                    xList.add(wavelength);
                    Double aDouble = new Double(p.getAbsResult().getAbs());
                    point.put("y", aDouble);
                    yList.add(aDouble);
                }
                points.add(point);
                payload.put("data", points);
                payload.put("sampleId", "");
                payload.put("taskId", taskId);
                payload.put("pointIndex", i);


                // 将数据点添加到缓存
                cachedDataPoints.add(payload);

                // 尝试发送到活跃session
                sendToActiveSession(null, taskId, payload);
                list.add(payload);
            }

        }
        if (anEnum == WaveLengthEnum.es) {
            if (ObjectUtils.isNotEmpty(p)) {
                JSONObject payload = new JSONObject();
                JSONArray points = new JSONArray();
                JSONObject point = new JSONObject();
                if (p.getTimestamp() != null) {
                    payload.put("timestamp", p.getTimestamp());
                    payload.put("totalPoints", scanCount);
                    point.put("yType", "波长(nm)");
                    point.put("xType", "ES");
                    double wavelength = p.getAbsResult().getWavelength();
                    point.put("y", wavelength);
                    xList.add(wavelength);
                    Double aDouble = new Double(p.getAbsResult().getAbs());
                    point.put("x", aDouble);
                    yList.add(aDouble);
                } else {
                    payload.put("totalPoints", (startWavelength - endWavelength) / scanInterval);
                    point.put("xType", "波长(nm)");
                    point.put("yType", "ES");
                    double wavelength = p.getAbsResult().getWavelength();
                    point.put("x", wavelength);
                    xList.add(wavelength);
                    Double aDouble = new Double(p.getAbsResult().getAbs());
                    point.put("y", aDouble);
                    yList.add(aDouble);
                }
                points.add(point);
                payload.put("data", points);
                payload.put("sampleId", "");
                payload.put("taskId", taskId);
                payload.put("pointIndex", i);
//                payload.put("totalPoints", (startWavelength - endWavelength) / scanInterval);

                // 将数据点添加到缓存
                cachedDataPoints.add(payload);

                // 尝试发送到活跃session
                sendToActiveSession(null, taskId, payload);
                list.add(payload);
            }

        }
        if (anEnum == WaveLengthEnum.er) {
            if (ObjectUtils.isNotEmpty(p)) {
                JSONObject payload = new JSONObject();
                JSONArray points = new JSONArray();
                JSONObject point = new JSONObject();
                if (p.getTimestamp() != null) {
                    payload.put("timestamp", p.getTimestamp());
                    payload.put("totalPoints", scanCount);
                    point.put("yType", "波长(nm)");
                    point.put("xType", "ER");
                    double wavelength = p.getAbsResult().getWavelength();
                    point.put("y", wavelength);
                    xList.add(wavelength);
                    Double aDouble = new Double(p.getAbsResult().getAbs());
                    point.put("x", aDouble);
                    yList.add(aDouble);
                } else {
                    payload.put("totalPoints", (startWavelength - endWavelength) / scanInterval);
                    point.put("xType", "波长(nm)");
                    point.put("yType", "ER");
                    double wavelength = p.getAbsResult().getWavelength();
                    point.put("x", wavelength);
                    xList.add(wavelength);
                    Double aDouble = new Double(p.getAbsResult().getAbs());
                    point.put("y", aDouble);
                    yList.add(aDouble);
                }
                points.add(point);
                payload.put("data", points);
                payload.put("sampleId", "");
                payload.put("taskId", taskId);
                payload.put("pointIndex", i);
//                payload.put("totalPoints", (startWavelength - endWavelength) / scanInterval);

                // 将数据点添加到缓存
                cachedDataPoints.add(payload);

                // 尝试发送到活跃session
                sendToActiveSession(null, taskId, payload);
                list.add(payload);
            }

        }
        List<JSONObject> put = waveformCache.put(taskId, cachedDataPoints);
    }

    /**
     * 发送错误信息给前端
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) throws IOException {
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("error", errorMessage);

        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(errorResponse.toJSONString()));
        }
    }


    /**
     * 调用Python服务
     *
     * @param url
     * @param requestBody
     * @return
     */
    private Map<String, Object> callPythonService(String url, Map<String, Object> requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            // 检查响应体是否为空
            if (responseBody == null) {
                throw new RuntimeException(YamlMessageSource.getMessageStatic("00000032")); // "算法服务返回异常"
            }

            Integer code = (Integer) responseBody.get("code");
            if (code == null || code != 200) {
                String message = (String) responseBody.get("message");
                String errorMsg = org.springframework.util.StringUtils.hasText(message) ? message : "未知错误";
                throw new RuntimeException(errorMsg);
            }

            return responseBody;

        } catch (ResourceAccessException ex) {
            throw new RuntimeException("算法服务异常,请重启服务");
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new RuntimeException("算法服务异常：" + ex.getStatusCode());
        } catch (Exception e) {
            // 兜底异常（包括上面 throw 的 RuntimeException）
            throw e;
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

    // 发送缓存的波形图数据
    private void sendCachedWaveformData(WebSocketSession session, String taskId) {
        List<JSONObject> cachedDataPoints = waveformCache.get(taskId);
        if (cachedDataPoints != null && !cachedDataPoints.isEmpty()) {
            log.info("发送缓存的波形图数据给新会话，共 {} 个数据点", cachedDataPoints.size());

            for (JSONObject payload : cachedDataPoints) {
                try {
                    if (session != null && session.isOpen()) {
                        session.sendMessage(new TextMessage(payload.toJSONString()));
                    }
                } catch (IOException e) {
                    log.error("发送缓存数据点失败: {}", e.getMessage());
                }
            }
        }
    }

    // 修改 sendBatchDataPoints 方法
    private List<JSONObject> sendBatchDataPoints(WebSocketSession originalSession, String scanType, String taskId) throws IOException, InterruptedException {
        ArrayList<JSONObject> list = new ArrayList<>();
        // 获取或创建波形图数据缓存
        List<JSONObject> cachedDataPoints = waveformCache.computeIfAbsent(taskId, k -> new ArrayList<>());


        int totalPoints = 20;
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

            // 构建数据点
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
            payload.put("taskId", taskId);
            payload.put("pointIndex", i);
            payload.put("totalPoints", totalPoints);

            // 将数据点添加到缓存
            cachedDataPoints.add(payload);

            // 尝试发送到活跃session
            sendToActiveSession(originalSession, taskId, payload);
            list.add(payload);
            // 间隔100毫秒发送下一个数据点
            Thread.sleep(100);
        }
        /*每一次的测量结果我都要按照任务-检测项id-测量日期这样的给存储起来，因为用户要看，如果用户*/
        WebSocketSession session = sessionTaskMap.get(taskId);
        JSONObject statusMessage = new JSONObject();
        statusMessage.put("finished", true);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(statusMessage.toJSONString()));
        }
        list.add(statusMessage);
        // 波形图完成时清理缓存（可选）
        waveformCache.remove(taskId);
        return list;
    }

    // 修改 sendBatchDataPoints 方法
    private Map<String, List<JSONObject>> sendBatchDataPoints(WebSocketSession originalSession, String taskId, String itemId, Map<String, Object> dataMap, String updateBy, String deviceSerialNumber, List<String> list, Integer samplePoolNum) throws Exception {
        boolean b = samplePoolNum > 1;
        //是不是光谱扫描
        String res = MapUtils.getString(dataMap, SPECTRAL_SCAN_TYPE.getDescription());
        //光度模式
        String luminosityMode = MapUtils.getString(dataMap, LUMINOSITY_MODE.getDescription());
        //获取扫描速度
        String scanSpeed = MapUtils.getString(dataMap, SCAN_SPEED.getDescription());
        //获取起始波长
        Double startWavelength = MapUtils.getDouble(dataMap, START_WAVELENGTH.getDescription());
        //获取终止波长
        Double endWavelength = MapUtils.getDouble(dataMap, END_WAVELENGTH.getDescription());
        //扫描间隔
        Integer scanInterval = MapUtils.getInteger(dataMap, SCAN_INTERVAL.getDescription());
        //时间间隔
        Integer timeInterval = MapUtils.getInteger(dataMap, TIME_INTERVAL.getDescription());
        //增益
        Integer gain = MapUtils.getInteger(dataMap, GAIN.getDescription());
        //狭缝，光谱带宽
        Double narrow = MapUtils.getDouble(dataMap, SPECTRAL_BANDWIDTH.getDescription());
        //灯状态，灯位置
        Integer lampStatus = MapUtils.getInteger(dataMap, LAMP_STATUS.getDescription());
        //扫描个数
        Integer scanCount = MapUtils.getInteger(dataMap, SCAN_COUNT.getDescription());
        //分析方法
        String methodType = MapUtils.getString(dataMap, METHOD_TYPE.getDescription());
        Map<String, List<JSONObject>> map = new HashMap<>();
        List<SpectrumPoint> spectrumPoints = new ArrayList<>();

        //如果是光谱扫描
        if (YES.getDescription().equals(res)) {
            log.info("光谱扫描>>>>>>>");
            map = spectrumScan(taskId, dataMap, deviceSerialNumber, list, b, itemId);
        } else if (scanCount != null) {
            log.info("时间扫描>>>>>");
            map = timeScan(taskId, dataMap, deviceSerialNumber, scanCount, luminosityMode,b,itemId);
        } else {
            log.info("光度扫描>>>>>>>>");
            map = intensityScan(taskId, dataMap, deviceSerialNumber,b,itemId);
        }

        /*每一次的测量结果我都要按照任务-检测项id-测量日期这样的给存储起来，因为用户要看，如果用户*/
        WebSocketSession session = sessionTaskMap.get(taskId);
        JSONObject statusMessage = new JSONObject();
        statusMessage.put("finished", true);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(statusMessage.toJSONString()));
            } catch (IOException e) {
                log.warn("发送失败: ", e);
            }
        }
        List<JSONObject> jsonObjects = map.get("line");
        if (CollectionUtils.isNotEmpty(jsonObjects)) {
            jsonObjects.add(statusMessage);
            map.put("line", jsonObjects);
        }
        if (CollectionUtils.isEmpty(jsonObjects)) {
            map.put("line", null);
        }
        // 波形图完成时清理缓存（可选）
        waveformCache.remove(taskId);

        return map;
    }

    /**
     * 使用高精度计时器实现完美准确的时间扫描
     * 改进：边测量边推送数据，让前端时间显示更准确
     */
    private Map<String, List<JSONObject>> timeScan(String taskId, Map<String, Object> dataMap, String deviceSerialNumber, Integer scanCount, String luminosityMode,boolean isMultiProductPool, String itemId) throws InterruptedException {
        Map<String, List<JSONObject>> map = new HashMap<>();
        // 线数据列表
        ArrayList<JSONObject> line = new ArrayList<>();
        // 结果数据列表
        ArrayList<JSONObject> res = new ArrayList<>();
        // X轴数据列表
        ArrayList<Double> xList = new ArrayList<>();
        // Y轴数据列表
        ArrayList<Double> yList = new ArrayList<>();

        // 增益
        Integer gain = MapUtils.getInteger(dataMap, GAIN.getDescription());
        // 狭缝，光谱带宽
        Double narrow = MapUtils.getDouble(dataMap, SPECTRAL_BANDWIDTH.getDescription());
        // 响应时间
        Double responseTime = MapUtils.getDouble(dataMap, RESPONSE_TIME.getDescription());
        // 特征值
        String characteristic = MapUtils.getString(dataMap, CHARACTERISTIC.getDescription());
        //时间间隔
        Double timeInterval = MapUtils.getDouble(dataMap, TIME_INTERVAL.getDescription());
        //总时长
        Double totalTime = timeInterval * scanCount;

        // 设置参数
        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.SetParam.getType());
        SetParam param = new SetParam();

        // 光度模式
        Integer photoMode = null;
        try {
            photoMode = Integer.valueOf(Objects.requireNonNull(WaveLengthEnum.getCode(luminosityMode)));
        } catch (Exception e) {
            log.warn("时间扫描-光度模式为空！");
        }

        // 灯状态，灯位置
        Integer lampStatus = null;
        try {
            lampStatus = Integer.valueOf(Objects.requireNonNull(EnergyLampEnums.getIdentityByName(MapUtils.getString(dataMap, LAMP_STATUS.getDescription()))));
        } catch (Exception e) {
            log.error("时间扫描-无效的灯状态");
        }

        param.setPhotoMode(photoMode);
        param.setResponseTime(responseTime);
        param.setGain(gain);
        param.setLampPos(lampStatus);
        param.setSlit1(narrow);
        inputParam.setParam(param);
        R<OperationResultEntity> operationDark3=new R<>();
        if (isMultiProductPool) {
            //切换样品池到黑挡块
            log.info("时间扫描-切换样品池到黑挡块>>>>>>>>>>>>>>>>>>>>");
            DevOperationInputParam operationInputParamDark = new DevOperationInputParam();
            operationInputParamDark.setDeviceSerialNumber(deviceSerialNumber);
            operationInputParamDark.setOperationType(OperationTypeEnums.SetParam.getType());
            SetParam sampleParamDark = new SetParam();
            Integer positionDark = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.BLACK_BLOCK);
            //设置样品池位置
            sampleParamDark.setCellPos(positionDark);
            operationInputParamDark.setParam(sampleParamDark);

            operationDark3 = devOperationUtilService.operation(operationInputParamDark);
        }else {
            operationDark3.setSuccess(true);
            promptUserToPlaceMaterial(taskId, "黑挡块");
        }
        if (operationDark3.getSuccess()) {
            log.info("时间扫描-切换样品池到黑挡块成功>>>>>>>>>>>>>>>>");
        } else {
            log.warn("时间扫描-切换样品池到黑挡块失败！");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", operationDark3.getMessage());
            sendToActiveSession(null, taskId, jsonObject);
            setTaskMeasuring(taskId, false);
            stopTask(taskId);
            map.put("line", line);
            map.put("res", res);
            return map;
        }


        log.info("时间扫描-开始暗电流矫正>>>>>>>>>>>>>>>>>>>>");
        DevOperationInputParam darkParam = new DevOperationInputParam();
        SetParam setParam1 = new SetParam();
        darkParam.setDeviceSerialNumber(deviceSerialNumber);
//        darkParam.setParam(setParam1);
        darkParam.setCalibrationType(DeviceCalibrationInfoEnum.Dark.getType());
        darkParam.setOperationType(OperationTypeEnums.Calibration.getType());
//        darkParam.setUpdateBy();
        R<OperationResultEntity> operationDark =
                devOperationUtilService.operation(darkParam);
        log.info("时间扫描-暗电流矫正结果：{}", operationDark);
        if (!operationDark.getSuccess()) {
            log.warn("时间扫描-暗电流矫正出错！");
            setTaskMeasuring(taskId, false);
            stopTask(taskId);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", operationDark.getMessage());
            boolean b = sendToActiveSession(null, taskId, jsonObject);
            throw new RuntimeException("时间扫描-暗电流矫正出错！");
        }


        // 设置参数
        R<OperationResultEntity> operation = devOperationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            log.warn("时间扫描-参数设置出错！");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", operation.getMessage());
            sendToActiveSession(null, taskId, jsonObject);
            setTaskMeasuring(taskId, false);
            stopTask(taskId);
            map.put("line", line);
            map.put("res", res);
            return map;
        }

        // 解析特征波长
        String[] parts = characteristic.split("[,，]");
        List<String> waveLength = Arrays.stream(parts)
                .filter(part -> part != null && !part.trim().isEmpty())
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(waveLength)) {
            inputParam.setWavelength(waveLength);
        }

        if (photoMode != null) {
            if (photoMode == 1 || photoMode == 2) {
                if (Thread.currentThread().isInterrupted()) {
                    log.warn("任务被取消！");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("error", "任务被取消！");
                    sendToActiveSession(null, taskId, jsonObject);
                    setTaskMeasuring(taskId, false);
                    throw new InterruptedException();
                }


            }
        }
        R<OperationResultEntity> operation4=new R<>();
        if (isMultiProductPool) {
            //切换样品池到校零物质
            log.info("时间扫描-切换样品池到校零物质>>>>>>>>>>>>>>>>>>>>");
            DevOperationInputParam operationInputParam = new DevOperationInputParam();
            operationInputParam.setDeviceSerialNumber(deviceSerialNumber);
            operationInputParam.setOperationType(OperationTypeEnums.SetParam.getType());
            SetParam sampleParam = new SetParam();
            Integer position = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.ZERO);
            //设置样品池位置
            sampleParam.setCellPos(position);
            operationInputParam.setParam(sampleParam);

            operation4 = devOperationUtilService.operation(operationInputParam);
        }else {
            operation4.setSuccess(true);
            List<TaskExecutionDetail> detailList = taskExecutionDetailMapper.initTaskCal(taskId);
            //获取当前检测项的所有标准物
            List<TaskExecutionDetail> collect = detailList.stream().filter(detail -> detail.getItemId().equals(itemId)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(collect)) {
                String standardName="";
                for (TaskExecutionDetail detail : collect) {
                    if (detail.getStandardType().equals(ZERO.getCode())){

                        CalStdCats cats = calStdCatsService.getById(detail.getStandardId());
                        if (cats==null){
                            CalArtifacts calArtifacts = calArtifactsService.getById(detail.getStandardId());
                            standardName=calArtifacts.getName();
                        }else {
                            standardName=cats.getName();
                        }
                    }
                }
                promptUserToPlaceMaterial(taskId, standardName);

            }else {
                log.warn("当前检测项没有标准物");
            }
        }
        if (operation4.getSuccess()) {
            log.info("时间扫描-切换样品池到校零物质成功>>>>>>>>>>>>>>>>");
        } else {
            log.warn("时间扫描-切换样品池到校零物质失败！");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", operation4.getMessage());
            sendToActiveSession(null, taskId, jsonObject);
            setTaskMeasuring(taskId, false);
            stopTask(taskId);
            map.put("line", line);
            map.put("res", res);
            return map;
        }
        // 执行校零操作
        inputParam.setOperationType(OperationTypeEnums.Zero.getType());
        R<OperationResultEntity> zero = devOperationUtilService.operation(inputParam);
        if (!zero.getSuccess()) {
            log.warn("时间扫描-校零失败！");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", zero.getMessage());
            sendToActiveSession(null, taskId, jsonObject);
            setTaskMeasuring(taskId, false);
            stopTask(taskId);
            map.put("line", line);
            map.put("res", res);
            return map;
        }

        List<ChangeParam> zeroList = zero.getData().getZeroResult();
        if (CollectionUtils.isEmpty(zeroList)) {
            log.warn("时间扫描-校零结果为空！");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", "时间扫描-校零结果为空！");
            sendToActiveSession(null, taskId, jsonObject);
            setTaskMeasuring(taskId, false);
            stopTask(taskId);
            map.put("line", line);
            map.put("res", res);
            return map;
        }


        // 使用定时任务实现边测量边推送
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TimeScan-" + taskId);
            t.setDaemon(true);
            return t;
        });

        // 使用阻塞队列来存储测量结果，实现生产者-消费者模式
        BlockingQueue<JSONObject> measurementQueue = new LinkedBlockingQueue<>();
        AtomicBoolean isCancelled = new AtomicBoolean(false);
        AtomicInteger completedMeasurements = new AtomicInteger(0);
        R<OperationResultEntity> operation5 =new R<>();
        if (isMultiProductPool) {
            //切换样品池到测量物质
            log.info("时间扫描-切换样品池到测量物质>>>>>>>>>>>>>>>>>>>>");
            DevOperationInputParam operationInputParam1 = new DevOperationInputParam();
            operationInputParam1.setDeviceSerialNumber(deviceSerialNumber);
            operationInputParam1.setOperationType(OperationTypeEnums.SetParam.getType());
            SetParam sampleParam1 = new SetParam();
            Integer position1 = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.MEASURE);
            //设置样品池位置
            sampleParam1.setCellPos(position1);
            operationInputParam1.setParam(sampleParam1);

            operation5=devOperationUtilService.operation(operationInputParam1);
        }else {
            operation5.setSuccess(true);
            List<TaskExecutionDetail> detailList = taskExecutionDetailMapper.initTaskCal(taskId);
            //获取当前检测项的所有标准物
            List<TaskExecutionDetail> collect = detailList.stream().filter(detail -> detail.getItemId().equals(itemId)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(collect)) {
                String standardName="";
                for (TaskExecutionDetail detail : collect) {
                    if (detail.getStandardType().equals(MEASUREMENT.getCode())){

                        CalStdCats cats = calStdCatsService.getById(detail.getStandardId());
                        if (cats==null){
                            CalArtifacts calArtifacts = calArtifactsService.getById(detail.getStandardId());
                            standardName=calArtifacts.getName();
                        }else {
                            standardName=cats.getName();
                        }
                    }
                }
                promptUserToPlaceMaterial(taskId, standardName);

            }else {
                log.warn("当前检测项没有标准物");
            }
        }

        if (operation5.getSuccess()) {
            log.info("时间扫描-切换样品池到测量物质成功>>>>>>>>>>>>>>>>");
        } else {
            log.warn("时间扫描-切换样品池到测量物质失败！");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", operation5.getMessage());
            sendToActiveSession(null, taskId, jsonObject);
            setTaskMeasuring(taskId, false);
            stopTask(taskId);
            map.put("line", line);
            map.put("res", res);
            return map;
        }

        /*// 定时测量任务 - 生产者
        Integer finalPhotoMode = photoMode;
        Runnable measurementTask = () -> {
            if (isCancelled.get()) {
                return;
            }*/

        try {
            DevOperationInputParam param1 = new DevOperationInputParam();
            // 执行单次测量
            param1.setOperationType(OperationTypeEnums.TimeMeasure.getType());
            param1.setDeviceSerialNumber(deviceSerialNumber);
            ArrayList<String> strings = new ArrayList<>();
            strings.add(characteristic);
            param1.setIntervalSeconds(timeInterval);
            param1.setTotalTime(totalTime);
            R<OperationResultEntity> measure = devOperationUtilService.operation(param1);
            UnknowCountMessageQueue queue = MessageOnTimeDeal.unknowCountMessageQueue;
            int i = 0;
            while (true) {
                try {
                    if (Thread.currentThread().isInterrupted()) {
                        log.info("线程中断");
                        setTaskMeasuring(taskId, false);
                        throw new InterruptedException("任务被中断");
                    }
                    Object o = queue.pollMessage(deviceSerialNumber, T600ComIdEnums.Measure.getComId());
                    if (o instanceof SpectrumPoint || o == null) {
                        if (o != null) {
                            i++;
                            SpectrumPoint p = (SpectrumPoint) o;

                            // 获取或创建波形图数据缓存
                            List<JSONObject> cachedDataPoints = waveformCache.computeIfAbsent(taskId, k -> new ArrayList<>());


                            getPointsByLuminosityMode(luminosityMode, p, xList, yList, taskId, i, cachedDataPoints, line, dataMap);
                            log.info("时间扫描结果：{}", p);
                        }
//                            log.info("时间扫描队列取到空,丢弃");
//                        }
                    } else {
                        break;
                    }
                } catch (Exception e) {
                    log.error("错误111：", e);
                    setTaskMeasuring(taskId, false);
                    throw new InterruptedException("任务被中断");
                }
            }
//           WaveLengthEnum anEnum = WaveLengthEnum.getByName(luminosityMode);
//
//                if (anEnum != null) {
//                    String value = null;
//                    String yType = null;
//
//                    switch (anEnum) {
//                        case abs:
//                            value = result.getAbs();
//                            yType = "abs";
//                            break;
//                        case tPercent:
//                            value = result.getTPercent();
//                            yType = "T%";
//                            break;
//                        case es:
//                            value = result.getEs();
//                            yType = "es";
//                            break;
//                        case er:
//                            value = result.getEr();
//                            yType = "er";
//                            break;
//                    }
//
//                    if (value != null) {
//                        JSONObject pointData = new JSONObject();
//                        JSONArray points = new JSONArray();
//
//                        double wavelength = Double.parseDouble(zeroParam.getWl());
//
//                        pointObj.put("x", wavelength);
//                        pointObj.put("y", Double.valueOf(value));
//                        pointObj.put("xType", "波长(nm)");
//                        pointObj.put("yType", yType);
//
//                        // 添加实际测量时间戳
////                            pointObj.put("timestamp", System.currentTimeMillis());
//
//                        points.add(pointObj);
//                        pointData.put("data", points);
//                        pointData.put("sampleId", "");
//                        pointData.put("taskId", taskId);
//                        pointData.put("pointIndex", completedMeasurements.getAndIncrement() + 1);
//                        pointData.put("totalPoints", scanCount);
//
//                        // 将测量结果放入队列
//                        boolean offer = measurementQueue.offer(pointData);
//
//                        // 更新xList和yList用于后续分析
//                        xList.add(wavelength);
//                        yList.add(Double.valueOf(value));
//                    }
//                }

        } catch (Exception e) {
            log.error("测量过程出错", e);
            JSONObject errorObj = new JSONObject();
            errorObj.put("error", "测量过程出错：" + e.getMessage());
            boolean offer = measurementQueue.offer(errorObj);
        }


//        // 数据推送任务 - 消费者
//        ScheduledExecutorService pushScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
//            Thread t = new Thread(r, "TimeScanPush-" + taskId);
//            t.setDaemon(true);
//            return t;
//        });
//        Thread.sleep(5000);
//        // 定期从队列中取出数据并推送给前端
//        ScheduledFuture<?> pushTask = pushScheduler.scheduleAtFixedRate(() -> {
//            if (isCancelled.get()) {
//                return;
//            }
//
//            List<JSONObject> batchData = new ArrayList<>();
//            measurementQueue.drainTo(batchData);
//
//            if (!batchData.isEmpty()) {
//                for (JSONObject data : batchData) {
//                    if (data.containsKey("error")) {
//                        sendToActiveSession(null, taskId, data);
//                        isCancelled.set(true);
//                        break;
//                    } else {
//                        // 推送单个数据点到前端
//                        sendToActiveSession(null, taskId, data);
//                        line.add(data);
//                    }
//                }
//            }
//
//            // 检查是否已完成所有测量
//            if (completedMeasurements.get() >= scanCount) {
//                isCancelled.set(true);
//            }
//        }, 0, 100, TimeUnit.MILLISECONDS); // 每100ms检查一次队列

   /*     // 开始定时测量
        ScheduledFuture<?> measurementFuture = scheduler.scheduleAtFixedRate(
                measurementTask,
                0,
                timeInterval,
                TimeUnit.SECONDS
        );*/

        // 等待所有测量完成
    /*    try {
            while (completedMeasurements.get() < scanCount && !isCancelled.get()) {
                Thread.sleep(500); // 检查间隔
            }
        } catch (
                InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("时间扫描被中断");
        } finally {
            isCancelled.set(true);
            measurementFuture.cancel(true);
            pushTask.cancel(true);
            scheduler.shutdown();
            pushScheduler.shutdown();

            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
                if (!pushScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    pushScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
                pushScheduler.shutdownNow();
            }
        }
*/
        // 最终分析结果
        try {
            if (!xList.isEmpty() && !yList.isEmpty()) {
                List<Double> analysisResult = findMaxOffsetUVTestTool.calcResult(xList, yList, null);
                if (CollectionUtils.isNotEmpty(analysisResult)) {
                    for (Double value : analysisResult) {
                        JSONObject resultObj = new JSONObject();
                        resultObj.put("peak", value.toString());
                        res.add(resultObj);
                    }
                }
            }
        } catch (
                Exception e) {
            log.warn("时间测量分析出错！", e);
            JSONObject errorObj = new JSONObject();
            errorObj.put("error", "时间测量分析出错！");
            sendToActiveSession(null, taskId, errorObj);
            setTaskMeasuring(taskId, false);
            stopTask(taskId);
        }

        map.put("line", line);
        map.put("res", res);
        return map;
    }

   /* private Map<String, List<JSONObject>> timeScan(String taskId, String itemId, Map<String, Object> dataMap, String updateBy, String deviceSerialNumber, WebSocketSession originalSession, Integer scanCount, Integer timeInterval, String luminosityMode) {
        Map<String, List<JSONObject>> map = new HashMap<>();
        //线
        ArrayList<JSONObject> line = new ArrayList<>();
        //数据
        ArrayList<JSONObject> res = new ArrayList<>();
//x轴list
        ArrayList<Double> xList = new ArrayList<>();
        //y轴list
        ArrayList<Double> yList = new ArrayList<>();

//增益
        Integer gain = MapUtils.getInteger(dataMap, GAIN.getDescription());
        //狭缝，光谱带宽
        Double narrow = MapUtils.getDouble(dataMap, SPECTRAL_BANDWIDTH.getDescription());
        //响应时间
        Double responseTime = MapUtils.getDouble(dataMap, RESPONSE_TIME.getDescription());
        //特征值
        String characteristic = MapUtils.getString(dataMap, CHARACTERISTIC.getDescription());

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.SetParam.getType());
        SetParam param = new SetParam();
        //光度模式
        Integer photoMode = null;
        try {
            photoMode = Integer.valueOf(Objects.requireNonNull(WaveLengthEnum.getCode(luminosityMode)));
        } catch (Exception e) {
            log.warn("光度模式为空！");
        }
        //灯状态，灯位置
        Integer lampStatus = null;
        try {
            lampStatus = Integer.valueOf(Objects.requireNonNull(EnergyLampEnums.getIdentityByName(MapUtils.getString(dataMap, LAMP_STATUS.getDescription()))));
        } catch (Exception e) {
            log.error("无效的灯状态");
        }
        param.setPhotoMode(photoMode);
        param.setResponseTime(responseTime);

        param.setGain(gain);

        param.setLampPos(lampStatus);

        param.setSlit1(narrow);
        inputParam.setParam(param);

//设置参数
        R<OperationResultEntity> operation = devOperationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            log.warn("参数设置出错！");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", "参数设置出错！");
            sendToActiveSession(null, taskId, jsonObject);
            setTaskMeasuring(taskId, false);
            stopTask(taskId);
        }

        String[] parts = characteristic.split("[,，]");
        List<String> waveLength = Arrays.stream(parts)
                .filter(part -> part != null && !part.trim().isEmpty())
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(waveLength)) {
            inputParam.setWavelength(waveLength);
        }

        inputParam.setOperationType(OperationTypeEnums.Zero.getType());
        //校零
        R<OperationResultEntity> zero = devOperationUtilService.operation(inputParam);
        List<ChangeParam> zeroList = zero.getData().getZeroResult();
        List<ChangeParam> measureList = new ArrayList<>();
        //测量
        if (zero.getSuccess()) {
            // 时间扫描实现
            for (int i = 0; i < scanCount; i++) {
                inputParam.setOperationType(OperationTypeEnums.Measure.getType());
                R<OperationResultEntity> measure = devOperationUtilService.operation(inputParam);
                log.info("测量完成：" + measure);

                ChangeParam zeroParam = zeroList.get(0);
                SpectrumPoint point = new SpectrumPoint(Double.parseDouble(zeroParam.getWl()));
                point.setZeroRsd(zeroParam);
                List<ChangeParam> originalMeasureResult = measure.getData().getOriginalMeasureResult();
                if (CollectionUtils.isNotEmpty(originalMeasureResult)) {
                    String wl = zeroParam.getWl();
                    for (ChangeParam changeParam : originalMeasureResult) {
                        if (StringUtils.equals(wl, changeParam.getWl())) {
                            point.setMeasureRsd(changeParam);
                            measureList.add(changeParam);
                        }
                    }
                }
                SpectrumCalcResult result = SpectrumResultCalculator.calculate(photoMode, point, SpectrumSourceType.SINGLE);
                WaveLengthEnum anEnum = WaveLengthEnum.getByName(luminosityMode);
                if (anEnum != null) {
                    String value = null;
                    JSONObject point1 = new JSONObject();

                    switch (anEnum) {
                        case abs:
                            value = Objects.requireNonNull(result).getAbs();
                            xList.add(result.getWavelength());
                            yList.add(Double.valueOf(result.getAbs()));
                            point1.put("yType", "abs");
                            break;
                        case tPercent:
                            value = Objects.requireNonNull(result).getTPercent();
                            xList.add(result.getWavelength());
                            yList.add(Double.valueOf(result.getTPercent()));
                            point1.put("yType", "T%");
                            break;
                        case es:
                            value = Objects.requireNonNull(result).getEs();
                            xList.add(result.getWavelength());
                            yList.add(Double.valueOf(result.getEs()));
                            point1.put("yType", "es");
                            break;
                        case er:
                            value = Objects.requireNonNull(result).getEr();
                            xList.add(result.getWavelength());
                            yList.add(Double.valueOf(result.getEr()));
                            point1.put("yType", "er");
                            break;
                    }

                    JSONObject payload = new JSONObject();
                    JSONArray points = new JSONArray();
                    double wavelength = Double.parseDouble(zeroParam.getWl());
                    point1.put("x", wavelength);
                    xList.add(wavelength);
                    Double aDouble = new Double(value);
                    point1.put("y", aDouble);
                    yList.add(aDouble);
//                    point.put("roundOffY", valueY);
//                    point.put("nm", waveLength);
                    point1.put("xType", "波长(nm)");

                    points.add(point1);
                    payload.put("data", points);
                    payload.put("sampleId", "");
                    payload.put("taskId", taskId);
                    payload.put("pointIndex", i);
                    payload.put("totalPoints", scanCount);


                    // 尝试发送到活跃session
                    sendToActiveSession(null, taskId, payload);
                    line.add(payload);
                }
//                if (photoMode != null) {
//                     measureList.addAll(measure.getData().getOriginalMeasureResult());
//                }

                // 如果不是最后一次测量，则等待指定时间
                if (i < scanCount - 1) {
                    try {
                        Thread.sleep(timeInterval * 1000L); // scanInterval是秒，转换为毫秒
                    } catch (InterruptedException e) {
                        log.warn("时间扫描线程被中断");
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }


            if (CollectionUtils.isNotEmpty(measureList)) {
                Integer finalPhotoMode = photoMode;
                try {
                    List<Double> doubles = findMaxOffsetUVTestTool.calcResult(xList, yList, null);
                    if (doubles != null && !doubles.isEmpty()) {
                        for (Double v : doubles) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("peak", v.toString());
                            res.add(jsonObject);
                        }
                    }
                } catch (Exception e) {
                    log.warn("时间测量出错！");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("error", "时间测量出错！");
                    sendToActiveSession(null, taskId, jsonObject);
                    setTaskMeasuring(taskId, false);
                    stopTask(taskId);
                }
            }

        } else {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", "校零失败！");
            sendToActiveSession(null, taskId, jsonObject);
            setTaskMeasuring(taskId, false);
            stopTask(taskId);
        }
        map.put("line", line);
        map.put("res", res);
        return map;
    }
*/


    private Map<String, List<JSONObject>> intensityScan(String taskId, Map<String, Object> dataMap, String deviceSerialNumber,boolean isMultiProductPool, String itemId) {
        //光度模式
        String luminosityMode = MapUtils.getString(dataMap, LUMINOSITY_MODE.getDescription());
        //获取扫描速度
        String scanSpeed = MapUtils.getString(dataMap, SCAN_SPEED.getDescription());
        //获取起始波长
        Double startWavelength = MapUtils.getDouble(dataMap, START_WAVELENGTH.getDescription());
        //获取终止波长
        Double endWavelength = MapUtils.getDouble(dataMap, END_WAVELENGTH.getDescription());
        //扫描间隔
        Double scanInterval = MapUtils.getDouble(dataMap, SCAN_INTERVAL.getDescription());
        //增益
        Integer gain = MapUtils.getInteger(dataMap, GAIN.getDescription());
        //狭缝，光谱带宽
        Double narrow = MapUtils.getDouble(dataMap, SPECTRAL_BANDWIDTH.getDescription());
        //响应时间
        Double responseTime = MapUtils.getDouble(dataMap, RESPONSE_TIME.getDescription());
        //特征值
        String characteristic = MapUtils.getString(dataMap, CHARACTERISTIC.getDescription());
        //灯状态，灯位置
        Integer lampStatus = null;
        try {
            lampStatus = Integer.valueOf(Objects.requireNonNull(EnergyLampEnums.getIdentityByName(MapUtils.getString(dataMap, LAMP_STATUS.getDescription()))));
        } catch (Exception e) {
            log.warn("灯位置为空！");
        }
        Map<String, List<JSONObject>> map = new HashMap<>();
        map.put("line", null);
        ArrayList<JSONObject> res = new ArrayList<>();

        R<OperationResultEntity> operation3=new R<>();
        if (isMultiProductPool) {
            //切换样品池到黑挡块
            log.info("切换样品池到黑挡块>>>>>>>>>>>>>>>>>>>>");
            DevOperationInputParam operationInputParam = new DevOperationInputParam();
            operationInputParam.setDeviceSerialNumber(deviceSerialNumber);
            operationInputParam.setOperationType(OperationTypeEnums.SetParam.getType());
            SetParam sampleParam = new SetParam();
            Integer position = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.BLACK_BLOCK);
            //设置样品池位置
            sampleParam.setCellPos(position);
            operationInputParam.setParam(sampleParam);

            operation3 = devOperationUtilService.operation(operationInputParam);
        }else {
            operation3.setSuccess(true);
            promptUserToPlaceMaterial(taskId, "黑挡块");
        }
        if (operation3.getSuccess()) {
            log.info("切换样品池到黑挡块成功>>>>>>>>>>>>>>>>");
        } else {
            log.warn("切换样品池到黑挡块失败！");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", operation3.getMessage());
            sendToActiveSession(null, taskId, jsonObject);
            setTaskMeasuring(taskId, false);
            stopTask(taskId);
            map.put("line", null);
            map.put("res", res);
            return map;
        }
        log.info("开始暗电流矫正>>>>>>>>>>>>>>>>>>>>");
        DevOperationInputParam darkParam = new DevOperationInputParam();
//        SetParam setParam1 = new SetParam();
        darkParam.setDeviceSerialNumber(deviceSerialNumber);
//        darkParam.setParam(setParam1);
        darkParam.setCalibrationType(DeviceCalibrationInfoEnum.Dark.getType());
        darkParam.setOperationType(OperationTypeEnums.Calibration.getType());
//        darkParam.setUpdateBy();
        R<OperationResultEntity> operationDark =
                devOperationUtilService.operation(darkParam);
        log.info("暗电流矫正结果：{}", operationDark);
        if (!operationDark.getSuccess()) {
            log.warn("暗电流矫正出错！");
            setTaskMeasuring(taskId, false);
            stopTask(taskId);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", operationDark.getMessage());
            boolean b = sendToActiveSession(null, taskId, jsonObject);
            throw new RuntimeException("暗电流矫正出错！");
        }

        DevOperationInputParam inputParam = new DevOperationInputParam();
        inputParam.setDeviceSerialNumber(deviceSerialNumber);
        inputParam.setOperationType(OperationTypeEnums.SetParam.getType());
        SetParam param = new SetParam();
        Integer photoMode = null;
        try {
            photoMode = Integer.valueOf(Objects.requireNonNull(WaveLengthEnum.getCode(luminosityMode)));
        } catch (Exception e) {
            log.warn("光度模式为空！");
        }
        param.setPhotoMode(photoMode);
        param.setResponseTime(responseTime);

        param.setGain(gain);

        param.setLampPos(lampStatus);

        param.setSlit1(narrow);
        inputParam.setParam(param);

        //设置参数
        R<OperationResultEntity> operation = devOperationUtilService.operation(inputParam);
        if (!operation.getSuccess()) {
            log.warn("参数设置出错！");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", operation.getMessage());
            sendToActiveSession(null, taskId, jsonObject);
            setTaskMeasuring(taskId, false);
            stopTask(taskId);
        }

        String[] parts = characteristic.split("[,，]");
        List<String> waveLength = Arrays.stream(parts)
                .filter(part -> part != null && !part.trim().isEmpty())
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(waveLength)) {
            inputParam.setWavelength(waveLength);
        }
        R<OperationResultEntity> operation5=new R<>();
        if (isMultiProductPool) {
            log.info("切换样品池到校零物质>>>>>>>>>>>>>>>>>>>>");
            DevOperationInputParam devOperationInputParam = new DevOperationInputParam();
            devOperationInputParam.setDeviceSerialNumber(deviceSerialNumber);
            devOperationInputParam.setOperationType(OperationTypeEnums.SetParam.getType());
            SetParam setParam = new SetParam();
            Integer actionPosition = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.ZERO);
            //设置样品池位置
            setParam.setCellPos(actionPosition);
            devOperationInputParam.setParam(setParam);

            operation5= devOperationUtilService.operation(devOperationInputParam);
        }else {
            operation5.setSuccess(true);
            List<TaskExecutionDetail> detailList = taskExecutionDetailMapper.initTaskCal(taskId);
            //获取当前检测项的所有标准物
            List<TaskExecutionDetail> collect = detailList.stream().filter(detail -> detail.getItemId().equals(itemId)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(collect)) {
                String standardName="";
                for (TaskExecutionDetail detail : collect) {
                    if (detail.getStandardType().equals(ZERO.getCode())){

                        CalStdCats cats = calStdCatsService.getById(detail.getStandardId());
                        if (cats==null){
                            CalArtifacts calArtifacts = calArtifactsService.getById(detail.getStandardId());
                            standardName=calArtifacts.getName();
                        }else {
                            standardName=cats.getName();
                        }
                    }
                }
                promptUserToPlaceMaterial(taskId, standardName);

            }else {
                log.warn("当前检测项没有标准物");
            }
        }
        if (operation5.getSuccess()) {
            log.info("切换样品池到校零物质成功>>>>>>>>>>>>>>>>");
        } else {
            log.warn("切换样品池到校零物质失败！");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", operation5.getMessage());
            sendToActiveSession(null, taskId, jsonObject);
            setTaskMeasuring(taskId, false);
            stopTask(taskId);
            map.put("line", null);
            map.put("res", res);
            return map;
        }


        inputParam.setOperationType(OperationTypeEnums.Zero.getType());
        //校零
        R<OperationResultEntity> zero = devOperationUtilService.operation(inputParam);

        //测量
        if (zero.getSuccess()) {
            R<OperationResultEntity> operation6=new R<>();
            if (isMultiProductPool) {
                log.info("切换样品池到测量物质>>>>>>>>>>>>>>>>>>>>");
                DevOperationInputParam param2 = new DevOperationInputParam();
                param2.setDeviceSerialNumber(deviceSerialNumber);
                param2.setOperationType(OperationTypeEnums.SetParam.getType());
                SetParam setParam3 = new SetParam();
                Integer actionPosition1 = materialManager.getActionPosition(taskId, StandardMaterialManager.MaterialType.ZERO);
                //设置样品池位置
                setParam3.setCellPos(actionPosition1);
                param2.setParam(setParam3);

                operation6 = devOperationUtilService.operation(param2);
            }else {
                operation6.setSuccess(true);
                List<TaskExecutionDetail> detailList = taskExecutionDetailMapper.initTaskCal(taskId);
                //获取当前检测项的所有标准物
                List<TaskExecutionDetail> collect = detailList.stream().filter(detail -> detail.getItemId().equals(itemId)).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(collect)) {
                    String standardName="";
                    for (TaskExecutionDetail detail : collect) {
                        if (detail.getStandardType().equals(MEASUREMENT.getCode())){

                            CalStdCats cats = calStdCatsService.getById(detail.getStandardId());
                            if (cats==null){
                                CalArtifacts calArtifacts = calArtifactsService.getById(detail.getStandardId());
                                standardName=calArtifacts.getName();
                            }else {
                                standardName=cats.getName();
                            }
                        }
                    }
                    promptUserToPlaceMaterial(taskId, standardName);

                }else {
                    log.warn("当前检测项没有标准物");
                }
            }
            if (operation6.getSuccess()) {
                log.info("切换样品池到测量物质成功>>>>>>>>>>>>>>>>");
            } else {
                log.warn("切换样品池到测量物质失败！");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", operation6.getMessage());
                sendToActiveSession(null, taskId, jsonObject);
                setTaskMeasuring(taskId, false);
                stopTask(taskId);
                map.put("line", null);
                map.put("res", res);
                return map;
            }


            inputParam.setOperationType(OperationTypeEnums.Measure.getType());
            LocalDateTime startTime = LocalDateTime.now();
            R<OperationResultEntity> measure = devOperationUtilService.operation(inputParam);
            if (!measure.getSuccess()) {
                log.warn("测量出错！");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("error", measure.getMessage());
                sendToActiveSession(null, taskId, jsonObject);
                setTaskMeasuring(taskId, false);
                stopTask(taskId);
                return map;
            }
            log.info("光度测量完成：" + measure);
//            OperationResultEntity data = measure.getData();
//            List<ChangeParam> originalMeasureResult = data.getOriginalMeasureResult();

//            for (ChangeParam changeParam : originalMeasureResult) {
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("peak", changeParam.getWl());
//                res.add(jsonObject);
//                log.info("测量结果：" + changeParam);
//            }
            if (photoMode != null) {
                List<ChangeParam> zeroList = zero.getData().getZeroResult();
                List<ChangeParam> measureList = measure.getData().getOriginalMeasureResult();
                Map<String, Pair<ChangeParam, ChangeParam>> pair = pairByWavelength(zeroList, measureList);

                ArrayList<String> list = new ArrayList<>();

                if (CollectionUtils.isNotEmpty(pair)) {
                    Integer finalPhotoMode = photoMode;
                    try {


                        pair.forEach((wavelength, pairValue) -> {
                            SpectrumPoint point = new SpectrumPoint(Double.parseDouble(wavelength));
                            point.setZeroRsd(pairValue.getKey());
                            point.setMeasureRsd(pairValue.getValue());
                            SpectrumCalcResult result = SpectrumResultCalculator.calculate(finalPhotoMode, point, SpectrumSourceType.SINGLE);
                            WaveLengthEnum anEnum = WaveLengthEnum.getByName(luminosityMode);
                            if (anEnum != null) {
                                JSONObject object = new JSONObject();
                                switch (anEnum) {
                                    case abs:
                                        object.put("peak", Objects.requireNonNull(result).getAbs());
                                        res.add(object);
                                        break;
                                    case tPercent:

                                        object.put("peak", Objects.requireNonNull(result).getTPercent());
                                        res.add(object);
                                        break;
                                    case es:
                                        object.put("peak", Objects.requireNonNull(result).getEs());
                                        res.add(object);
                                        break;
                                    case er:
                                        object.put("peak", Objects.requireNonNull(result).getEr());
                                        res.add(object);
                                        break;
                                }
                            }
                        });
                    } catch (Exception e) {
                        log.warn("光度测量出错！");
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("error", "光度测量出错！");
                        sendToActiveSession(null, taskId, jsonObject);
                        setTaskMeasuring(taskId, false);
                        stopTask(taskId);
                    }
                }
            }

        } else {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("error", zero.getMessage());
            sendToActiveSession(null, taskId, jsonObject);
            setTaskMeasuring(taskId, false);
            stopTask(taskId);
        }

        setTaskMeasuring(taskId, false);
        map.put("res", res);
        return map;
    }

    //pairByWavelength
    private Map<String, Pair<ChangeParam, ChangeParam>> pairByWavelength(List<ChangeParam> zeroList, List<ChangeParam> measureList) {
        // 按波长属性配对
        Map<String, Pair<ChangeParam, ChangeParam>> pairedResults = new HashMap<>();

// 遍历校零结果
        for (ChangeParam zeroParam : zeroList) {
            String key = zeroParam.getWl(); // 或其他唯一标识属性
            pairedResults.put(key, new Pair<>(zeroParam, null));
        }

// 遍历测量结果，与校零结果配对
        for (ChangeParam measureParam : measureList) {
            String key = measureParam.getWl(); // 使用相同属性作为key
            Pair<ChangeParam, ChangeParam> existingPair = pairedResults.get(key);
            if (existingPair != null) {
                // 更新配对结果
                pairedResults.put(key, new Pair<>(existingPair.getKey(), measureParam));
            } else {
                // 如果校零列表中没有对应项，则只有测量值
                pairedResults.put(key, new Pair<>(null, measureParam));
            }
        }
        return pairedResults;

    }

    // 安全发送到活跃会话
// 安全发送到活跃会话
    private boolean sendToActiveSession(WebSocketSession originalSession, String taskId, JSONObject payload) {
        // 首先尝试使用原始session
        if (originalSession != null && originalSession.isOpen()) {
            try {
                originalSession.sendMessage(new TextMessage(payload.toJSONString()));
                return true;
            } catch (IOException e) {
                log.warn("原始session发送失败: {}", e.getMessage());
            }
        }

        // 尝试使用sessionTaskMap中的session
        WebSocketSession mappedSession = sessionTaskMap.get(taskId);
        if (mappedSession != null && mappedSession.isOpen()) {
            try {
                mappedSession.sendMessage(new TextMessage(payload.toJSONString()));
                return true;
            } catch (IOException e) {
                log.warn("映射session发送失败: {}", e.getMessage());
            }
        }

        return false;
    }


    // 发送待发送的数据点
    private void sendPendingDataPoints(String taskId, List<JSONObject> pendingDataPoints) {
        if (pendingDataPoints.isEmpty()) {
            return;
        }

        // 获取当前活跃的session
        WebSocketSession activeSession = sessionTaskMap.get(taskId);
        if (activeSession == null || !activeSession.isOpen()) {
            log.info("没有活跃session，无法发送 {} 个待发送数据点", pendingDataPoints.size());
            return;
        }

        log.info("发送 {} 个待发送数据点到活跃session: {}", pendingDataPoints.size(), activeSession.getId());

        for (JSONObject payload : pendingDataPoints) {
            try {
                activeSession.sendMessage(new TextMessage(payload.toJSONString()));
            } catch (IOException e) {
                log.error("发送待发送数据点失败: {}", e.getMessage());
                // 如果发送失败，可以考虑缓存到Ehcache
                break; // 停止发送剩余的数据点
            }
        }

        // 清空已发送的数据点
        pendingDataPoints.clear();
    }

//    private void sendBatchDataPoints(WebSocketSession session, String scanType, String taskId) throws IOException, InterruptedException {
//        //这是一条线，对应一个测量值
//        ArrayList<JSONObject> list = new ArrayList<>();
//
//        int totalPoints = 500;
//        String xType = "s"; // 默认为时间扫描
//        String yType = "Es"; // 默认y类型
//        String waveLength = "660nm"; // 默认波长
//        Integer digit = 4; // 默认小数位数
//
//        // 根据扫描类型设置参数
//        if ("spectrum".equals(scanType)) {
//            xType = "nm";
//            yType = "Er";
//            waveLength = "496nm";
//        }
//
//        for (int i = 0; i < totalPoints; i++) {
//            // 生成模拟数据
//            BigDecimal xValue = new BigDecimal(i + 1); // 从1开始递增
//            BigDecimal yValue = generateSimulatedYValue(i); // 生成模拟y值
//
//            // 发送单个数据点
//            JSONObject payload = new JSONObject();
//            JSONArray points = new JSONArray();
//            JSONObject point = new JSONObject();
//
//            point.put("x", xValue);
//
//            Double decimalY = yValue.doubleValue();
//            Double value = DataCountUtil.formatValue(digit, decimalY);
//            String valueY = DataCountUtil.addZero(value, digit);
//
//            point.put("y", decimalY);
//            point.put("roundOffY", valueY);
//            point.put("nm", waveLength);
//            point.put("xType", xType);
//            point.put("yType", yType);
//
//            points.add(point);
//            payload.put("data", points);
//            payload.put("sampleId", "");
//            if (session != null && session.isOpen()) {
//                log.info("发送数据点-----------" + session.getId());
//                session.sendMessage(new TextMessage(payload.toJSONString()));
//            } else {
//                //如果session已经关闭，则从sessionTaskMap中获取session
//                session = sessionTaskMap.get(taskId);
//                log.info("之前session死了" + session.getId());
//                if (session == null || !session.isOpen()) {
//                    log.info("新的session已经关闭了");
//                } else {
//                    session.sendMessage(new TextMessage(payload.toJSONString()));
//                }
//            }
//            list.add(payload);
//            //如果当前没有任何session那就存储到缓存中去
//            // 间隔1秒发送下一个数据点
//            Thread.sleep(1000);
//        }
//    }

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
     * 用来给特征值检测结果赋值
     */
    private void updateWorkbookCell(Workbook workbook, CellUpdDto cellUpdDto) {
        Sheet sheet = workbook.getSheet(cellUpdDto.getSheetName());

        /*// 使用新方法更新特定单元格
templateService.updateCellValue(sheet, excelCellLocator, excelCellEditor,
    "测量值 (nm)", "钬溶液", "536.8", "123.45");
*/
        if (cellUpdDto.getIsMultiple()) {
            // 先更新单元格值
            templateService.updateCellValue(sheet, excelCellLocator, excelCellEditor,
                    cellUpdDto.getValueType(), cellUpdDto.getProjectName(),
                    cellUpdDto.getFeatureValue(), cellUpdDto.getCellValue(), cellUpdDto.getPosition());

        } else {
            templateService.updateCellValue(sheet, excelCellLocator, excelCellEditor,
                    cellUpdDto.getValueType(), cellUpdDto.getProjectName(), cellUpdDto.getPosition(), cellUpdDto.getCellValue());

        }

        // 计算所有公式
        calculateFormulas(workbook);
    }

    private void updateWorkbookCells(Workbook workbook, CellUpdDto cellUpdDto) {
        // 在updateWorkbookCells方法开始处添加空值检查
        if (workbook == null) {
            log.error("Workbook 为空");
            return;
        }
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
    private void sendSheetUpdate(WebSocketSession session, Sheet sheet, String taskId) throws IOException {
        log.debug("推送Sheet更新数据");
        Workbook workbook = sheet.getWorkbook();

        // 构造完整的Sheet结构化数据
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("sheetName", sheet.getSheetName());
        updateData.put("sheetData", extractSheetData(sheet, workbook)); // 传递workbook用于公式计算

        ObjectMapper mapper = new ObjectMapper();
        String jsonData = mapper.writeValueAsString(updateData);
        if (session != null && session.isOpen()) {
            log.debug("推送Sheet更新数据，这个session还活着{}", session.getUri());
            session.sendMessage(new TextMessage(jsonData));
        } else {
            session = sessionTaskMap.get(taskId);

            log.debug("推送Sheet更新数据，这个session已经死了，使用新的{}", session.getUri());
//            log.info("新的session是 " + session);
            if (session == null || !session.isOpen()) {
                log.info("新的session已经关闭了");
            } else {
                session.sendMessage(new TextMessage(jsonData));
            }
        }
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
            Row row = sheet.getRow(region.getFirstRow());
            if (row != null) {
                Cell cell = row.getCell(region.getFirstColumn());
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
            Row row = sheet.getRow(rowNum);
            if (row != null) {
                Map<String, Object> cellData = new HashMap<>();
                for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
                    Cell cell = row.getCell(cellNum);
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
//        String id = getTaskIdFromSession(session);
//        sessionTaskMap.remove(id);


        String sessionId = session.getId();
        String taskId = getTaskIdFromSession(session);

        sessionTaskMap.remove(taskId);
        lastActivityTime.remove(sessionId);
        cancelHeartbeatTask(sessionId);

        super.afterConnectionClosed(session, status);
    }

    private void cancelHeartbeatTask(String sessionId) {
        ScheduledFuture<?> task = heartbeatTasks.remove(sessionId);
        if (task != null && !task.isDone()) {
            task.cancel(false);
        }
    }

    @PreDestroy
    public void destroy() {
        heartbeatTasks.values().forEach(task -> task.cancel(false));
        heartbeatTasks.clear();
        heartbeatScheduler.shutdown();
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
