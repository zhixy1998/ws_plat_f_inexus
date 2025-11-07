package com.persee.workstation.uv_inexus.websocket.concise;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.*;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FPeakValleyMarking;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTaskSpecimen;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FTimeSweepParamter;
import com.persee.workstation.uv_inexus.service.concise.measurementService.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemAlgorithmConfService;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.FormulaUtil;
import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
public class ConciseTimeWebSocket extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TimingWebSocket.class);
    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private Future<?> future;
    private String ip;
    private String deviceSerialNumber;
    private boolean occupancyStatus = false;
    private Integer digit;



    @Autowired
    private TimeSweepConciseService timeSweepConciseService;

    @Autowired
    private FMeasureTaskService tMeasureTaskService;

    @Autowired
    private FMeasureMethodService tMeasureMethodService;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private FMeasureDataService tMeasureDataService;

    @Autowired
    private FMeasureAnalysisDataService tMeasureAnalysisDataService;

    @Autowired
    private FMeasureAnalysisImageService tMeasureAnalysisImageService;

    @Autowired
    private FSamplePoolConfigService fSamplePoolConfigService;

    @Autowired
    private FMeasureSampleService tMeasureSampleService;

    @Autowired
    private SystemAlgorithmConfService systemAlgorithmConfService;

    @Autowired
    private DevMessageUtil devMessageUtil;

    @Autowired
    private RestTemplate restTemplate;

    // WebSocket 会话对象
    private WebSocketSession currentSession;

    @Autowired
    private FormulaUtil formulaUtil;

    // 当WebSocket连接建立成功时，会调用此方法
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        occupancyStatus = false;

        // 将耗时逻辑提交到线程池
        future = executor.submit(() -> {
            try {
                log.info("WebSocket连接已建立: " + session.getId());
                sessions.put(session.getId(), session);
                String query = Objects.requireNonNull(session.getUri()).getQuery();
                String taskId = getQueryParam(query, "taskId");
                deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");
                String updateBy = getQueryParam(query, "updateBy");
                digit = Integer.parseInt(getQueryParam(query, "digit"));
                if (taskId == null) {
                    sendClose(session, "缺少 taskId 参数");
                    return;
                }
                if (deviceSerialNumber == null) {
                    sendClose(session, "缺少 deviceSerialNumber 参数");
                    return;
                }
                if (updateBy == null) {
                    sendClose(session, "缺少 updateBy 参数");
                    return;
                }
                // 根据任务id查询任务数据
                FMeasureTask task = tMeasureTaskService.getById(taskId);
                if (task == null) {
                    sendClose(session, "任务不存在");
                    return;
                }

                //占用仪器
                R<Boolean> message = deviceInfoService.useDevice(deviceSerialNumber, Constants.DEVICE_STATUS_MEASURE);
                if (!message.getSuccess()) {
                    sendClose(session, message.getMessage());
                    return;
                }
                occupancyStatus=true;

                /// 设置参数
                Result result = timeSweepConciseService.setUpParametersPort(task.getMethodKey(), deviceSerialNumber);
                if (result.getCode() != 200) {
                    sendAndClose(session, "设置参数失败");
                    return;
                }

                // 根据方法id查询方法数据
                FMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
                if (method == null) {
                    sendAndClose(session, "方法不存在");
                    return;
                }
                // 将方法参数提取出来
                FTimeSweepParamter timeSweepParameter = JSON.parseObject(method.getMeasurementParamInfo(), FTimeSweepParamter.class);


                startTimedTask(session, timeSweepParameter, deviceSerialNumber, task,updateBy);
            } catch (Exception e) {
                e.printStackTrace();
                sendAndClose(session, "执行异常: " + e.getMessage());
            }
        });
    }


    // 开始定时任务
    private void startTimedTask(WebSocketSession session, FTimeSweepParamter param, String deviceSerialNumber, FMeasureTask task,String updateBy) {
        // 获取任务的校零参数
        final ChangeParam baselineData = (task.getZeroParam() != null && !task.getZeroParam().equals(""))
                ? JSON.parseObject(task.getZeroParam(), ChangeParam.class)
                : null;

        // 获取任务样品参数
        List<FTaskSpecimen> specimenList = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);

        List<FTaskSpecimen> newSampleData = new ArrayList<>();
        for (FTaskSpecimen taskSpecimen : specimenList) {
            if ("0".equals(taskSpecimen.getDeleteType()) && "0".equals(taskSpecimen.getIsTested())) {
                newSampleData.add(taskSpecimen);
            }
        }



        ip = deviceInfoService.getIp(deviceSerialNumber);
        // 根据序列号查询对应的附件配置表信息
        QueryWrapper<FSamplePoolConfigPO> wrapper = new QueryWrapper<>();
        wrapper.eq("device_serial_number", deviceSerialNumber);
        FSamplePoolConfigPO configPO = fSamplePoolConfigService.getOne(wrapper);
        if (configPO == null) {
            sendAndClose(session, "没有找到对应的附件配置信息");
        }
        String maxLocation = "";
        if (configPO.getIsAutoChangePool()){
            List<String> sampleIds = newSampleData.stream().map(obj -> obj.getSpecimenKey()).collect(Collectors.toCollection(ArrayList::new));
            QueryWrapper<FMeasureSample> eq = new QueryWrapper<>();
            eq.in("id", sampleIds);
            List<FMeasureSample> list = tMeasureSampleService.list(eq);
            maxLocation = list.stream().map(FMeasureSample::getSampleLocation).max(String::compareTo).orElse(null);
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();


        boolean poolStatus = false;
        if (configPO.getIsAutoChangePool()){
            List<FSamplePoolStatus> poolStatuses = JSON.parseArray(configPO.getPoolInfo(), FSamplePoolStatus.class);
            for (FSamplePoolStatus status : poolStatuses) {
                if (status.getPoolStatus()){
                    poolStatus = true;
                    break;
                }
            }
        }


        // 循环每个样品进行测量
        for (FTaskSpecimen newSampleDatum : newSampleData) {
            ///  设置样品池位置
            FMeasureSample sample = tMeasureSampleService.getById(newSampleDatum.getSpecimenKey());
            if (configPO.getIsAutoChangePool()){
                if (sample == null){
                    sendAndClose(session, "请先创建样品");
                    return;
                }

                if (poolStatus){
                    R<String> stringR = devMessageUtil.setCurrentCellPos(Integer.valueOf(sample.getSampleLocation()), ip);
                    if (!stringR.getSuccess()) {
                        sendAndClose(session, "设置出错");
                        return;
                    }
                    fSamplePoolConfigService.updateCurrentPoolNum(deviceSerialNumber, Integer.parseInt(sample.getSampleLocation()), updateBy);
                    JSONObject object = new JSONObject();
                    object.put("currentPoolNum",sample.getSampleLocation());
                    try {
                        session.sendMessage(new TextMessage(object.toJSONString()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            // 计算并执行定时任务
            int totalTasks = Integer.parseInt(param.getTotalNumber());  // 执行的总次数
            CountDownLatch latch = new CountDownLatch(totalTasks);  // 每个样品的定时任务数作为latch的计数

            executeTimedTaskForSample(session, param, baselineData, newSampleDatum, ip, latch,task,deviceSerialNumber);

            try {
                latch.await();  // 等待当前样品的所有任务完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (sample.getSampleLocation()==null||sample.getSampleLocation().equals("")||sample.getSampleLocation().equals(maxLocation)||!configPO.getIsAutoChangePool()) {
                break;
            }

            // 任务完成，清理资源后继续下一个样品
            System.out.println("当前样品任务执行完毕，开始执行下一个样品...");
        }

        // 关闭会话
        close(session);

    }

    // 执行单个样品的定时任务
    private void executeTimedTaskForSample(WebSocketSession session,
                                           FTimeSweepParamter param,
                                           ChangeParam baselineData,
                                           FTaskSpecimen newSampleDatum,
                                           String ip,
                                           CountDownLatch latch,
                                           FMeasureTask task,
                                           String deviceSerialNumber) {


        double interval = Double.parseDouble(param.getIntervalTime()); // 间隔时间（秒）

        // 根据总时长和间隔计算总任务次数
        int totalTasks = Integer.parseInt(param.getTotalNumber());  // 执行的总次数

        long currentStartTime = System.currentTimeMillis();  // 当前任务开始时间

        // 记录数据
        List<BigDecimal> x_data = new ArrayList<>();
        List<BigDecimal> y_data = new ArrayList<>();
        List<ChangeParam> originalData = new ArrayList<>();
        for (int i = 0; i < totalTasks; i++) {
            // 计算任务执行的时间点
            final long nextExecutionTime = (long) (currentStartTime + (i * interval * 1000L));

            int finalI = i;
            scheduler.schedule(() -> {
                try {
                    if (Thread.currentThread().isInterrupted()) {
                        return;  // 如果线程被中断，直接退出
                    }
                    // 计算延迟时间，确保任务精确执行
                    long delay = nextExecutionTime - System.currentTimeMillis();
                    if (delay > 0) {
                        Thread.sleep(delay);  // 等待下一个执行时间点
                    }

                    // 执行测量任务
                    BigDecimal xValue = BigDecimal.valueOf((System.currentTimeMillis() - currentStartTime) / 1000.0);
                    Command command = new Command(SetParamEnum.Measure.getComId(), new Object[]{});
                    ChangeParam changeParam = devMessageUtil.timeSweepGetMeasureData(command, ip);

                    if (changeParam == null) {
                        // 使用shutdown()方法结束任务
                        scheduler.shutdown();
                        // 发送错误信息并关闭会话
                        sendAndClose(session, "扫描失败");
                        latch.countDown(); // 完成当前任务
                        return;
                    }

                    Double v = formulaUtil.calculateWl(Double.parseDouble(changeParam.getWl()), deviceSerialNumber);
                    changeParam.setWl(String.valueOf(v));

                    // 记录原始数据
                    originalData.add(changeParam);

                    // 计算并推送 Y 值（光度模式等相关计算）
                    calculateAndPushYValue(param, baselineData, changeParam, xValue, x_data, y_data, session, newSampleDatum.getSpecimenKey());

                    if (finalI == totalTasks - 1) {
                        // 最后一次测量完成，结束任务
                        Constants.endTime = LocalDateTime.now();
                        saveData(session, originalData, x_data, y_data, newSampleDatum.getSpecimenKey(), task, deviceSerialNumber); // 保存数据
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                    sendAndClose(session, "任务执行出现错误");
                } finally {
                    latch.countDown(); // 每个定时任务完成后，计数器减一
                }
            }, nextExecutionTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);  // 精确调度任务到指定时间
        }
    }





    /**
     * 获取查询参数
     *
     * @param query
     * @param key
     * @return
     */
    private String getQueryParam(String query, String key) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals(key)) return pair[1];
        }
        return null;
    }



    /**
     * 计算并推送 Y 值（光度模式等相关计算）
     *
     * @param param
     * @param baselineData
     * @param changeParam
     * @param xValue
     * @param x_data
     * @param y_data
     * @param session
     * @param sampleId
     * @throws IOException
     */
    private void calculateAndPushYValue(
            FTimeSweepParamter param,
            ChangeParam baselineData,
            ChangeParam changeParam,
            BigDecimal xValue,
            List<BigDecimal> x_data,
            List<BigDecimal> y_data,
            WebSocketSession session,
            String sampleId) throws IOException {

        // 将总时长转换为bigDecimal类型
        BigDecimal totalTime = new BigDecimal(param.getScanTime());
        int compareTo = xValue.compareTo(totalTime);
        if (compareTo > 0) {
            xValue = totalTime;
        }

        if (param.getLuminosity().equals("1")) {
            String s = DataCountUtil.countTPercent(baselineData, changeParam, 1.0);
            String y = DataCountUtil.changeTPercentOrAbs2(null,s);
            BigDecimal integerPart = xValue.setScale(1, RoundingMode.DOWN);  // 直接舍弃小数部分
            x_data.add(xValue);
            y_data.add(new BigDecimal(y));
            JSONObject payload = new JSONObject();
            JSONArray points = new JSONArray();
            JSONObject point = new JSONObject();
            point.put("x", integerPart);
            Double decimalY = new Double(y);
            Double value = DataCountUtil.formatValue(digit, decimalY);
            String valueY = DataCountUtil.addZero(value, digit);
            point.put("y", valueY);
            point.put("nm", param.getWaveLength()+"nm");
            point.put("xType", "s");
            point.put("yType", "Abs");
            points.add(point);
            payload.put("data", points);
            payload.put("sampleId", sampleId);
            session.sendMessage(new TextMessage(payload.toJSONString()));
        } else if (param.getLuminosity().equals("2")) {
            String s = DataCountUtil.countTPercent(baselineData, changeParam, 1.0);
            BigDecimal integerPart = xValue.setScale(1, RoundingMode.DOWN);  // 直接舍弃小数部分
            x_data.add(xValue);
            y_data.add(new BigDecimal(s));
            JSONObject payload = new JSONObject();
            JSONArray points = new JSONArray();
            JSONObject point = new JSONObject();
            point.put("x", integerPart);
            Double decimalY = new Double(s);
            Double value = DataCountUtil.formatValue(digit, decimalY);
            String valueY = DataCountUtil.addZero(value, digit);
            point.put("y", valueY);
            point.put("nm", param.getWaveLength()+"nm");
            point.put("xType", "s");
            point.put("yType", "T%");
            points.add(point);
            payload.put("data", points);
            payload.put("sampleId", sampleId);
            session.sendMessage(new TextMessage(payload.toJSONString()));
        } else if (param.getLuminosity().equals("3")) {
            Double sData = Double.valueOf(changeParam.getS());
            String y = String.valueOf((sData / (4095 * 32)) * 100);
            BigDecimal integerPart = xValue.setScale(1, RoundingMode.DOWN);  // 直接舍弃小数部分
            x_data.add(xValue);
            y_data.add(new BigDecimal(y));
            JSONObject payload = new JSONObject();
            JSONArray points = new JSONArray();
            JSONObject point = new JSONObject();
            point.put("x", integerPart);
            Double decimalY = new Double(y);
            Double value = DataCountUtil.formatValue(digit, decimalY);
            String valueY = DataCountUtil.addZero(value, digit);
            point.put("y", valueY);
            point.put("nm", param.getWaveLength()+"nm");
            point.put("xType", "s");
            point.put("yType", "Es");
            points.add(point);
            payload.put("data", points);
            payload.put("sampleId", sampleId);
            session.sendMessage(new TextMessage(payload.toJSONString()));
        } else if (param.getLuminosity().equals("4")) {
            Double rData = Double.valueOf(changeParam.getR());
            String y = String.valueOf((rData / (4095 * 32)) * 100);
            BigDecimal integerPart = xValue.setScale(1, RoundingMode.DOWN);  // 直接舍弃小数部分
            x_data.add(xValue);
            y_data.add(new BigDecimal(y));
            JSONObject payload = new JSONObject();
            JSONArray points = new JSONArray();
            JSONObject point = new JSONObject();
            point.put("x", integerPart);
            Double decimalY = new Double(y);
            Double value = DataCountUtil.formatValue(digit, decimalY);
            String valueY = DataCountUtil.addZero(value, digit);
            point.put("y", valueY);
            point.put("nm", param.getWaveLength()+"nm");
            point.put("xType", "s");
            point.put("yType", "Er");
            points.add(point);
            payload.put("data", points);
            payload.put("sampleId", sampleId);
            session.sendMessage(new TextMessage(payload.toJSONString()));
        } else {
            sendAndClose(session, "光度模式错误");
            session.close();
        }
        System.err.println("Current JVM Timezone: " + TimeZone.getDefault().getID());
    }


    /**
     * 存储数据
     * @param session
     * @param originalData
     * @param x_data
     * @param y_data
     * @param sampleId
     * @param task
     * @throws IOException
     */
    private void saveData(WebSocketSession session,
                          List<ChangeParam> originalData,
                          List<BigDecimal> x_data,
                          List<BigDecimal> y_data,
                          String sampleId,
                          FMeasureTask task,
                          String deviceSerialNumber) {
        // 先对数据进行寻峰寻谷
        SystemAlgorithmConfVO confVO = systemAlgorithmConfService.queryAlgorithmConf();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("x_data", x_data);
        body.put("y_data", y_data);
        double height = Double.parseDouble(confVO.getPeakValleyDiff()) / 100;
        body.put("height", height);
        body.put("distance", confVO.getPeakInterval());
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request, Map.class).getBody();
        Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request, Map.class).getBody();
        List<Double> xDataFeng = (List<Double>) resultMap.get("peak_x");
        List<String> yDataFeng = (List<String>) resultMap.get("peak_y");
        List<Double> xDataGu = (List<Double>) resultMap1.get("valley_x");
        List<String> yDataGu = (List<String>) resultMap1.get("valley_y");
        // 创建一个测量数据对象
        FMeasureData tMeasureData = new FMeasureData();
        // 根据任务id和样品id去测量数据表中进行查询,根据序列号进行从大到小的排序,并读取最大的那一条
        // 如果发现测量数据表中有数据则将最大的序列号拿出来加1并放入新建的测量数据对象中
        FMeasureData data = tMeasureDataService.getOne(new QueryWrapper<FMeasureData>().eq("task_id", task.getId()).eq("sample_key", sampleId).orderByDesc("serial_number").last("LIMIT 1"));
        if (data != null) {
            tMeasureData.setSerialNumber(data.getSerialNumber() + 1);
        } else {
            tMeasureData.setSerialNumber(1);
        }
        List<String> listData = new ArrayList<>();
        for (int i = 0; i < x_data.size(); i++) {
            listData.add(x_data.get(i) + "," + y_data.get(i));
        }
        List<FPeakValleyMarking> peakValleys = new ArrayList<>();
        List<FPeakValleyMarking> markings = new ArrayList<>();
        for (int i = 0; i < xDataFeng.size(); i++) {
            FPeakValleyMarking marking = new FPeakValleyMarking();
            marking.setX(xDataFeng.get(i).toString());
            marking.setY(yDataFeng.get(i));
            marking.setType("波峰");
            peakValleys.add(marking);
        }
        for (int i = 0; i < xDataGu.size(); i++) {
            FPeakValleyMarking marking = new FPeakValleyMarking();
            marking.setX(xDataGu.get(i).toString());
            marking.setY(yDataGu.get(i));
            marking.setType("波谷");
            markings.add(marking);
        }
        tMeasureData.setTaskId(task.getId());
        tMeasureData.setSampleKey(sampleId);
        tMeasureData.setInstrumentData(JSON.toJSONString(originalData));
        tMeasureData.setOriginalCalculationData(JSON.toJSONString(listData));
        tMeasureData.setMeasurementStartTime(Constants.startTime);
        tMeasureData.setMeasurementEndTime(Constants.endTime);
        tMeasureData.setIsDelete("0");
        tMeasureData.setIsSave("1");
        boolean save = tMeasureDataService.save(tMeasureData);
        if (!save) {
            sendAndClose(session, "保存测量数据失败");
            return;
        }

        FMeasureAnalysisData analysisData = new FMeasureAnalysisData();
        FMeasureAnalysisData analysisDataLast = tMeasureAnalysisDataService.getOne(
                new QueryWrapper<FMeasureAnalysisData>()
                        .eq("task_id", task.getId())
                        .eq("sample_key", sampleId)
                        .orderByDesc("analysis_number")
                        .last("LIMIT 1")
        );
        analysisData.setAnalysisNumber(analysisDataLast != null ? analysisDataLast.getAnalysisNumber() + 1 : 1);
        // 根据样品id查询样品数据
        FMeasureSample sample = tMeasureSampleService.getById(sampleId);
        analysisData.setAnalysisName(sample.getSampleName()+"("+analysisData.getAnalysisNumber()+")");
        analysisData.setTaskId(task.getId());
        analysisData.setSampleKey(sampleId);
        analysisData.setMeasureDataId(tMeasureData.getId());
        analysisData.setAnalysisParam(JSON.toJSONString(originalData));
        analysisData.setAnalysisResult(JSON.toJSONString(listData));
        analysisData.setAnalysisType("5");
        analysisData.setPeak(com.alibaba.fastjson2.JSON.toJSONString(peakValleys));
        analysisData.setGrain(com.alibaba.fastjson2.JSON.toJSONString(markings));
        analysisData.setColor("#000000");
        analysisData.setIsDelete("0");
        analysisData.setIsSave("1");
        boolean save1 = tMeasureAnalysisDataService.save(analysisData);
        if (!save1) {
            sendAndClose(session, "保存分析数据失败");
            return;
        }

        ///  创建分析图片表的对象
        FMeasureAnalysisImage image = new FMeasureAnalysisImage();
        image.setAnalysisId(analysisData.getId());
        boolean save2 = tMeasureAnalysisImageService.save(image);
        if (!save2) {
            sendAndClose(session, "保存分析图片数据失败");
            return;
        }

        List<FTaskSpecimen> specimen = JSON.parseArray(task.getSampleKey(), FTaskSpecimen.class);
        for (FTaskSpecimen taskSpecimen : specimen) {
            if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                taskSpecimen.setIsTested("1");
                break;
            }
        }
        task.setSampleKey(JSON.toJSONString(specimen));
        task.setDeviceSerialNumber(deviceSerialNumber);
        boolean update = tMeasureTaskService.updateById(task);
        if (!update) {
            sendAndClose(session, "更新任务失败");
            return;
        }

    }

    /**
     * 全局数据清空
     */
    private void exceptionHandling() {
        Constants.startTime = null;
        Constants.endTime = null;
    }


    // 当WebSocket接收到消息时，会调用此方法
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 处理客户端发来的消息
        log.info("收到客户端消息: " + message.getPayload());
        if (message.getPayload().equals("close")){
            session.sendMessage(new TextMessage("停止成功"));
            sessions.remove(session.getId());
            session.close();
            // 如果线程正在被使用
            if (occupancyStatus){
                // 释放线程
                deviceInfoService.releaseDevice(deviceSerialNumber);
            }
        }
    }

    // 当WebSocket连接关闭时，会调用此方法
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        log.info("WebSocket连接已关闭: " + session.getId());
        this.currentSession = null;  // 清理会话

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();  // 强制停止所有任务
            log.info("定时任务已强制停止");
        }

        exceptionHandling();

        // 取消正在执行的任务
        if (future != null && !future.isDone()) {
            future.cancel(true);
            log.info("定时任务已强制停止");
        }

    }

    /**
     * 连接异常时触发
     * @param session
     * @param exception
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session.getId());
        try {
            session.close();
            deviceInfoService.releaseDevice(deviceSerialNumber);
        } catch (IOException e) {
            handleTransportError(session, e);
            Thread.currentThread().interrupt();
            return;
        }
        System.out.println("连接异常: " + exception.getMessage());
    }


    /**
     * 发送消息并关闭连接
     * @param session
     * @param msg
     */
    private void sendAndClose(WebSocketSession session, String msg) {
        try {
            // 释放设备
            deviceInfoService.releaseDevice(deviceSerialNumber);
            session.sendMessage(new TextMessage(msg));
            session.close();
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    /**
     * 关闭连接
     * @param session
     */
    private void close(WebSocketSession session) {
        try {
            // 释放设备
            deviceInfoService.releaseDevice(deviceSerialNumber);
            session.close();
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    /**
     * 发送消息并关闭连接2
     *
     * @param session
     * @param msg
     */
    private void sendClose(WebSocketSession session, String msg) {
        try {
            session.sendMessage(new TextMessage(msg));
            sessions.remove(session.getId());
            session.close();
        } catch (IOException e) {
            handleTransportError(session, e);
            Thread.currentThread().interrupt();
            return;
        }
    }


    /**
     * 构建命令
     * @param cmdEnum
     * @param value
     * @return
     */
    private Command buildCommand(SetParamEnum cmdEnum, Object value) {
        Command cmd = new Command();
        cmd.setCmdId(cmdEnum.getComId());
        cmd.setParams(new Object[]{value});
        return cmd;
    }
}
