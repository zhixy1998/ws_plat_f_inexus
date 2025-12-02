/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dto.measurementDto.QueryDataDTO;
import com.persee.workstation.uv_inexus.dto.measurementDto.RationParameter;
import com.persee.workstation.uv_inexus.enums.measurementEnums.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.*;
import com.persee.workstation.uv_inexus.util.DataCountUtil;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.Command;
import com.persee.workstation.uv_inexus.util.changeUtil.DevMessageUtil;
import com.persee.workstation.uv_inexus.util.pythonUtil.FittingReceiveInfo;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RationMeasureWebSocket extends TextWebSocketHandler {
    // 所有设备连接 <sessionId, session>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 假设的设备数据缓存队列
    private static final Map<String, List<String>> deviceDataCache = new ConcurrentHashMap<>();

    private static volatile Boolean empty = false;
    private static volatile Boolean lastIsEmpty = false;
    private static volatile Double minuend = 0.0;
    //存放空白样数据（计算后的值）
    private static volatile List<RationSaveCountDataInfo> emptyDataList = new ArrayList<>();
    //存放空白样计算出的Abs的值
    private static volatile List<RationCountDataInfo> emptyList = new ArrayList<>();

    @Autowired
    private TMeasureTaskService tMeasureTaskService;
    @Autowired
    private DevMessageUtil devMessageUtil;

    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    @Autowired
    private TMeasureDataService tMeasureDataService;

    @Autowired
    RationService rationService;

    @Autowired
    TMeasureSampleService tMeasureSampleService;

    @Autowired
    TMeasureAnalysisDataService tMeasureAnalysisDataService;

    @Autowired
    DeviceInfoService infoService;

    @Autowired
    TMeasureTaskAttachmentService attachmentService;

    @Value("1")
    String right;

    @Value("0")
    String error;

    private static volatile int status = 0;

    private static volatile int digit = 4;

    private static volatile boolean isAvg = false;
    private static volatile boolean isSd = false;
    private static volatile boolean isRsd = false;
    private static volatile boolean reCount = false;
    /**
     * 前端连接成功 WebSocket 时触发
     * @param session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {

        if (status != 0) {
            sendAndClose(session, "已有连接，不要重复操作");
        }
        status = 1;
        // 记录当前连接
        sessions.put(session.getId(), session);
        initParams();

        // 解析参数
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        String taskId = getQueryParam(query, "taskId");
        String deviceSerialNumber= getQueryParam(query, "deviceSerialNumber");
        String sampleKind= getQueryParam(query, "sampleKind");
        int di = Integer.parseInt(getQueryParam(query, "digit"));
        isAvg = StringUtils.hasText(getQueryParam(query, "avg"));
        isSd = StringUtils.hasText(getQueryParam(query, "sd"));
        isRsd = StringUtils.hasText(getQueryParam(query, "rsd"));

        if (deviceSerialNumber == null) {
            sendAndClose(session, "未连接仪器");
            return;
        }

        if (taskId == null) {
            sendAndClose(session, "请创建任务后再测量");
            return;
        }
        if(!StringUtils.hasText(sampleKind)){
            sendAndClose(session, "请选择样品类型");
            return;
        }

        TMeasureTask task = tMeasureTaskService.getById(taskId);
        if (task == null || !StringUtils.hasText(task.getSampleKey())) {
            sendAndClose(session, "任务或样品不存在");
            return;
        }

        List<TaskSpecimen> taskSpecimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);

        TMeasureMethod byId = tMeasureMethodService.getById(task.getMethodKey());
        RationParameter parameter = JSON.parseObject(byId.getMeasurementParamInfo(), RationParameter.class);

        if (parameter.getWaveLength() == null || parameter.getWaveLength().isEmpty()) {
            log.info("缺少波长， 无法测量");
            sendAndClose(session, "缺少波长， 无法测量");
            return;
        }

        if (di < 0) {
            digit = Constants.DEFAULT_DECIMAL_NUM;
        } else {
            digit = di;
        }
        R<TMeasureTaskAttachment> attachmentR = attachmentService.queryByTaskId(taskId);
        if (attachmentR.getSuccess()) {
            TMeasureTaskAttachment attachment = attachmentR.getData();
            if (attachment != null && StringUtils.hasText(attachment.getDataParam())) {
                QueryDataDTO queryDataDTO = JSON.parseObject(attachment.getDataParam(), QueryDataDTO.class);
                if (queryDataDTO.getDigit() != digit) {
                    reCount = true;
                }
            }
        }

        // 检查任务中是否存在空白样本，如果存在则设置empty为true
        empty = taskSpecimen.stream()
                .anyMatch(specimen ->
                        specimen.getSampleType() != null &&
                        specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())
                );

        //后续添加仪器状态检测
        String ip = infoService.getIp(deviceSerialNumber);
        if (sampleKind.equals(SampleTypeEnum.kind_stand.getIdentity())) {
            boolean repeat = parameter.getRepeatMode().equals("单池重复");

            /*
             * 循环所有样品
             * 删除的过滤
             * 测过的，要处理
             */

            //参与曲线拟合的数据
            List<RationCountDataInfo> fitCurveData = new ArrayList<>();
            //参与曲线拟合的参数
            List<String> fitCurveParams = new ArrayList<>();
            //测过的未知样，曲线拟合之后要重新计算
            List<TaskSpecimen> testUnknowSample = new ArrayList<>();

            for (TaskSpecimen specimen : taskSpecimen) {
                if (specimen.getDeleteType().equals(right)) {
                    System.out.println("删除的样品,不需要测量");
                    continue;
                }

                TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                if (!sample.getSampleKind().equals(sampleKind)) {

                    if (sample.getSampleKind().equals(SampleTypeEnum.kind_unknown.getIdentity()) && specimen.getIsTested().equals(right)) {
                        testUnknowSample.add(specimen);
                    }

                    log.info("不是需要的样品，跳过");
                    continue;
                }

                if (specimen.getIsTested().equals(right)) {
                    /*
                    测过的样品
                    处理空白样-----空白样统一处理，处理之后的放公共变量里
                    标准样需要返回分析结果，ABS或sumAbs
                    重新计算。遍历过程要保障，都是测过的样品，顺序不能乱
                    标准样，首先，取原始数据，计算abs值，计算sumAbs值
                    保存数据库？不对，测过的应该不会变，所以，直接取值
                    空白样一样，直接取值存即可
                     */
                    LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(TMeasureData::getTaskId, taskId)
                            .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                            .orderByDesc(TMeasureData::getSerialNumber)
                            .last("limit 1");
                    TMeasureData data = tMeasureDataService.getOne(queryWrapper);
                    List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
                    if (list != null && !list.isEmpty()) {
                        RationCountDataInfo ansData;
                        RationSaveCountDataInfo dealData = new RationSaveCountDataInfo();
                        dealData.setSampleId(specimen.getSpecimenKey());
                        dealData.setTaskId(taskId);
                        dealData.setMeasureDataId(data.getId());

                        if (repeat) {
                            List<RationWavelengthDataInfo> wavelengthDataInfos = countRepeatData(list, digit);
                            R<RationCountDataInfo> absValue = rationService.countAbsValue(wavelengthDataInfos, parameter, digit);
                            if (!absValue.getSuccess()) {
                                log.info("计算abs失败");
                                continue;
                            }
                            ansData = absValue.getData();

                        } else {

                            R<RationCountDataInfo> absValue = rationService.countAbsValue(list, parameter, digit);
                            if (!absValue.getSuccess()) {
                                log.info("重新计算abs失败");
                                continue;
                            }
                            ansData = absValue.getData();
                        }

                        if (specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {
                            lastIsEmpty = true;

                            emptySampleDeal(digit, ansData, dealData);
                        } else {
                            lastIsEmpty = false;
                            //测量过的数据，非空白样，添加标准浓度为了拟合曲线
                            //TODO 未来需要添加质量和浓度的区分
                            fitCurveParams.add(sample.getNormalConcentration());

                            if (empty) {
                                RationCountDataInfo sumAbsData = new RationCountDataInfo();
                                sumAbsData.setHeadName(SampleTableEnum.SUM_Abs.getName());
                                sumAbsData.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
                                sumAbsData.setColValue(ansData.getColValue() - minuend);
                                fitCurveData.add(sumAbsData);
                            } else {
                                fitCurveData.add(ansData);
                            }
                        }
                    }
                }

                /*
                 * 测量
                 * 区分重复与不重复测，测量 + 计算出abs值一起
                 */
                R<RationDataVO> r;
                if (repeat) {
                    //重复测量，单独处理！
                    r = singleSampleRepeatMeasure(sample, parameter, task, ip, session.getId());

                } else {
                    try {
                        r = singleSampleSimpleMeasure(sample, parameter, task, ip, session.getId());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                if (!r.getSuccess()) {
                    sendAndClose(session, r.getMessage());
                    return;
                }
                RationDataVO data = r.getData();
                List<RationCountDataInfo> measureAbsDataList = data.getCountData();

                RationCountDataInfo absData = new RationCountDataInfo();
                for (RationCountDataInfo dataInfo : measureAbsDataList) {
                    if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                        BeanUtils.copyProperties(dataInfo, absData);
                        break;
                    }
                }
                if (sample.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {
                    lastIsEmpty = true;
                    R<RationSaveCountDataInfo> saved = rationService.saveSampleMeasureData(data);
                    if (!saved.getSuccess()) {
                        log.info("数据保存异常");
                        sendAndClose(session, saved.getMessage());
                        return;
                    }

                    RationSaveCountDataInfo measureData = saved.getData();
                    String measureDataId = measureData.getMeasureDataId();

                    RationSaveCountDataInfo dealData = new RationSaveCountDataInfo();
                    dealData.setTaskId(taskId);
                    dealData.setSampleId(sample.getId());
                    dealData.setMeasureDataId(measureDataId);

                    emptySampleDeal(digit, absData, dealData);
                    for (RationSaveCountDataInfo info : emptyDataList) {
                        String sampleId = info.getSampleId();
                        for (RationCountDataInfo countDatum : info.getCountData()) {
                            if (countDatum.getIdentity().equals(SampleTableEnum.SUM_Abs.getIdentity())) {
                                sampleSend(session, sampleId, countDatum.getIdentity(), null, String.valueOf(countDatum.getColValue()));
                            }
                        }

                    }
                    continue;
                } else {
                    lastIsEmpty = false;
                    //测量成功后，非空白样，添加标准浓度为了拟合曲线
                    //TODO 未来需要添加质量和浓度的区分
                    fitCurveParams.add(sample.getNormalConcentration());

                    if (empty) {
                        RationCountDataInfo sumAbsData = new RationCountDataInfo();
                        sumAbsData.setHeadName(SampleTableEnum.SUM_Abs.getName());
                        sumAbsData.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
                        double v = absData.getColValue() - minuend;
                        sumAbsData.setColValue(DataCountUtil.formatValue(digit, v));
                        fitCurveData.add(sumAbsData);
                        measureAbsDataList.add(sumAbsData);

                        sampleSend(session, sample.getId(), sumAbsData.getIdentity(), null, String.valueOf(sumAbsData.getColValue()));

                    } else {
                        fitCurveData.add(absData);
                    }
                    sendPoint(session, sample.getNormalConcentration(), absData.getColValue(), parameter);

                    data.setCountData(measureAbsDataList);
                    rationService.saveSampleMeasureData(data);
                }
                specimen.setIsTested(right);
            }
            task.setSampleKey(JSON.toJSONString(taskSpecimen));
            tMeasureTaskService.updateById(task);

            //所有样品测完，拟合曲线
            //拟合曲线
            R<FittingReceiveInfo> infoR = rationService.fittingCurve(fitCurveData, empty, task.getMethodKey(), fitCurveParams);

            if (!infoR.getSuccess()) {
                sendAndClose(session, infoR.getMessage());
                return;
            }
            FittingReceiveInfo fittingReceiveInfo = infoR.getData();
            String fitParam = JSON.toJSONString(fittingReceiveInfo);
            task.setFittingParam(fitParam);
            tMeasureTaskService.updateById(task);

            //绘图
            RationFitCurveVO curveData = rationService.curveData(fittingReceiveInfo);

            if (session.isOpen()) {
                JSONObject payload = new JSONObject();
                payload.put("R2", curveData.getR2());
                payload.put("dataList", curveData.getDataList());
                payload.put("curveType", curveData.getCurveType());
                payload.put("equation", curveData.getEquation());
                payload.put("equationType", RationCurveParamEnums.getName(parameter.getEquationType()));
                payload.put("coefficientList", curveData.getCoefficientList());
                session.sendMessage(new TextMessage(payload.toJSONString()));
            }

            if (!testUnknowSample.isEmpty()) {
                reCountUnknowSampleData(session, testUnknowSample, taskId, fittingReceiveInfo, parameter);
            }


        } else if (sampleKind.equals(SampleTypeEnum.kind_unknown.getIdentity())) {
            boolean repeat = parameter.getRepeatMode().equals("单池重复");
            /*
             * 循环所有样品
             * 删除的过滤
             * 测过的，要处理
             */
            for (TaskSpecimen specimen : taskSpecimen) {
                if (specimen.getDeleteType().equals(right)) {
                    System.out.println("删除的样品,不需要测量");
                    continue;
                }

                TMeasureSample sample = tMeasureSampleService.getById(specimen.getSpecimenKey());
                if (!sample.getSampleKind().equals(sampleKind)) {
                    log.info("不是需要的样品，跳过");
                    continue;
                }

                if (specimen.getIsTested().equals(right)) {
                    /*
                    测过的样品，未知样，只需要处理空白即可
                     */
                    if (specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {
                        //只处理空白样
                        LambdaQueryWrapper<TMeasureData> queryWrapper = new LambdaQueryWrapper<>();
                        queryWrapper.eq(TMeasureData::getTaskId, taskId)
                                .eq(TMeasureData::getSampleKey, specimen.getSpecimenKey())
                                .orderByDesc(TMeasureData::getSerialNumber)
                                .last("limit 1");
                        TMeasureData data = tMeasureDataService.getOne(queryWrapper);
                        List<RationWavelengthDataInfo> list = JSON.parseArray(data.getOriginalCalculationData(), RationWavelengthDataInfo.class);
                        if (list != null && !list.isEmpty()) {
                            RationCountDataInfo ansData;

                            RationSaveCountDataInfo dealData = new RationSaveCountDataInfo();
                            dealData.setSampleId(specimen.getSpecimenKey());
                            dealData.setTaskId(taskId);
                            dealData.setMeasureDataId(data.getId());

                            if (repeat) {
                                List<RationWavelengthDataInfo> wavelengthDataInfos = countRepeatData(list, digit);
                                R<RationCountDataInfo> absValue = rationService.countAbsValue(wavelengthDataInfos, parameter, digit);
                                if (!absValue.getSuccess()) {
                                    log.info("数据处理失败");
                                    continue;
                                }
                                ansData = absValue.getData();

                            } else {

                                R<RationCountDataInfo> absValue = rationService.countAbsValue(list, parameter, digit);
                                if (!absValue.getSuccess()) {
                                    log.info("数据处理失败");
                                    continue;
                                }
                                ansData = absValue.getData();
                            }

                            emptySampleDeal(digit, ansData, dealData);
                        }
                    }
                }

                /*
                 * 测量
                 * 区分重复与不重复测，测量 + 计算出abs值一起
                 */
                R<RationDataVO> r;
                if (repeat) {
                    //重复测量，单独处理！
                    r = singleSampleRepeatMeasure(sample, parameter, task, ip, session.getId());

                } else {
                    try {
                        r = singleSampleSimpleMeasure(sample, parameter, task, ip, session.getId());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                if (!r.getSuccess()) {
                    sendAndClose(session, r.getMessage());
                    return;
                }
                RationDataVO data = r.getData();

                //保存数据
                R<RationSaveCountDataInfo> saved = rationService.saveSampleMeasureData(data);
                if (!saved.getSuccess()) {
                    log.info("数据保存异常");
                    sendAndClose(session, saved.getMessage());
                    return;
                }
                RationSaveCountDataInfo saveDataInfo = saved.getData();

                List<RationCountDataInfo> measureAbsDataList = saveDataInfo.getCountData();

                RationCountDataInfo absData = new RationCountDataInfo();
                for (RationCountDataInfo dataInfo : measureAbsDataList) {
                    if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                        BeanUtils.copyProperties(dataInfo, absData);
                        break;
                    }
                }
                if (sample.getSampleType().equals(SampleTypeEnum.empty.getIdentity())) {
                    lastIsEmpty = true;

                    String measureDataId = saveDataInfo.getMeasureDataId();

                    RationSaveCountDataInfo dealData = new RationSaveCountDataInfo();
                    dealData.setTaskId(taskId);
                    dealData.setSampleId(sample.getId());
                    dealData.setMeasureDataId(measureDataId);

                    emptySampleDeal(digit, absData, dealData);
                    for (RationSaveCountDataInfo info : emptyDataList) {
                        String sampleId = info.getSampleId();
                        for (RationCountDataInfo countDatum : info.getCountData()) {
                            if (countDatum.getIdentity().equals(SampleTableEnum.SUM_Abs.getIdentity())) {
                                sampleSend(session, sampleId, countDatum.getIdentity(), null, String.valueOf(countDatum.getColValue()));
                            }
                        }

                    }
                    continue;
                } else {
                    lastIsEmpty = false;
                    //TODO 未来需要添加质量和浓度的区分
                    /*
                    未知样处理----计算sumAbs值，计算拟合浓度，计算公式，保存结果
                    如果有空白样，和没有空白样
                    统一一个参数，然后根据有无空白，赋值
                    然后各类计算
                    最后保存。。。。应该先保存一次测量结果，在保存计算结果，
                     */
                    double countValue = 0.0;

                    if (empty) {
                        RationCountDataInfo sumAbsData = new RationCountDataInfo();
                        sumAbsData.setHeadName(SampleTableEnum.SUM_Abs.getName());
                        sumAbsData.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
                        double v = absData.getColValue() - minuend;
                        sumAbsData.setColValue(DataCountUtil.formatValue(digit, v));
                        countValue = sumAbsData.getColValue();
                        measureAbsDataList.add(sumAbsData);
                        sampleSend(session, sample.getId(), sumAbsData.getIdentity(), null, String.valueOf(sumAbsData.getColValue()));
                    } else {
                        countValue = absData.getColValue();
                    }
                    FittingReceiveInfo fittingInfo = null;
                    if (StringUtils.hasText(task.getFittingParam())) {
                        fittingInfo= JSON.parseObject(task.getFittingParam(), FittingReceiveInfo.class);
                    }
                    if (fittingInfo != null) {
                        String ans = rationService.valueCalculate(fittingInfo, countValue, parameter.getEquationType());
                        RationCountDataInfo valueData = new RationCountDataInfo();
                        valueData.setHeadName(SampleTableEnum.FIT_CONCENTRATION.getName());
                        valueData.setIdentity(SampleTableEnum.FIT_CONCENTRATION.getIdentity());
                        valueData.setColValue(ans==null? null : Double.valueOf(ans));
                        measureAbsDataList.add(valueData);
                    }

                    //定量的公式计算----待定
//                    rationService.formula()

                    saveDataInfo.setCountData(measureAbsDataList);
                    rationService.saveAnalyzeData(saveDataInfo);
                }
                specimen.setIsTested(right);
            }
            task.setSampleKey(JSON.toJSONString(taskSpecimen));
            tMeasureTaskService.updateById(task);
        } else {
            log.info("未知的样品种类");
        }
    }

    public void reCountUnknowSampleData(WebSocketSession session, List<TaskSpecimen> dealSampleList, String taskId, FittingReceiveInfo curveData, RationParameter parameter) throws IOException {

        boolean empty = dealSampleList.stream().anyMatch(specimen -> specimen.getSampleType().equals(SampleTypeEnum.empty.getIdentity()));

        for (TaskSpecimen specimen : dealSampleList) {

            List<RationCountDataInfo> newData = new ArrayList<>();

            RationCountDataInfo toCountData = new RationCountDataInfo();

            TMeasureAnalysisData saveData = new TMeasureAnalysisData();
            String analysisDataId = null;

            LambdaQueryWrapper<TMeasureAnalysisData> analysisDataQueryWrapper = new LambdaQueryWrapper<>();
            analysisDataQueryWrapper.eq(TMeasureAnalysisData::getTaskId, taskId)
                    .eq(TMeasureAnalysisData::getSampleKey, specimen.getSpecimenKey())
                    .orderByDesc(TMeasureAnalysisData::getDataVersion);

            List<TMeasureAnalysisData> list = tMeasureAnalysisDataService.list(analysisDataQueryWrapper);
            for (TMeasureAnalysisData analysisData : list) {
                if (StringUtils.hasText(analysisData.getMeasureDataId()) && StringUtils.hasText(analysisData.getAnalysisResult())) {
                    List<RationCountDataInfo> countDataInfoList = JSON.parseArray(analysisData.getAnalysisResult(), RationCountDataInfo.class);
                    if (empty) {
                        for (RationCountDataInfo dataInfo : countDataInfoList) {
                            if (dataInfo.getIdentity().equals(SampleTableEnum.SUM_Abs.getIdentity())) {
                                BeanUtils.copyProperties(dataInfo, toCountData);
                                newData.add(dataInfo);
                            }
                            if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                                newData.add(dataInfo);
                            }
                        }
                    } else {
                        for (RationCountDataInfo dataInfo : countDataInfoList) {
                            if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                                BeanUtils.copyProperties(dataInfo, toCountData);
                                newData.add(dataInfo);
                            }
                        }
                    }
                    analysisDataId = analysisData.getId();
                    BeanUtils.copyProperties(analysisData, saveData);
                    break;
                }
            }
            if (!StringUtils.hasText(analysisDataId)) {
                log.info("该样品没有找到对应的测量数据");
                continue;
            }
            //拟合浓度计算
            String valued = rationService.valueCalculate(curveData, toCountData.getColValue(), parameter.getEquationType(), digit);
            RationCountDataInfo valueData = new RationCountDataInfo();
            valueData.setHeadName(SampleTableEnum.FIT_CONCENTRATION.getName());
            valueData.setIdentity(SampleTableEnum.FIT_CONCENTRATION.getIdentity());
            valueData.setColValue(valued==null? null : Double.valueOf(valued));
            newData.add(valueData);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sampleId", specimen.getSpecimenKey());
            jsonObject.put("key", valueData.getIdentity());
            jsonObject.put("value", valueData.getColValue());
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));

            //公式计算---待定
            //TODO 公式计算

            saveData.setAnalysisResult(JSON.toJSONString(newData));
            tMeasureAnalysisDataService.update(saveData, analysisDataQueryWrapper);
        }
    }

    private static void initParams() {
        //初始化测量参数
        isAvg = false;
        isSd = false;
        isRsd = false;
        digit = 4;
        empty = false;
        emptyList.clear();
        emptyDataList.clear();
        lastIsEmpty = false;
        minuend = 0.0;
    }

    public void sendPoint(WebSocketSession session, String param, Double absValue, RationParameter parameter) throws IOException {
        JSONObject point = new JSONObject();
        if (parameter.getCurveType().equals(RationCurveParamEnums.typeAbs.getIdentity())) {
            point.put("x", absValue);
            point.put("y", param);
        } else {
            point.put("x", param);
            point.put("y", absValue);
        }
        if (session.isOpen()) {
            JSONObject payload = new JSONObject();
            payload.put("point", point);
            payload.put("equationType", RationCurveParamEnums.getName(parameter.getEquationType()));
            session.sendMessage(new TextMessage(payload.toJSONString()));
        }
    }

    private void emptySampleDeal(Integer digit, RationCountDataInfo ansData, RationSaveCountDataInfo dealData){
        empty = true;

        RationCountDataInfo yAbs = new RationCountDataInfo();
        yAbs.setHeadName(SampleTableEnum.SUM_Abs.getName());
        yAbs.setIdentity(SampleTableEnum.SUM_Abs.getIdentity());
        yAbs.setColValue(0.0);

        List<RationCountDataInfo> currentList = new ArrayList<>();

        if (lastIsEmpty) {
            minuend = 0.0;
            emptyList.add(ansData);

            double sub = 0.0;
            //计算平均值
            for (RationCountDataInfo dataInfo : emptyList) {
                sub += dataInfo.getColValue();
            }
            minuend = DataCountUtil.formatValue(digit, sub / emptyList.size());
            //添加到空白列表
            currentList.add(ansData);
            yAbs.setColValue(ansData.getColValue() - minuend);
            currentList.add(yAbs);
            dealData.setCountData(currentList);
            emptyDataList.add(dealData);

            for (RationSaveCountDataInfo data : emptyDataList) {
                List<RationCountDataInfo> list = data.getCountData();
                List<RationCountDataInfo> newList = new ArrayList<>();

                for (RationCountDataInfo dataInfo : list) {
                    if (dataInfo.getIdentity().equals(SampleTableEnum.Abs.getIdentity())) {
                        newList.add(dataInfo);
                        yAbs.setColValue(dataInfo.getColValue() - minuend);
                        newList.add(yAbs);
                    }
                }
                data.setCountData(newList);
            }
            rationService.saveEmptySampleData(emptyDataList);
        } else {

            emptyList.clear();
            emptyDataList.clear();
            minuend = 0.0;
            emptyList.add(ansData);
            minuend = ansData.getColValue();
            yAbs.setColValue(ansData.getColValue() - minuend);
            currentList.add(yAbs);
            currentList.add(ansData);
            dealData.setCountData(currentList);

            emptyDataList.add(dealData);
        }
        lastIsEmpty = true;
    }

    public List<RationWavelengthDataInfo> countRepeatData(List<RationWavelengthDataInfo> originData, Integer digit) {

        List<RationWavelengthDataInfo> ans = new ArrayList<>();

        Map<String, List<Double>> absValue = new HashMap<>();
        Map<String, List<Double>> tPercentValue = new HashMap<>();
        Map<String, RationWavelengthDataInfo> dataMap = new HashMap<>();
        Set<String> wavelengthSet = new HashSet<>();

        for (RationWavelengthDataInfo data : originData) {
            if (data.getIdentity().equals(DataIdentityEnums.result.getIdentity())) {
                dataMap.putIfAbsent(data.getWavelength(), data);
                continue;
            }
            double abs = Double.parseDouble(data.getAbsValue());
            double tPercent = Double.parseDouble(data.getTPercentValue());

            absValue.computeIfAbsent(data.getWavelength(), k -> new ArrayList<>()).add(abs);
            tPercentValue.computeIfAbsent(data.getWavelength(), k -> new ArrayList<>()).add(tPercent);
            wavelengthSet.add(data.getWavelength());
        }

        for (String wavelength : wavelengthSet) {
            RationWavelengthDataInfo dataInfo = dataMap.get(wavelength);

            List<Double> allAbs = absValue.get(wavelength);
            double mean = DataCountUtil.calculateMean(allAbs, digit);

            List<Double> allPercent = tPercentValue.get(wavelength);
            double meanT = DataCountUtil.calculateMean(allPercent, digit);

            dataInfo.setAbsValue(String.valueOf(mean));
            dataInfo.setTPercentValue(String.valueOf(meanT));

            ans.add(dataInfo);
        }
        return ans;
    }



    //单个样品普通不微分测量
    public R<RationDataVO> singleSampleSimpleMeasure(TMeasureSample sample, RationParameter parameter, TMeasureTask task, String ip, String sessionId) throws Exception {

        WebSocketSession session = sessions.get(sessionId);

        List<RationWavelengthDataInfo> originDataList = new ArrayList<>();
        List<Command> list = new ArrayList<>();
        List<ChangeParam> machineData = new ArrayList<>();
        List<RationCountDataInfo> countData = new ArrayList<>();
        RationDataVO dataInfo = new RationDataVO();

        List<String> waveLengths = parameter.getWaveLength();

        list.add(new Command("SetPhotoMode", new Object[]{WaveLengthEnum.abs.getCode()}));
        list.add(new Command("SetResponseTime", new Object[]{parameter.getResponseTime()})); //修改设置响应时间
        LocalDateTime startTime = LocalDateTime.now();
        for (String waveLength : waveLengths) {
            list.add(new Command("SetWl", new Object[]{waveLength}));
            list.add(new Command("SetCellPos", new Object[]{sample.getSampleLocation()}));
            list.add(new Command("Measure", new Object[]{}));

            R<ChangeParam> res = devMessageUtil.photometricAndRationMeasurement(list, ip);
            if (!res.getSuccess()) {
                log.info("测量出错");
                return R.fail("测量出错");
            }
            ChangeParam response = res.getData();
            machineData.add(response);
            ChangeParam originRsd = new ChangeParam();
            if (StringUtils.hasText(task.getZeroParam())) {
                List<ChangeParam> params = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
                for (ChangeParam param : params) {
                    if (param.getWl().equals(waveLength)) {
                        BeanUtils.copyProperties(param, originRsd);
                        break;
                    }
                }
            }

            String tPercent = DataCountUtil.countTPercent(originRsd, response, 1.0);
            String data = DataCountUtil.changeTPercentOrAbs(null, tPercent);

            RationWavelengthDataInfo originDataInfo = new RationWavelengthDataInfo();
            originDataInfo.setIdentity(DataIdentityEnums.result.getIdentity());
            originDataInfo.setWavelength(waveLength);

            originDataInfo.setAbsValue(data);
            originDataInfo.setTPercentValue(tPercent);

            originDataList.add(originDataInfo);

            if (session.isOpen()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sampleId", sample.getId());
                jsonObject.put("key", waveLength + "-" + WaveLengthEnum.abs.getCode());
                jsonObject.put("value", DataCountUtil.formatValueWithBigDecimal(digit, originDataInfo.getAbsValue()));
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));

                jsonObject.put("key", waveLength + "-" + WaveLengthEnum.tPercent.getCode());
                jsonObject.put("value", DataCountUtil.formatValueWithBigDecimal(digit, originDataInfo.getTPercentValue()));
                session.sendMessage(new TextMessage(jsonObject.toJSONString()));
            }
            list.clear();
        }
        LocalDateTime endTime = LocalDateTime.now();
        dataInfo.setMachineData(machineData);
        dataInfo.setWavelengthData(originDataList);
        dataInfo.setTaskId(task.getId());
        dataInfo.setSampleId(sample.getId());
        dataInfo.setSampleTime(startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        dataInfo.setStartTime(startTime);
        dataInfo.setEndTime(endTime);

        R<RationCountDataInfo> absValue = rationService.countAbsValue(originDataList, parameter,digit);
        if (!absValue.getSuccess()) {
            log.info("计算出错");
            sendAndClose(session, absValue.getMessage());
            return R.fail("计算出错");
        }
        RationCountDataInfo data = absValue.getData();
        if (session.isOpen()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sampleId", sample.getId());
            jsonObject.put("key", data.getIdentity());
            jsonObject.put("value", data.getColValue());
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));

            jsonObject.put("key", "samplingTime");
            jsonObject.put("value", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
        }
        countData.add(data);
        dataInfo.setCountData(countData);

        return R.ok(dataInfo);
    }

    //微分测
    public R<Boolean> singleSampleDifferentialMeasure(){
        return R.ok();
    }

    public R<RationDataVO> singleSampleRepeatMeasure(TMeasureSample sample, RationParameter parameter, TMeasureTask task, String ip, String sessionId) throws IOException {
        int repeatCount = Integer.parseInt(parameter.getRepeatCount());

        WebSocketSession session = sessions.get(sessionId);

        List<RationWavelengthDataInfo> originDataList = new ArrayList<>();
        List<Command> list = new ArrayList<>();
        List<ChangeParam> machineData = new ArrayList<>();
        List<RationCountDataInfo> countData = new ArrayList<>();
        RationDataVO ans = new RationDataVO();

        List<String> waveLengths = parameter.getWaveLength();

        LocalDateTime startTime = LocalDateTime.now();
        list.add(new Command("SetPhotoMode", new Object[]{WaveLengthEnum.abs.getCode()}));
        list.add(new Command("SetResponseTime", new Object[]{parameter.getResponseTime()}));

        for (int i = 0; i < repeatCount; i++) {
            for (String waveLength : waveLengths) {
                list.add(new Command("SetWl", new Object[]{waveLength}));
                list.add(new Command("SetCellPos", new Object[]{sample.getSampleLocation()}));
                list.add(new Command("Measure", new Object[]{}));

                R<ChangeParam> res = null;
                try {
                    res = devMessageUtil.photometricAndRationMeasurement(list, ip);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (!res.getSuccess()) {
                    log.info("测量出错");
                    return R.fail("测量出错");
                }
                ChangeParam response = res.getData();
                response.setWl(waveLength + "-" + i);
                machineData.add(response);
                ChangeParam originRsd = new ChangeParam();
                List<ChangeParam> params = JSON.parseArray(task.getZeroParam(), ChangeParam.class);
                for (ChangeParam param : params) {
                    if (param.getWl().equals(waveLength)) {
                        BeanUtils.copyProperties(param, originRsd);
                        break;
                    }
                }
                String tPercent = DataCountUtil.countTPercent(originRsd, response, 1.0);
                String data = DataCountUtil.changeTPercentOrAbs(null, tPercent);

                RationWavelengthDataInfo originDataInfo = new RationWavelengthDataInfo();
                originDataInfo.setIdentity(DataIdentityEnums.middle.getIdentity() + "-" + i);
                originDataInfo.setWavelength(waveLength);

                originDataInfo.setAbsValue(data);
                originDataInfo.setTPercentValue(tPercent);
                originDataInfo.setMeasureTime(LocalDateTime.now());

                originDataList.add(originDataInfo);

                if (session.isOpen()) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("sampleId", sample.getId());
                    jsonObject.put("key", waveLength + "-" + WaveLengthEnum.abs.getCode());
                    jsonObject.put("identity", waveLength + "-" + WaveLengthEnum.abs.getCode() + "-" + i);
                    jsonObject.put("value", originDataInfo.getAbsValue());
                    session.sendMessage(new TextMessage(jsonObject.toJSONString()));

                    jsonObject.put("key", waveLength + "-" + WaveLengthEnum.tPercent.getCode());
                    jsonObject.put("identity", waveLength + "-" + WaveLengthEnum.tPercent.getCode() + "-" + i);
                    jsonObject.put("value", originDataInfo.getTPercentValue());
                    session.sendMessage(new TextMessage(jsonObject.toJSONString()));
                }

                list.clear();
            }
        }
        //重复测完，开始计算平均值

        Map<String, List<Double>> absValues = originDataList.stream()
                .collect(Collectors.groupingBy(
                        RationWavelengthDataInfo::getWavelength,
                        Collectors.mapping(info -> Double.valueOf(info.getAbsValue()), Collectors.toList())
                ));

        Map<String, List<Double>> tPercentValues = originDataList.stream()
                .collect(Collectors.groupingBy(
                        RationWavelengthDataInfo::getWavelength,
                        Collectors.mapping(info -> Double.valueOf(info.getTPercentValue()), Collectors.toList())
                ));
        for (String waveLength : waveLengths) {
            Map<String, RepeatMeasureData> otherDataList = new HashMap<>();

            List<Double> absList = absValues.get(waveLength);
            double absMean = DataCountUtil.calculateMean(absList, digit);
            Double absSd = DataCountUtil.calculateSD(absList, digit);
            Double absRsd = DataCountUtil.calculateRSD(absList, digit);

            List<Double> tPercentList = tPercentValues.get(waveLength);
            double tPercentMean = DataCountUtil.calculateMean(tPercentList, digit);
            Double tPercentSd = DataCountUtil.calculateSD(tPercentList, digit);
            Double tPercentRsd = DataCountUtil.calculateRSD(tPercentList, digit);

            RepeatMeasureData absData = new RepeatMeasureData();
            absData.setSd(absSd == null ? "未检出" :String.valueOf(absSd));
            absData.setRsd(absRsd == null ? "未检出" :String.valueOf(absRsd));
            otherDataList.put(waveLength + "-" + WaveLengthEnum.abs.getCode(),absData);

            RepeatMeasureData tPercentData = new RepeatMeasureData();
            tPercentData.setSd(tPercentSd == null ? "未检出" :String.valueOf(tPercentSd));
            tPercentData.setRsd(tPercentRsd == null ? "未检出" :String.valueOf(tPercentRsd));
            otherDataList.put(waveLength + "-" + WaveLengthEnum.tPercent.getCode() ,tPercentData);

            RationWavelengthDataInfo info = new RationWavelengthDataInfo();
            info.setWavelength(waveLength);
            info.setAbsValue(String.valueOf(absMean));
            info.setIdentity("result");
            info.setTPercentValue(String.valueOf(tPercentMean));
            info.setOtherData(otherDataList);
            info.setMeasureTime(LocalDateTime.now());

            if (session.isOpen()) {
                repeatSend(session, absList, sample, repeatCount, waveLength + "-" + WaveLengthEnum.abs.getCode());
                repeatSend(session, tPercentList, sample, repeatCount, waveLength + "-" + WaveLengthEnum.tPercent.getCode());
            }

            originDataList.add(info);
        }
        LocalDateTime endTime = LocalDateTime.now();

        R<RationCountDataInfo> absValue = rationService.countAbsValue(originDataList, parameter, digit);
        if (!absValue.getSuccess()) {
            return R.fail(absValue.getMessage());
        }
        RationCountDataInfo data = absValue.getData();
        countData.add(data);
        if (session.isOpen()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sampleId", sample.getId());
            jsonObject.put("key", data.getIdentity());
            jsonObject.put("value", data.getColValue());
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));

            jsonObject.put("key", "samplingTime");
            jsonObject.put("value", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
        }
        ans.setTaskId(task.getId());
        ans.setSampleId(sample.getId());
        ans.setStartTime(startTime);
        ans.setEndTime(endTime);
        ans.setCountData(countData);
        ans.setMachineData(machineData);
        ans.setSampleTime(startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        ans.setWavelengthData(originDataList);

        return R.ok(ans);

    }

    public void sampleSend(WebSocketSession session, String sampleId, String key, String identity, String value) throws IOException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sampleId", sampleId);
        jsonObject.put("key", key);
        if (StringUtils.hasText(identity)) {
            jsonObject.put("identity", identity);
        }
        jsonObject.put("value", value);
        session.sendMessage(new TextMessage(jsonObject.toJSONString()));
    }

    public void repeatSend(WebSocketSession session, List<Double> data, TMeasureSample sample, Integer repetitionTime, String key) throws IOException {

        Double v = DataCountUtil.calculateMean(data, digit);
        Double sd = DataCountUtil.calculateSD(data, digit);
        Double rsd = DataCountUtil.calculateRSD(data, digit);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sampleId", sample.getId());
        repetitionTime = repetitionTime + 1;

        if (isAvg) {
            jsonObject.put("key", SampleTableEnum.SAMPLE_NUMBER.getIdentity());
            jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + repetitionTime);
            jsonObject.put("value", sample.getSampleNumber() + "-" + DataIdentityEnums.avg.getName());
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));

            jsonObject.put("key", key);
            jsonObject.put("identity", DataIdentityEnums.avg.getIdentity() + "-" + repetitionTime);
            jsonObject.put("value", String.valueOf(v));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
        }

        if (isSd) {
            jsonObject.put("key", SampleTableEnum.SAMPLE_NUMBER.getIdentity());
            jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + (repetitionTime + 1));
            jsonObject.put("value", sample.getSampleNumber() + "-" + DataIdentityEnums.sd.getName());
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));

            jsonObject.put("key", key);
            jsonObject.put("identity", DataIdentityEnums.sd.getIdentity() + "-" + (repetitionTime + 1));
            jsonObject.put("value", sd == null ? "未检出" :String.valueOf(sd));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
        }

        if (isRsd) {
            jsonObject.put("key", SampleTableEnum.SAMPLE_NUMBER.getIdentity());
            jsonObject.put("identity", DataIdentityEnums.middle.getIdentity() + "-" + (repetitionTime + 2));
            jsonObject.put("value", sample.getSampleNumber() + "-" + DataIdentityEnums.rsd.getName());
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));

            jsonObject.put("key", key);
            jsonObject.put("identity", DataIdentityEnums.rsd.getIdentity() + "-" + (repetitionTime + 2));
            jsonObject.put("value", rsd == null ? "未检出" :String.valueOf(rsd));
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
        }

        jsonObject.put("key", key);
        jsonObject.remove("identity");
        jsonObject.put("value", String.valueOf(v));

    }

    /**
     * 收到前端消息时触发
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
     * @param closeStatus
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        sessions.remove(session.getId());
        status = 0;
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
        status = 0;
        System.out.println("连接异常: " + exception.getMessage());
    }

    /**
     * 获取查询参数
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
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("result", "finish");
            session.sendMessage(new TextMessage(jsonObject.toJSONString()));
            status = 0;
            session.close();
        } catch (IOException ignored) {}
    }
}
