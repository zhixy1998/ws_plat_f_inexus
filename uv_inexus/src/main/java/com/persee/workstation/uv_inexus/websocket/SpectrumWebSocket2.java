    package com.persee.workstation.uv_inexus.websocket;
    
    import com.alibaba.fastjson.JSONArray;
    import com.alibaba.fastjson.JSONObject;
    import com.alibaba.fastjson2.JSON;
    import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
    import com.persee.workstation.uv_inexus.constant.Constants;
    import com.persee.workstation.uv_inexus.constant.PythonPathConstant;
    import com.persee.workstation.uv_inexus.dto.measurementDto.SpectrumParameter;
    import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureData;
    import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
    import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTask;
    import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.PeakValleyMarking;
    import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
    import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
    import com.persee.workstation.uv_inexus.service.guide.measurementService.SpectrumService;
    import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureDataService;
    import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureMethodService;
    import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureTaskService;
    import com.persee.workstation.uv_inexus.service.guide.systemManageService.SystemAlgorithmConfService;
    import com.persee.workstation.uv_inexus.util.DataCountUtil;
    import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
    import com.persee.workstation.uv_inexus.util.changeUtil.Command;
    import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
    import com.persee.workstation.uv_inexus.util.nettyUtil.SetParamEnum;
    import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAlgorithmConfVO;
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
    import java.util.*;
    import java.util.concurrent.ConcurrentHashMap;
    
    import static java.lang.Thread.sleep;
    
    @Component
    public class SpectrumWebSocket2 extends TextWebSocketHandler {
    
        // 所有设备连接 <sessionId, session>
        private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
        // 假设的设备数据缓存队列
        private static final Map<String, List<String>> deviceDataCache = new ConcurrentHashMap<>();
    
    
        @Autowired
        private SpectrumService spectrumService;
        @Autowired
        private TMeasureTaskService tMeasureTaskService;
        @Autowired
        private DevMessageUtil devMessageUtil;
        @Autowired
        private RestTemplate restTemplate;
        @Autowired
        private TMeasureDataService traceDataService;
        @Autowired
        private TMeasureMethodService tMeasureMethodService;
        @Autowired
        private SystemAlgorithmConfService systemAlgorithmConfService;
        @Autowired
        private DeviceInfoService deviceInfoService;
        @Autowired
        private TMeasureDataService tMeasureDataService;
    
    
        /**
         * 前端连接成功 WebSocket 时触发
         *
         * @param session
         */
        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            if (Constants.endTime != null||Constants.startTime != null) {
                sendAndClose(session, "当前测试未结束,无法开启新的测试");
                return;
            }
    
            // 记录当前连接
            sessions.put(session.getId(), session);
    
            // 解析参数
            String query = Objects.requireNonNull(session.getUri()).getQuery();
            String taskId = getQueryParam(query, "taskId");
            String sampleId = getQueryParam(query, "sampleId");
            String deviceSerialNumber = getQueryParam(query, "deviceSerialNumber");
    
            if (taskId == null) {
                sendAndClose(session, "缺少 taskId 参数");
                return;
            }
            if (sampleId == null) {
                sendAndClose(session, "缺少 sampleId 参数");
                return;
            }
            if (deviceSerialNumber == null) {
                sendAndClose(session, "缺少 deviceSerialNumber 参数");
                return;
            }
            // 根据任务id查询任务数据
            TMeasureTask task = tMeasureTaskService.getById(taskId);
            if (task.getSampleKey()==null||task.getSampleKey().equals("")) {
                sendAndClose(session, "请先创建样品");
                return;
            }
            // 根据方法id查询方法数据
            TMeasureMethod method = tMeasureMethodService.getById(task.getMethodKey());
            // 将方法参数提取出来
            SpectrumParameter spectrumParameter = JSON.parseObject(method.getMeasurementParamInfo(), SpectrumParameter.class);
            // 将任务中的Rsd取出来
            List<ChangeParam> baselineData = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
            if (baselineData == null||baselineData.size()==0){
                sendAndClose(session, "请先设置基线数据");
                return;
            }
            // ==== 新增：获取重复模式参数 ====
            Integer repetitionTime = Integer.valueOf(spectrumParameter.getRepetitionTime());
            Integer timeInterval = Integer.valueOf(spectrumParameter.getTimeInterval());
            boolean repeatMode = "单池重复".equalsIgnoreCase(spectrumParameter.getRepetitionPattern());
            int repeatCount = repetitionTime != null ? repetitionTime : 1;
            long repeatInterval = timeInterval != null ? timeInterval : 0;
            // 开始获取数据
            String ip = deviceInfoService.getIp(deviceSerialNumber);
            Command command = new Command();
            command.setCmdId(SetParamEnum.Scan.getComId());
            command.setParams(new Object[]{});
            // 非重复模式：这里先发一次；重复模式：后面线程里循环里再发
            if (!repeatMode) {
                devMessageUtil.getMeasureData(command, ip);
            }
    
    
            // 往前端推送数据的线程
            new Thread(() -> {
                System.err.println("----------------------------------进入推送数据的线程");
                try {
                    // 等待仪器开始吐第一组数据（非重复模式已发命令；重复模式首组命令稍后发）
                    while (!repeatMode && Constants.instrumentData.size() == 0) {
                        sleep(50);
                    }
                    if (!repeatMode) {
                        Boolean flag = true;
                        int count = 0;
                        while (flag) {
                            List<String> data = new ArrayList<>(Constants.instrumentData);
                            if (data != null && !data.isEmpty()) {
                                int size = Constants.measurd_data.size();
                                int sum = data.size();
                                if (data.contains(Constants.EndData)) {
                                    flag = false;
                                }
                                // 如果size为0则跳过本次循环
                                if (size == 0) {
                                    continue;
                                }
    
                                if (data.get(sum - 1).equals(Constants.EndData)) {
                                    flag = false;
                                }
                                if (size<10&&flag)continue;
                                JSONArray dataArray = new JSONArray();
                                JSONObject payload = new JSONObject();
                                List<Double> x_data = new ArrayList<>();
                                List<Double> y_data = new ArrayList<>();
                                for (int i = 0; i < size; i++) {
    
                                    if (spectrumParameter.getLuminosity().equals("1")) {
                                        String s = DataCountUtil.countTPercent(baselineData.get(0), Constants.measurd_data.get(0), 1.0);
                                        String y = DataCountUtil.changeTPercentOrAbs2(null, s);
                                        Constants.originalCalculationData.add(baselineData.get(0).getWl() + "," + y);
                                        x_data.add(Double.parseDouble(baselineData.get(0).getWl()));
                                        y_data.add(Double.parseDouble(y));
                                        System.err.println("=============================================" + y);
                                    } else if (spectrumParameter.getLuminosity().equals("2")) {
                                        String s = DataCountUtil.countTPercent(baselineData.get(0), Constants.measurd_data.get(0), 1.0);
                                        String y = DataCountUtil.changeTPercentOrAbs2(s, null);
                                        Constants.originalCalculationData.add(baselineData.get(0).getWl() + "," + y);
                                        x_data.add(Double.parseDouble(baselineData.get(0).getWl()));
                                        y_data.add(Double.parseDouble(y));
                                        System.err.println("=============================================" + y);
                                    } else if (spectrumParameter.getLuminosity().equals("3")) {
                                        Double sData = Double.valueOf(Constants.measurd_data.get(0).getS());
                                        String y = String.valueOf((sData / (4095 * 32)) * 100);
                                        Constants.originalCalculationData.add(baselineData.get(0).getWl() + "," + y);
                                        x_data.add(Double.parseDouble(baselineData.get(0).getWl()));
                                        y_data.add(Double.parseDouble(y));
                                        System.err.println("=============================================" + y);
                                    } else if (spectrumParameter.getLuminosity().equals("4")) {
                                        Double rData = Double.valueOf(Constants.measurd_data.get(0).getR());
                                        String y = String.valueOf((rData / (4095 * 32)) * 100);
                                        Constants.originalCalculationData.add(baselineData.get(0).getWl() + "," + y);
                                        x_data.add(Double.parseDouble(baselineData.get(0).getWl()));
                                        y_data.add(Double.parseDouble(y));
                                        System.err.println("=============================================" + y);
                                    }
                                    System.err.println("++++++++" + Constants.measurd_data.get(0));
                                    System.err.println("++++++++" + baselineData.get(0));
                                    System.err.println("++++++++" + y_data);
                                    Constants.measurd_data.remove(0);
                                    baselineData.remove(0);
                                }
                                Map<String, Object> resultMap = new HashMap<>();
                                if (spectrumParameter.getLadderCorrection().equals("true")) {
                                    // 每次传过来的数据进行一次门限消除
                                    SystemAlgorithmConfVO systemAlgorithmConfVO = systemAlgorithmConfService.queryAlgorithmConf();
                                    if (systemAlgorithmConfVO == null) {
                                        session.sendMessage(new TextMessage("门限消除算法配置为空"));
                                    }
                                    HttpHeaders headers = new HttpHeaders();
                                    headers.setContentType(MediaType.APPLICATION_JSON);
                                    Map<String, Object> body = new HashMap<>();
                                    body.put("x_data", x_data);
                                    body.put("y_data", y_data);
                                    body.put("k", systemAlgorithmConfVO.getIntegratingSphere());
                                    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                                    resultMap = restTemplate.postForEntity(PythonPathConstant.REMOVE_STEP_OR_SPIKES, request, Map.class).getBody();
                                }
                                if (resultMap != null&&resultMap.size()>0) {
                                    y_data = (List<Double>) resultMap.get("smoothed_data");
                                }
                                x_data.stream().map(x -> String.valueOf(x)).forEach(Constants.x_data::add);
                                y_data.stream().map(y -> String.valueOf(y)).forEach(Constants.y_data::add);
                                // 传送给前端消除门限后的值
                                for (int j = 0; j < size; j++) {
                                    if (!data.isEmpty()
                                            && data.get(0).equals(Constants.EndData)) {
                                        break;
                                    }
                                    JSONObject point = new JSONObject();
                                    point.put("x", x_data.get(j));
                                    point.put("y", y_data.get(j));
                                    dataArray.add(point);
                                }
                                payload.put("data", dataArray);
                                if (session.isOpen()) {
                                    session.sendMessage(new TextMessage(payload.toJSONString()));
                                }
                            }
                        }
                    }else{
                        // ===== 重复模式 =====
                        for (int r = 0; r < repeatCount; r++) {
                            doOneMeasurement(session, command, spectrumParameter, task, taskId, sampleId, deviceSerialNumber,ip);
    
                            // 如果不是最后一次 → 等待 interval 再来下一次
                            if (r < repeatCount - 1) {
                                try { sleep(repeatInterval); } catch (InterruptedException e) { handleTransportError(session, e); }
                            }
                        }
    
                        // 全部完成后再告诉前端 finish
                        JSONObject payload = new JSONObject();
                        payload.put("result", "finish");
                        sendAndClose(session, String.valueOf(payload));
                    }
    
    
                } catch (Exception e) {
                    System.err.println("异常!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"+e);
                    handleTransportError(session, e);
                }
            }).start();
    
    
            // 创建线程循环如果公共变量为空则进行一秒的睡眠
            new Thread(() -> {
                System.err.println("-----------------------进入保存数据的线程");
                if (!"单池重复".equalsIgnoreCase(spectrumParameter.getRepetitionPattern())) {
                    // 原逻辑：等 endTime
                    while (Constants.endTime == null) {
                        try {
                            sleep(1000);
                        } catch (InterruptedException e) {
                            handleTransportError(session, e);
                        }
                    }
                } else {
                    // 重复模式：等平均值被写入 x_data/y_data（推送线程末尾会写）
                    while (Constants.x_data.isEmpty() || Constants.y_data.isEmpty()) {
                        try {
                            sleep(200);
                        } catch (InterruptedException e) {
                            handleTransportError(session, e);
                        }
                    }
                }
                if (Constants.x_data== null|| Constants.x_data.size()==0){
                    sendAndClose(session, "波长数据错误");
                    return;
                }
                if (Constants.y_data== null|| Constants.y_data.size()==0){
                    sendAndClose(session, "数据错误");
                    return;
                }
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                Map<String, Object> body = new HashMap<>();
                body.put("x_data", Constants.x_data);
                body.put("y_data", Constants.y_data);
                body.put("height", 0.5);
                body.put("distance", 1);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                Map<String, Object> resultMap = restTemplate.postForEntity(PythonPathConstant.PEAK_SEARCHING, request, Map.class).getBody();
                Map<String, Object> resultMap1 = restTemplate.postForEntity(PythonPathConstant.VALLEY_SEARCHING, request, Map.class).getBody();
                List<Double> xDataFeng = (List<Double>) resultMap.get("peak_x");
                List<Double> yDataFeng = (List<Double>) resultMap.get("peak_y");
                List<Double> xDataGu = (List<Double>) resultMap1.get("valley_x");
                List<Double> yDataGu = (List<Double>) resultMap1.get("valley_y");
                // 进行存储操作
                // 创建一个测量数据对象
                TMeasureData measureData = new TMeasureData();
                // 根据任务id和样品id去测量数据表中进行查询,根据序列号进行从大到小的排序,并读取最大的那一条
                // 如果发现测量数据表中有数据则将最大的序列号拿出来加1并放入新建的测量数据对象中
                TMeasureData data = tMeasureDataService.getOne(new QueryWrapper<TMeasureData>().eq("task_id", taskId).eq("sample_key", sampleId).orderByDesc("serial_number").last("LIMIT 1"));
                if (data != null) {
                    measureData.setSerialNumber(data.getSerialNumber() + 1);
                } else {
                    measureData.setSerialNumber(1);
                }
                List<String> listData = new ArrayList<>();
                for (int i = 0; i < Constants.x_data.size(); i++) {
                    listData.add(Constants.x_data.get(i) + "," + Constants.y_data.get(i));
                }
                List<PeakValleyMarking> peakValleys = new ArrayList<>();
                List<PeakValleyMarking> markings = new ArrayList<>();
    
                for (int i = 0; i < xDataFeng.size(); i++) {
                    PeakValleyMarking marking = new PeakValleyMarking();
                    marking.setX(xDataFeng.get(i).toString());
                    marking.setY(yDataFeng.get(i).toString());
                    marking.setType("波峰");
                    peakValleys.add(marking);
                }
                for (int i = 0; i < xDataGu.size(); i++) {
                    PeakValleyMarking marking = new PeakValleyMarking();
                    marking.setX(xDataGu.get(i).toString());
                    marking.setY(yDataGu.get(i).toString());
                    marking.setType("波谷");
                    markings.add(marking);
                }
                measureData.setTaskId(taskId);
                measureData.setSampleKey(sampleId);
                measureData.setInstrumentData(JSON.toJSONString(Constants.instrumentData));
                measureData.setOriginalCalculationData(JSON.toJSONString(listData));
                measureData.setMeasurementStartTime(Constants.startTime);
                measureData.setMeasurementEndTime(Constants.endTime);
                measureData.setPeak(JSON.toJSONString(peakValleys));
                measureData.setGrain(JSON.toJSONString(markings));
                measureData.setIsDelete("0");
                boolean save = traceDataService.save(measureData);
                if (!save) {
                    sendAndClose(session, "保存测量数据失败");
                    return;
                }
                List<TaskSpecimen> specimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
                for (TaskSpecimen taskSpecimen : specimen) {
                    if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                        taskSpecimen.setIsTested("1");
                    }
                }
                task.setSampleKey(JSON.toJSONString(specimen));
                task.setDeviceSerialNumber(deviceSerialNumber);
                boolean update = tMeasureTaskService.updateById(task);
                if (!update) {
                    sendAndClose(session, "更新任务失败");
                    return;
                }
                System.err.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                JSONObject payload = new JSONObject();
                payload.put("result", "finish");
                // 将公共的变量中的数据清除
                sendAndClose(session, String.valueOf(payload));
            }).start();
        }
    
        /**
         * 收到前端消息时触发
         *
         * @param session
         * @param message
         * @throws IOException
         */
        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
            String msg = message.getPayload();
            System.out.println("收到前端消息: " + msg);
    
            // 示例：如果前端发来"save"，就保存当前缓存
            if ("save".equalsIgnoreCase(msg)) {
                String deviceId = getQueryParam(session.getUri().getQuery(), "device_id");
                if (deviceId != null && deviceDataCache.containsKey(deviceId)) {
                    List<String> toSave = new ArrayList<>(deviceDataCache.get(deviceId));
                    System.out.println("保存数据[" + deviceId + "]: " + toSave.size() + "条");
                    // TODO: 持久化保存到数据库
                    deviceDataCache.get(deviceId).clear();
                }
            }
        }
    
        /**
         * 前端连接关闭时触发
         * @param session
         * @param status
         */
        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            sessions.remove(session.getId());
            System.out.println("连接关闭: " + session.getId());
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
            } catch (IOException e) {
                handleTransportError(session, e);
            }
            exceptionHandling();
            System.out.println("连接异常: " + exception.getMessage());
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
         * 发送消息并关闭连接
         * @param session
         * @param msg
         */
        private void sendAndClose(WebSocketSession session, String msg) {
            try {
                session.sendMessage(new TextMessage(msg));
                exceptionHandling();
                sessions.remove(session.getId());
                session.close();
            } catch (IOException e) {
                handleTransportError(session, e);
            }
        }
    
        /**
         * 全局数据清空
         */
        private void exceptionHandling() {
            Constants.x_data.clear();
            Constants.y_data.clear();
            Constants.field3Values.clear();
            Constants.instrumentData.clear();
            Constants.originalCalculationData.clear();
            Constants.startTime = null;
            Constants.endTime = null;
        }
    
        /**
         * 计算光度模式的Abs/T%
         * @param spectrumParameter
         * @param baselineCopy
         * @param x
         * @param y
         */
        private void calcOnePoint(SpectrumParameter spectrumParameter,
                                  List<ChangeParam> baselineCopy,
                                  List<Double> x,
                                  List<Double> y) {
            if (spectrumParameter.getLuminosity().equals("1")) {
                String s = DataCountUtil.countTPercent(baselineCopy.get(0), Constants.measurd_data.get(0), 1.0);
                String yy = DataCountUtil.changeTPercentOrAbs2(null, s);
                Constants.originalCalculationData.add(baselineCopy.get(0).getWl() + "," + yy);
                x.add(Double.parseDouble(baselineCopy.get(0).getWl()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("2")) {
                String s = DataCountUtil.countTPercent(baselineCopy.get(0), Constants.measurd_data.get(0), 1.0);
                String yy = DataCountUtil.changeTPercentOrAbs2(s, null);
                Constants.originalCalculationData.add(baselineCopy.get(0).getWl() + "," + yy);
                x.add(Double.parseDouble(baselineCopy.get(0).getWl()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("3")) {
                Double sData = Double.valueOf(Constants.measurd_data.get(0).getS());
                String yy = String.valueOf((sData / (4095 * 32)) * 100);
                Constants.originalCalculationData.add(baselineCopy.get(0).getWl() + "," + yy);
                x.add(Double.parseDouble(baselineCopy.get(0).getWl()));
                y.add(Double.parseDouble(yy));
            } else if (spectrumParameter.getLuminosity().equals("4")) {
                Double rData = Double.valueOf(Constants.measurd_data.get(0).getR());
                String yy = String.valueOf((rData / (4095 * 32)) * 100);
                Constants.originalCalculationData.add(baselineCopy.get(0).getWl() + "," + yy);
                x.add(Double.parseDouble(baselineCopy.get(0).getWl()));
                y.add(Double.parseDouble(yy));
            }
            Constants.measurd_data.remove(0);
            baselineCopy.remove(0);
        }
    
    
        // 每次测量
        private void doOneMeasurement(WebSocketSession session,
                                      Command command,
                                      SpectrumParameter spectrumParameter,
                                      TMeasureTask task,
                                      String taskId,
                                      String sampleId,
                                      String deviceSerialNumber,
                                      String ip) throws IOException {

            // 1. 发送测量命令
            devMessageUtil.getMeasureData(command, ip);

            List<ChangeParam> baselineCopy = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
            List<Double> x = new ArrayList<>();
            List<Double> y = new ArrayList<>();

            // 2. 采集数据，实时推送（每 10 条推一次）
            int pushCount = 0;
            while (true) {
                List<String> snapshot = new ArrayList<>(Constants.instrumentData);
                if (!snapshot.isEmpty() && snapshot.get(snapshot.size() - 1).equals(Constants.EndData)) {
                    while (!baselineCopy.isEmpty() && !Constants.measurd_data.isEmpty()) {
                        calcOnePoint(spectrumParameter, baselineCopy, x, y);
                        pushCount = pushDataIfNeeded(session, x, y, pushCount);
                    }
                    break;
                }
                while (!baselineCopy.isEmpty() && !Constants.measurd_data.isEmpty()) {
                    calcOnePoint(spectrumParameter, baselineCopy, x, y);
                    pushCount = pushDataIfNeeded(session, x, y, pushCount);
                }
                try { sleep(10); } catch (InterruptedException e) { handleTransportError(session, e); }
            }

            // 3. 推送最后不足10条的数据
            pushRemainingData(session, x, y, pushCount);

            // 4. 保存一次完整测量
            saveOneMeasurement(task, taskId, sampleId, deviceSerialNumber, x, y);

            // 5. 清理缓存
            Constants.instrumentData.clear();
            Constants.measurd_data.clear();
            Constants.originalCalculationData.clear();
            Constants.endTime = null;
        }


        // 推送逻辑：每 10 个点推送一次
        private int pushDataIfNeeded(WebSocketSession session, List<Double> x, List<Double> y, int pushCount) throws IOException {
            if (x.size() >= (pushCount + 1) * 10) {
                JSONArray dataArray = new JSONArray();
                for (int i = pushCount * 10; i < x.size(); i++) {
                    JSONObject point = new JSONObject();
                    point.put("x", x.get(i));
                    point.put("y", y.get(i));
                    dataArray.add(point);
                }
                JSONObject payload = new JSONObject();
                payload.put("data", dataArray);
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload.toJSONString()));
                }
                pushCount++;
            }
            return pushCount;
        }

        // 推送剩余数据（最后不足10条的情况）
        private void pushRemainingData(WebSocketSession session, List<Double> x, List<Double> y, int pushCount) throws IOException {
            int startIndex = pushCount * 10;
            if (startIndex < x.size()) {
                JSONArray dataArray = new JSONArray();
                for (int i = startIndex; i < x.size(); i++) {
                    JSONObject point = new JSONObject();
                    point.put("x", x.get(i));
                    point.put("y", y.get(i));
                    dataArray.add(point);
                }
                JSONObject payload = new JSONObject();
                payload.put("data", dataArray);
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload.toJSONString()));
                }
            }
        }


        // 保存一次测量结果
        private void saveOneMeasurement(TMeasureTask task,
                                        String taskId,
                                        String sampleId,
                                        String deviceSerialNumber,
                                        List<Double> x,
                                        List<Double> y) {
            List<String> listData = new ArrayList<>();
            for (int i = 0; i < x.size(); i++) {
                listData.add(x.get(i) + "," + y.get(i));
            }
    
            TMeasureData measureData = new TMeasureData();
            TMeasureData lastData = tMeasureDataService.getOne(
                    new QueryWrapper<TMeasureData>()
                            .eq("task_id", taskId)
                            .eq("sample_key", sampleId)
                            .orderByDesc("serial_number")
                            .last("LIMIT 1")
            );
            measureData.setSerialNumber(lastData != null ? lastData.getSerialNumber() + 1 : 1);
            measureData.setTaskId(taskId);
            measureData.setSampleKey(sampleId);
            measureData.setInstrumentData(JSON.toJSONString(Constants.instrumentData));
            measureData.setOriginalCalculationData(JSON.toJSONString(listData));
            measureData.setMeasurementStartTime(Constants.startTime);
            measureData.setMeasurementEndTime(Constants.endTime);
            measureData.setIsDelete("0");
    
            traceDataService.save(measureData);
    
            // 更新任务状态
            List<TaskSpecimen> specimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            for (TaskSpecimen taskSpecimen : specimen) {
                if (taskSpecimen.getSpecimenKey().equals(sampleId)) {
                    taskSpecimen.setIsTested("1");
                }
            }
            task.setSampleKey(JSON.toJSONString(specimen));
            task.setDeviceSerialNumber(deviceSerialNumber);
            tMeasureTaskService.updateById(task);
        }
    
    
    
    }
