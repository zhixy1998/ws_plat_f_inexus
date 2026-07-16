/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.StepPoint;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.websocket.MeasureController;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.persee.workstation.uv_inexus.websocket.MeasureController.addLampPosData;

/**
 * 报文解析类，负责解析收取的报文是否符合发送指令所需的结果，并解析成相应结构返回
 */


@Slf4j
public class UdpResponseDeal {

    public static String result = "false";

    public static String Sn = null;

    private static final String[] SAMPLE_TYPE_NAMES = {
            "1", "T700镜面反射附件", "1", "T700标准及微量样品池",
            "3", "3", "T700可变角度样品架", "T700超微量样品池",
            "2", "T700试管架", "1", "5", "未识别",
            "T600标准及微量样品池", "1", "1", "3",
            "1", "T600试管架", "T600可变角度样品架", "T600超微量样品池",
            "3", "5", "T600镜面反射附件", "2"
    };

    private static OperationResultEntity resultEntity;

    private static ChangeParam lastSuccessfulParam = null;

    /**
     * 根据命令ID分发解析逻辑
     */
    public static OperationResultEntity parse(String cmdId, List<byte[]> responses, byte[] sendBytes) {
        resultEntity = new OperationResultEntity();
        resultEntity.setIsSuccess(true);
        switch (cmdId) {
            case "GetSn":
                return parseSn(responses);
            case "GotoMLamp":
                return goToMlLamp(responses);

            case "SetWLampStatus":
            case "SetDLampStatus":
            case "SetLampPos":
            case "SetChangeLampWl":
            case "SetWlList":
            case "SetScanWlInterval":
            case "SetScanSpeed":
            case "SetWl":
            case "SetStartWl":
            case "SetEndWl":
            case "SetFilter":
            case "SetGain":
            case "SetPadModeTrue":
            case "SetPadModeFalse":
            case "SetPCModeTrue":
            case "SetPCModeFalse":
            case "SetProgrammeModeTrue":
            case "SetProgrammeModeFalse":
            case "SetInitStatus":
            case "SetCurrentNegativeHV":
                return parseCompareBytes(responses, sendBytes);
            case "SetSlit1":
                return parseSlit1(responses, sendBytes);
//            case "GetInitStatus":
//                return getInitStatus(responses);
            case "SetPhotoMode":
                return parseMode(responses, sendBytes);
            case "InitSampleCell":
            case "InitFilterMotor":
            case "InitWlMotor":
            case "InitLampMotor":
            case "CheckWLamp":
            case "CheckDLamp":
            case "CheckWl":
            case "Dark":
            case "DisConnect":
            case "DisConnectIAP":
            case "InitSlit1Motor":
            case "CorrectWl":
                return parseBooleanFrom5thByte(responses);
            case "SetCellPos":
                return parseSetCellPos(responses, sendBytes);
            case "GetAccessoryType":
                return parseType(responses);
            case "Measure":
            case "T800TimeMeasure":
                return measure(responses);
            case "LiveMeasure":
                return liveMeasure(responses);
            case "Zero":
                return zero(responses);
//                return parseZero(responses);
            case "ScanFast":
            case "Scan":
            case "BaseLine":
            case "BaseLineFast":
                return parseScan(responses);
            case "GetWlList":
                return parseWlList(responses);
            case "T800GetWlList" :
                return parseT800WlList(responses);


            case "SetResponseTime":
                return parseResponseTime(responses, sendBytes);

            case "GetChangeLampWl":
                return parseChangeLampWl(responses);

//            case "SetCellPos":
//                return parseCellPos(responses, sendBytes);

//            case "SetSlit1":
//                return parseSlit1(responses, sendBytes);

            case "GetCellPos":
                return parseCellPos(responses, sendBytes);
            case "GetWLampStatus":
            case "GetDLampStatus":
                return getLampStatus(responses);

            case "GetInitStatus":
                return getInitStatus(responses);
            case "GetLampPos":
                return parseLampPos(responses);

            case "SearchAround":
//                return deviceIp(responses);
            case "SearchInSameZoom":
                return pareSearch(responses);
            case "InitSlit1Moto1":
                return parseInitSlit1Motor(responses, sendBytes);

            case "GetPhotoMode":
                return parsePhotoMode(responses);
            case "GetResponseTime":
                return parseGetResponseTime(responses);
            case "Stop":
                return parseStop(responses);
            case"GetDeviceTotalTime":
            case"GetWLampTotalTime":
            case"GetDLampTotalTime":
            case"GetDeviceCurrentTime":
            case"GetWLampCurrentTime":
            case"GetDLampCurrentTime":
                return pareTime(responses);
            case"GetCurrentNegativeHV":
                return parseCurrentNegativeHV(responses);

//            case "NewIP":
//                return parseNewIP(responses);
            default:
                return resultEntity;
        }
    }

    private static OperationResultEntity parseCurrentNegativeHV(List<byte[]> responses) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }
        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 5) {
            System.out.println("回应报文长度不足5字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }
        int NegativeHV = ((lastResponse[4] & 0xFF) << 8) | ((lastResponse[5] & 0xFF));
        resultEntity.setCurrentNegativeHighVoltage((float) NegativeHV / 10);
        return resultEntity;
    }

    private static OperationResultEntity pareTime(List<byte[]> responses) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }
        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 5) {
            System.out.println("回应报文长度不足5字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }
        int time = ((lastResponse[4] & 0xFF) << 8) | (lastResponse[5] & 0xFF);
        System.out.println("设备运行时间: " + time);
        resultEntity.setUseTime(time);
        return resultEntity;
    }

    private static OperationResultEntity parseStop(List<byte[]> responses) {
        if (responses == null || responses.isEmpty()) {
            result = "false"; // 设置全局变量
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A015");
            return resultEntity;
        }

        byte[] last = responses.get(responses.size() - 1);
        boolean success = last.length >= 5 && (last[2] & 0xFF) == 0 && (last[3] & 0xFF) == 0;
        resultEntity.setIsSuccess(success);
        return resultEntity;
    }

    private static OperationResultEntity parseGetResponseTime(List<byte[]> responses) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }
        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 5) {
            System.out.println("回应报文长度不足5字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }
        float time = (lastResponse[4] & 0xFF) / 100.0f;
        String rawtime = String.valueOf(time);
        MeasureController.addTimeData(rawtime);

        long l = System.currentTimeMillis();
        if (DetectionConstants.detectionInfo != null && rawtime != null) {
            DetectionConstants.detectionInfo.put("time", rawtime);
        }

        long l1 = System.currentTimeMillis() - l;
        System.out.println("耗时：" + l1);
        resultEntity.setTime(rawtime);
        return resultEntity;
    }


    private static OperationResultEntity parsePhotoMode(List<byte[]> responses) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        for (int i = 0; i < responses.size(); i++) {
            byte[] data = responses.get(i);

            if (data[3] == 0x05)  {
                int mode = data[4] & 0xFF;

                DetectionConstants.luminosity = String.valueOf(mode);

                if (mode == 0){
                    String photomode = "Abs";
                    MeasureController.addPhotoModeData(photomode);
                } else if (mode == 1) {
                    String photomode = "T%";
                    MeasureController.addPhotoModeData(photomode);
                }else if (mode == 2) {
                    String photomode = "Es";
                    MeasureController.addPhotoModeData(photomode);
                }else if (mode == 3) {
                    String photomode = "Er";
                    MeasureController.addPhotoModeData(photomode);
                }
                resultEntity.setLuminosity(String.valueOf(mode));
            }
        }

        return resultEntity;
    }


    private static OperationResultEntity parseInitSlit1Motor(List<byte[]> responses, byte[] sendBytes) {
        if (responses == null || responses.isEmpty()) {
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        byte[] last = responses.get(responses.size() - 1);
        boolean success = false;
        if ((last[3] & 0xFF) == 0x04) {
            success = last.length >= 5 && (last[4] & 0xFF) == 0;
        }
        resultEntity.setIsSuccess(success);
        return resultEntity;
    }

    private static OperationResultEntity parseChangeLampWl(List<byte[]> responses) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 6) { // 至少要有6字节
            System.out.println("回应报文长度不足6字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        // 判断第3个字节（索引2）是否为 0x05
        if ((lastResponse[3] & 0xFF) != 0x05) {
            System.out.printf("最后一条报文第3位不是0x05（实际值: %02X），跳过解析\n", lastResponse[2]);
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        // 如果通过检查，则进行解析
        int wl = (((lastResponse[4] & 0xFF) << 8) | (lastResponse[5] & 0xFF));
        double ChangeLampWl = wl;
        String rawchagelampwl = String.valueOf(ChangeLampWl);

        System.out.printf("波长原始值=%d, 解析结果=%.2f\n", wl, ChangeLampWl);

        MeasureController.addWlData(rawchagelampwl);
        resultEntity.setChangeLampWl(rawchagelampwl);
        return resultEntity;
    }


    private static OperationResultEntity parseResponseTime(List<byte[]> responses, byte[] sendBytes) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        if (sendBytes == null || sendBytes.length < 5) {
            System.out.println("发送报文长度不足5字节，实际长度: " + sendBytes.length);
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 5) {
            System.out.println("回应报文长度不足5字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        boolean isEqual = lastResponse[2] == sendBytes[2] &&
                lastResponse[3] == sendBytes[3] &&
                lastResponse[4] == sendBytes[4];

        result = isEqual ? "true" : "false";

        float time = (lastResponse[4] & 0xFF) / 100.0f;
        String rawtime = String.valueOf(time);
        MeasureController.addTimeData(rawtime);
        resultEntity.setTime(rawtime);
        resultEntity.setIsSuccess(isEqual);
        return resultEntity;
    }

    private static OperationResultEntity parseSlit1(List<byte[]> responses, byte[] sendBytes) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A016");
            return resultEntity;
        }

        if (sendBytes == null || sendBytes.length < 5) {
            System.out.println("发送报文长度不足5字节，实际长度: " + sendBytes.length);
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A017");
            resultEntity.setErrorArgs(new Object[]{sendBytes.length});
            return resultEntity;
        }

        byte[] lastResponse = null;
        for (int i = responses.size() - 1; i >= 0; i--) {
            byte[] resp = responses.get(i);
            if (resp != null && resp.length >= 4 &&
                    (resp[0] & 0xFF) == 0xFF &&
                    (resp[1] & 0xFF) == 0x65 &&
                    (resp[2] & 0xFF) == 0x20 &&
                    (resp[3] & 0xFF) == 0xF1) {
                continue;
            }
            lastResponse = resp;
            break;
        }

        if (lastResponse == null || lastResponse.length < 5) {
            System.out.println("未找到有效回应报文或报文长度不足5字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A018");
            resultEntity.setErrorArgs(new Object[]{sendBytes.length});
            return resultEntity;
        }

        boolean isEqual = lastResponse[2] == sendBytes[2] &&
                lastResponse[3] == sendBytes[3] &&
                lastResponse[4] == sendBytes[4];

        resultEntity.setIsSuccess(isEqual);

        float Bandwidth = (lastResponse[4] & 0xFF) / 10.f;
        String SpectralBandwidth = String.valueOf(Bandwidth);
        MeasureController.addBandwidthData(SpectralBandwidth);

        if (DetectionConstants.detectionInfo != null) {
            DetectionConstants.detectionInfo.put("bandwidth", SpectralBandwidth);
        }
        resultEntity.setBandwidth(SpectralBandwidth);

        return resultEntity;
    }


    //    private static Object parseLampPos(List<byte[]> responses) {
//        byte[] lastResponse = responses.get(responses.size() - 1);
//        if (lastResponse == null || lastResponse.length < 5) {
//            System.out.println("回应报文长度不足5字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
//            result = "false";
//            return false;
//        }
//
//        int pos = (lastResponse[4] & 0xFF); // pos == 0 表示钨灯亮，pos == 1 表示氘灯亮
//
//        // 构造 dataList
//        List<Map<String, String>> dataList = new ArrayList<>();
//
//        Map<String, String> tungsten = new HashMap<>();
//        tungsten.put("label", "钨灯");
//        tungsten.put("value", pos == 0 ? "亮" : "不亮");
//
//        Map<String, String> deuterium = new HashMap<>();
//        deuterium.put("label", "氘灯");
//        deuterium.put("value", pos == 1 ? "亮" : "不亮");
//
//        dataList.add(tungsten);
//        dataList.add(deuterium);
//
//        Map<String, Object> finalJson = new HashMap<>();
//        finalJson.put("dataList", dataList);
//
//        try {
//            ObjectMapper mapper = new ObjectMapper();
//            String json = mapper.writeValueAsString(finalJson);
//
//            MeasureController.addLampPosData(json);
//            System.out.println("解析灯状态并推送: " + json);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return pos;
//    }
    private static OperationResultEntity parseLampPos(List<byte[]> responses) {
        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 5) {
            System.out.println("回应报文长度不足5字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        int pos = (lastResponse[4] & 0xFF); // pos == 0 钨灯亮, pos ==1 氘灯亮

        List<Map<String, String>> dataList = new ArrayList<>();

        Map<String, String> tungsten = new HashMap<>();
        tungsten.put("label", "钨灯");
        tungsten.put("value", pos == 0 ? "亮" : "不亮");


        Map<String, String> deuterium = new HashMap<>();
        deuterium.put("label", "氘灯");
        deuterium.put("value", pos == 1 ? "亮" : "不亮");

        dataList.add(tungsten);
        dataList.add(deuterium);

        Map<String, Object> lampData = new HashMap<>();
        lampData.put("dataList", dataList);

        // 入队 Map（现在队列类型是 Map）
        addLampPosData(lampData);

        System.out.println("解析灯状态完成，入队等待推送: " + lampData);
        resultEntity.setLampPos(String.valueOf(pos));
        return resultEntity;
    }

    private static OperationResultEntity parseT800WlList(List<byte[]> responses) {


        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A019");
            return resultEntity;
        }

        ArrayList<StepPoint> resultList = new ArrayList<>();

        try {
            for (byte[] resp : responses) {
                if (resp == null || resp.length < 11) {
                    System.out.println("回应报文长度不足11字节，跳过解析");
                    continue;
                }

                int cmdType1 = resp[2] & 0xFF; // 第3个字节（索引2）
                int cmdType = resp[3] & 0xFF;   // 第4个字节（索引3）

                // 满足条件：第3字节为19 或 第4字节为10 时解析
                if (cmdType1 == 19 || cmdType == 10) {
                    int n = resp[5] & 0xFF;

                    int actualStep = ((resp[7] & 0xFF) << 8) | (resp[8] & 0xFF);
                    int theoryStep = ((resp[9] & 0xFF) << 8) | (resp[10] & 0xFF);

                    resultList.add(new StepPoint(actualStep, theoryStep, n));
                }
            }

            if (resultList.isEmpty()) {
                System.out.println("未解析到有效 StepPoint 数据");
                resultEntity.setIsSuccess(false);
                resultEntity.setErrorCode("0020A01A");
            } else {
                System.out.println("WlList 解析结果：" + resultList);
                resultEntity.setIsSuccess(true);
                resultEntity.setErrorMsg(null);
                resultEntity.setStepPoints(resultList);  // 使用新增字段 stepPoints
            }

            return resultEntity;

        } catch (Exception e) {
            System.out.println("WlList 解析异常: " + e.getMessage());
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A01B");
            resultEntity.setErrorArgs(new Object[]{e.getMessage()});
            return resultEntity;
        }
    }

    private static OperationResultEntity parseWlList(List<byte[]> responses) {


        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A019");
            return resultEntity;
        }

        ArrayList<StepPoint> resultList = new ArrayList<>();

        try {
            for (byte[] resp : responses) {
                if (resp == null || resp.length < 11) {
                    System.out.println("回应报文长度不足11字节，跳过解析");
                    continue;
                }

                int cmdType1 = resp[2] & 0xFF;
                int cmdType = resp[3] & 0xFF;

                if (cmdType1 == 19 || cmdType == 10) {
                    int n = resp[5] & 0xFF;

                    int theoryStep = ((resp[8] & 0xFF) << 8) | (resp[7] & 0xFF);
                    int actualStep = ((resp[10] & 0xFF) << 8) | (resp[9] & 0xFF);

                    resultList.add(new StepPoint(actualStep, theoryStep, n));
                }
            }

            if (resultList.isEmpty()) {
                System.out.println("未解析到有效 StepPoint 数据");
                resultEntity.setIsSuccess(false);
                resultEntity.setErrorCode("0020A01A");
            } else {
                System.out.println("WlList 解析结果：" + resultList);
                resultEntity.setIsSuccess(true);
                resultEntity.setErrorMsg(null);
                resultEntity.setStepPoints(resultList);
            }

            return resultEntity;

        } catch (Exception e) {
            System.out.println("WlList 解析异常: " + e.getMessage());
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A01B");
            resultEntity.setErrorArgs(new Object[]{e.getMessage()});
            return resultEntity;
        }
    }



    /**
     * 通用：最后一条回应的第5位是否为0
     */
    private static OperationResultEntity parseBooleanFrom5thByte(List<byte[]> responses) {
        if (responses == null || responses.isEmpty()) {
            result = "false"; // 设置全局变量
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A015");
            return resultEntity;
        }

        byte[] last = responses.get(responses.size() - 1);
        boolean success = last.length >= 5 && (last[4] & 0xFF) == 0 && (last[3] & 0xFF) != 7;
        resultEntity.setIsSuccess(success);
        return resultEntity;
    }

    private static OperationResultEntity getInitStatus(List<byte[]> responses) {
        if (responses == null || responses.isEmpty()) {

            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        byte[] last = responses.get(responses.size() - 1);
        boolean success = last.length >= 5 && (last[4] & 0xFF) == 1;

        resultEntity.setIsInited(success ? 1 : 0);
        return resultEntity;
    }

    private static OperationResultEntity getLampStatus(List<byte[]> responses) {
        if (responses == null || responses.isEmpty()) {
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        byte[] last = responses.get(responses.size() - 1);

        resultEntity.setLampStatus(last.length >= 5 && (last[4] & 0xFF) == 1);

        return resultEntity;
    }

    private static OperationResultEntity parseSetCellPos(List<byte[]> responses, byte[] sendBytes) {
        if (responses == null || responses.isEmpty()) {
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        byte[] last = responses.get(responses.size() - 1);
        boolean success = last.length >= 5 && (last[4] & 0xFF) == (sendBytes[4] & 0xFF);
        resultEntity.setIsSuccess(success);  // 根据结果更新 result
        return resultEntity;
    }

    /**
     * GetSn：拼接第2~4条回应中的序列号部分
     */


    private static OperationResultEntity parseSn(List<byte[]> responses) {
        if (responses.size() < 4) {
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        StringBuilder sn = new StringBuilder();
        for (int i = 1; i <= 3; i++) {
            byte[] part = responses.get(i);
            if (part.length >= 12) {
                sn.append(new String(part, 4, 7)); // 提取序列号部分
            } else {
                System.out.println("第 " + (i + 1) + " 条回应太短，跳过");
            }
        }

        sn.insert(2, "-");

        // 处理空字符和trim，然后截取前16位
        String rawSn = sn.toString().replace("\u0000", "").trim();

        // 确保只取前16位，若长度不足则取全部
        Sn = rawSn.length() >= 16 ? rawSn.substring(0, 16) : rawSn;

        resultEntity.setDeviceSerialNumber(Sn);

        System.out.println("设备序列号：" + Sn);
        return resultEntity; // 返回处理后的结果
    }

    private static OperationResultEntity goToMlLamp(List<byte[]> responses) {
        if (responses.size() < 4) {
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }
        return resultEntity; // 返回处理后的结果
    }

    /**
     * 附件类型解析
     */
    private static OperationResultEntity parseType(List<byte[]> responses) {
        if (responses ==  null || responses.isEmpty()) {
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        byte[] last = responses.get(responses.size() - 1);

        int type = (last[4] & 0xFF);
        String sampleType = (type >= 0 && type < SAMPLE_TYPE_NAMES.length)
                ? SAMPLE_TYPE_NAMES[type]
                : "未知类型(" + type + ")";

        // 添加到队列，供推送线程发送
        MeasureController.addSampleTypeData(sampleType);
        // 转成无符号 int

        int ans = last[4] & 0xFF;
        resultEntity.setAttachmentType(ans);
        return resultEntity;
    }

    /**
     S、D、R解析
     */
    public static OperationResultEntity parseZero(List<byte[]> responses) {
        ChangeParam param = new ChangeParam();

        boolean hasWlS = false;
        boolean hasDR  = false;

        if (responses == null || responses.isEmpty()) {
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A01C");
            return resultEntity;
        }

        try {
            for (byte[] resp : responses) {

                if (resp == null || resp.length < 11) {
                    continue;
                }

                int cmdType1 = resp[2] & 0xFF; // 大类
                int cmdType  = resp[3] & 0xFF; // 小类

                // 只处理 0x13
                if (cmdType1 != 0x13) {
                    continue;
                }

                // ---------- wl + s ----------
                if (cmdType == 0x03 || cmdType == 0x05 || cmdType == 0x07) {

                    int wl0 = ((resp[5] & 0xFF) << 8)
                            | (resp[6] & 0xFF);

                    int s0  = ((resp[7] & 0xFF) << 24)
                            | ((resp[8] & 0xFF) << 16)
                            | ((resp[9] & 0xFF) << 8)
                            | (resp[10] & 0xFF);

                    param.setWl(String.valueOf(wl0));
                    param.setS(String.valueOf(s0));
                    hasWlS = true;
                }

                // ---------- d + r ----------
                else if (cmdType == 0x04 || cmdType == 0x06 || cmdType == 0x08) {

                    int d0 = ((resp[4] & 0xFF) << 16)
                            | ((resp[5] & 0xFF) << 8)
                            | (resp[6] & 0xFF);

                    int r0 = ((resp[7] & 0xFF) << 24)
                            | ((resp[8] & 0xFF) << 16)
                            | ((resp[9] & 0xFF) << 8)
                            | (resp[10] & 0xFF);

                    param.setD(String.valueOf(d0));
                    param.setR(String.valueOf(r0));
                    hasDR = true;
                }

                // ---------- Zero 只要一组 ----------
                if (hasWlS && hasDR) {
                    break;
                }
            }

            if (!hasWlS && !hasDR) {
                resultEntity.setIsSuccess(false);
                resultEntity.setErrorCode("0020A01D");
                return resultEntity;
            }

            resultEntity.setCurrentMeasure(param);
            resultEntity.setIsSuccess(true);
            return resultEntity;

        } catch (Exception e) {
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A01B");
            resultEntity.setErrorArgs(new Object[]{e.getMessage()});
            return resultEntity;
        }
    }


    public static OperationResultEntity parseScan(List<byte[]> responses) {
        ChangeParam param = new ChangeParam();

        if (responses == null || responses.isEmpty()) {
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A01E");
            return resultEntity;
        }

        try {
            for (byte[] resp : responses) {

                if (resp == null || resp.length < 11) {
                    continue;
                }

                int cmdType1 = resp[2] & 0xFF;
                int cmdType  = resp[3] & 0xFF;

                // 只处理 0x13 类型
                if (cmdType1 != 0x13) {
                    continue;
                }

                if (cmdType == 0x01 || cmdType == 0x05
                        || cmdType == 0x09 || cmdType == 0x0B) {

                    int wl0 = ((resp[5] & 0xFF) << 8)
                            | (resp[6] & 0xFF);

                    int s0 = ((resp[8] & 0xFF) << 16)
                            | ((resp[9] & 0xFF) << 8)
                            | (resp[10] & 0xFF);

                    param.setWl(String.valueOf(wl0));
                    param.setS(String.valueOf(s0));
                }

                else if (cmdType == 0x02 || cmdType == 0x06 || cmdType == 0x30 || cmdType == 0x0C) {

                    int d0 = ((resp[4] & 0xFF) << 16)
                            | ((resp[5] & 0xFF) << 8)
                            | (resp[6] & 0xFF);

                    int r0 = ((resp[8] & 0xFF) << 16)
                            | ((resp[9] & 0xFF) << 8)
                            | (resp[10] & 0xFF);

                    param.setD(String.valueOf(d0));
                    param.setR(String.valueOf(r0));
                }
            }

            if (param.isEmpty()) {
                resultEntity.setIsSuccess(false);
                resultEntity.setErrorCode("0020A01F");
                return resultEntity;
            }

            resultEntity.setCurrentMeasure(param);
            resultEntity.setIsSuccess(true);
            return resultEntity;

        } catch (Exception e) {
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A020");
            resultEntity.setErrorArgs(new Object[]{e.getMessage()});
            return resultEntity;
        }
    }

//    public static OperationResultEntity parseScan(
//            List<byte[]> responses,
//            ChangeParam param   // 👈 新增：外部传入的“会话对象”
//    ) {
//
//        if (responses == null || responses.isEmpty()) {
//            return null;
//        }
//
//        try {
//            for (byte[] resp : responses) {
//
//                if (resp == null || resp.length < 11) {
//                    continue;
//                }
//
//                int cmdType1 = resp[2] & 0xFF;
//                int cmdType  = resp[3] & 0xFF;
//
//                if (cmdType1 != 0x13) {
//                    continue;
//                }
//
//                // wl + s
//                if (cmdType == 0x01 || cmdType == 0x05
//                        || cmdType == 0x09 || cmdType == 0x0B) {
//
//                    int wl0 = ((resp[5] & 0xFF) << 8)
//                            | (resp[6] & 0xFF);
//
//                    int s0  = ((resp[7] & 0xFF) << 24)
//                            | ((resp[8] & 0xFF) << 16)
//                            | ((resp[9] & 0xFF) << 8)
//                            | (resp[10] & 0xFF);
//
//                    param.setWl(String.valueOf(wl0));
//                    param.setS(String.valueOf(s0));
//                }
//                // r + d
//                else if (cmdType == 0x02 || cmdType == 0x06 || cmdType == 0x30) {
//
//                    int d0 = ((resp[4] & 0xFF) << 16)
//                            | ((resp[5] & 0xFF) << 8)
//                            | (resp[6] & 0xFF);
//
//                    int r0 = ((resp[7] & 0xFF) << 24)
//                            | ((resp[8] & 0xFF) << 16)
//                            | ((resp[9] & 0xFF) << 8)
//                            | (resp[10] & 0xFF);
//
//                    param.setD(String.valueOf(d0));
//                    param.setR(String.valueOf(r0));
//                }
//            }
//
//            // 👇 只有“完整一次测量”才返回
//            if (param.getWl() != null
//                    && param.getS() != null
//                    && param.getR() != null
//                    && param.getD() != null) {
//
//                OperationResultEntity result = new OperationResultEntity();
//                result.setCurrentMeasure(R.ok(param));
//                result.setIsSuccess(true);
//                return result;
//            }
//
//            return null;
//
//        } catch (Exception e) {
//            return null;
//        }
//    }


    /**
     * S、D、R解析
     */
    public static OperationResultEntity measure(List<byte[]> responses) {

        if (responses == null || responses.isEmpty()) {
            System.out.println("回应为空，无法解析 measure");
            resultEntity.setErrorCode("0020A021");
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        ChangeParam param = new ChangeParam();
        boolean hasWlS = false;
        boolean hasDR = false;

        try {
            for (byte[] resp : responses) {

                if (resp == null || resp.length < 11) {
                    continue;
                }

                int cmdType1 = resp[2] & 0xFF; // 大类
                int cmdType = resp[3] & 0xFF; // 小类

                // 只处理 0x13
                if (cmdType1 != 0x13) {
                    continue;
                }

                // ---------- wl + s ----------
                if (cmdType == 0x03 || cmdType == 0x05 || cmdType == 0x07) {

                    int step = ((resp[5] & 0xFF) << 8)
                            | (resp[6] & 0xFF);

                    int s0 = ((resp[8] & 0xFF) << 16)
                            | ((resp[9] & 0xFF) << 8)
                            | (resp[10] & 0xFF);

                    param.setStep(String.valueOf(step));
                    param.setS(String.valueOf(s0));
                    hasWlS = true;
                }

                // ---------- d + r ----------
                else if (cmdType == 0x04 || cmdType == 0x06 || cmdType == 0x08) {

                    int d0 = ((resp[4] & 0xFF) << 16)
                            | ((resp[5] & 0xFF) << 8)
                            | (resp[6] & 0xFF);

                    int r0 =  ((resp[8] & 0xFF) << 16)
                            | ((resp[9] & 0xFF) << 8)
                            | (resp[10] & 0xFF);

                    param.setD(String.valueOf(d0));
                    param.setR(String.valueOf(r0));
                    hasDR = true;
                }

                // ---------- 一组数据齐了 ----------
                if (hasWlS && hasDR) {
                    lastSuccessfulParam = param;
                    break; // Zero 只需要一组
                }
            }



            // 3. 修改判断逻辑，使用上一次的数据进行容错
            if (hasWlS || hasDR) {
                // 本次有部分数据，尝试用上一次的数据补全缺失部分
                if (lastSuccessfulParam != null) {
                    if (!hasWlS) {
                        param.setStep(lastSuccessfulParam.getStep());
                        param.setS(lastSuccessfulParam.getS());
                    }
                    if (!hasDR) {
                        param.setD(lastSuccessfulParam.getD());
                        param.setR(lastSuccessfulParam.getR());
                    }
                }
                resultEntity.setMeasureResult(param);
            } else {
                // 本次完全没数据，直接使用上一次的完整缓存
                if (lastSuccessfulParam != null) {
                    resultEntity.setMeasureResult(lastSuccessfulParam);
                } else {
                    // 连上一次的缓存也没有，才报错
                    resultEntity.setErrorCode("0020A022");
                    resultEntity.setIsSuccess(false);
                }
            }

            return resultEntity;

        } catch (Exception e) {
            System.out.println("measure解析异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * S、D、R解析
     */
    public static OperationResultEntity liveMeasure(List<byte[]> responses) {

        if (responses == null || responses.isEmpty()) {
            System.out.println("回应为空，无法解析 measure");
            resultEntity.setErrorCode("0020A021");
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        ChangeParam param = new ChangeParam();
        boolean hasWlS = false;
        boolean hasDR = false;

        try {
            for (byte[] resp : responses) {

                if (resp == null || resp.length < 11) {
                    continue;
                }

                int cmdType1 = resp[2] & 0xFF; // 大类
                int cmdType = resp[3] & 0xFF; // 小类

                // 只处理 0x13
                if (cmdType1 != 0x13) {
                    continue;
                }

                // ---------- wl + s ----------
                if (cmdType == 0x83 || cmdType == 0x05 || cmdType == 0x07) {

                    int step = ((resp[5] & 0xFF) << 8)
                            | (resp[6] & 0xFF);

                    int s0 = ((resp[8] & 0xFF) << 16)
                            | ((resp[9] & 0xFF) << 8)
                            | (resp[10] & 0xFF);

                    param.setStep(String.valueOf(step));
                    param.setS(String.valueOf(s0));
                    hasWlS = true;
                }

                // ---------- d + r ----------
                else if (cmdType == 0x84 || cmdType == 0x06 || cmdType == 0x08) {

                    int d0 = ((resp[4] & 0xFF) << 16)
                            | ((resp[5] & 0xFF) << 8)
                            | (resp[6] & 0xFF);

                    int r0 =  ((resp[8] & 0xFF) << 16)
                            | ((resp[9] & 0xFF) << 8)
                            | (resp[10] & 0xFF);

                    param.setD(String.valueOf(d0));
                    param.setR(String.valueOf(r0));
                    hasDR = true;
                }

                // ---------- 一组数据齐了 ----------
                if (hasWlS && hasDR) {
                    break; // Zero 只需要一组
                }
            }



            if (hasWlS || hasDR) {
                resultEntity.setMeasureResult(param);
            } else {
                resultEntity.setErrorCode("0020A022");
                resultEntity.setIsSuccess(false);
            }

            return resultEntity;

        } catch (Exception e) {
            System.out.println("measure解析异常: " + e.getMessage());
            return null;
        }
    }


    public static OperationResultEntity zero(List<byte[]> responses) {

        if (responses == null || responses.isEmpty()) {
            System.out.println("回应为空，无法解析 measure");
            resultEntity.setErrorCode("0020A021");
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        ChangeParam param = new ChangeParam();
        boolean hasWlS = false;
        boolean hasDR = false;

        try {
            for (byte[] resp : responses) {

                if (resp == null || resp.length < 11) {
                    continue;
                }

                int cmdType1 = resp[2] & 0xFF; // 大类
                int cmdType = resp[3] & 0xFF; // 小类

                // 只处理 0x13
                if (cmdType1 != 0x13) {
                    continue;
                }

                // ---------- wl + s ----------
                if (cmdType == 0x03 || cmdType == 0x05 || cmdType == 0x07) {

                    int step = ((resp[5] & 0xFF) << 8)
                            | (resp[6] & 0xFF);

                    int s0 = ((resp[7] & 0xFF) << 24)
                            | ((resp[8] & 0xFF) << 16)
                            | ((resp[9] & 0xFF) << 8)
                            | (resp[10] & 0xFF);

                    param.setStep(String.valueOf(step));
                    param.setS(String.valueOf(s0));
                    hasWlS = true;
                }

                // ---------- d + r ----------
                else if (cmdType == 0x04 || cmdType == 0x06 || cmdType == 0x08) {

                    int d0 = ((resp[4] & 0xFF) << 16)
                            | ((resp[5] & 0xFF) << 8)
                            | (resp[6] & 0xFF);

                    int r0 = ((resp[7] & 0xFF) << 24)
                            | ((resp[8] & 0xFF) << 16)
                            | ((resp[9] & 0xFF) << 8)
                            | (resp[10] & 0xFF);

                    param.setD(String.valueOf(d0));
                    param.setR(String.valueOf(r0));
                    hasDR = true;
                }

                // ---------- 一组数据齐了 ----------
                if (hasWlS && hasDR) {
                    break; // Zero 只需要一组
                }
            }



            if (hasWlS || hasDR) {
                resultEntity.setMeasureResult(param);
            } else {
                resultEntity.setErrorCode("0020A022");
                resultEntity.setIsSuccess(false);
            }

            return resultEntity;

        } catch (Exception e) {
            System.out.println("measure解析异常: " + e.getMessage());
            return null;
        }
    }

    /*
        扫描的解析规则
    * */
    public static List<Double> xList = new ArrayList<>();
    public static List<Double> yList = new ArrayList<>();

    private static List<List<Double>> parseScanFast(List<byte[]> responses) {
        // 清空上一次的结果
        xList.clear();
        yList.clear();

        for (int i = 0; i < responses.size(); i++) {
            byte[] data = responses.get(i);

            // 只处理 第5位 != 0 的回应
            if (data.length >= 12 && (data[4] & 0xFF) != 0) {
                int x = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                int y = ((data[7] & 0xFF) << 24) | ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
                xList.add(x / 50.0);
                yList.add(y / 8388607.0 * 100);
            } else {
                System.out.println("第 " + (i + 1) + " 条回应不满足条件，跳过");
            }
        }
        log.info("xList: {}", xList);
        log.info("yList: {}", yList);
        return Arrays.asList(xList, yList); // [0] 是 xList， [1] 是 yList
    }

    private static Map<String, String> parseSingleTest(List<byte[]> responses) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < responses.size(); i++) {
            byte[] data = responses.get(i);

            // 只处理 第5位 != 0 的回应
            if (data.length >= 12 && (data[4] & 0xFF) != 0) {
                int x = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                int y = ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
                String result_x = String.valueOf(x / 50.0);
                String result_y = String.valueOf(y / 8388607.0 * 100);

                result.put("wave", result_x);
                result.put("machineValue", String.valueOf(x));
                result.put("value", result_y);

            } else {
                System.out.println("第 " + (i + 1) + " 条回应不满足条件，跳过");
            }
        }
        return result;
    }

    private static OperationResultEntity parseCellPos(List<byte[]> responses, byte[] sendBytes) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        if (sendBytes == null || sendBytes.length < 5) {
            System.out.println("发送报文长度不足5字节，实际长度: " + sendBytes.length);
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 5) {
            System.out.println("回应报文长度不足5字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

//        boolean isEqual = (lastResponse[4] & 0xFF)== 0;
        boolean isEqual1 = sendBytes[4] == lastResponse[4];

        result = isEqual1 ? "true" : "false";

        int pos = (lastResponse[4] & 0xFF) + 1;
        String rawpos = String.valueOf(pos);
        MeasureController.addPosData(rawpos);

        if (DetectionConstants.detectionInfo != null && rawpos != null) {
            DetectionConstants.detectionInfo.put("pos", rawpos);
        }
        resultEntity.setCurrentPoolNum(pos);
        resultEntity.setPos(String.valueOf(pos));

        return resultEntity;
    }

    // 处理 NewIP 指令
    private static OperationResultEntity parseNewIP(List<byte[]> responses) {


        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 12) {
            System.out.println("回应报文长度不足12字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            resultEntity.setIsSuccess(false);
            return resultEntity;
        }

        int b1 = lastResponse[15] & 0xFF;
        int b2 = lastResponse[16] & 0xFF;
        int b3 = lastResponse[17] & 0xFF;
        int b4 = lastResponse[18] & 0xFF;
        resultEntity.setIp(b1 + "." + b2 + "." + b3 + "." + b4);

        return resultEntity;
    }

    private static OperationResultEntity parseCompareBytes(List<byte[]> responses, byte[] sendBytes) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A016");
            return resultEntity;
        }

        if (sendBytes == null || sendBytes.length < 5) {
            System.out.println("发送报文长度不足5字节，实际长度: " + sendBytes.length);
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A017");
            resultEntity.setErrorArgs(new Object[]{sendBytes.length});
            return resultEntity;
        }

        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 5) {
            System.out.println("回应报文长度不足5字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A018");
            resultEntity.setErrorArgs(new Object[]{lastResponse == null ? 0 : lastResponse.length});
            return resultEntity;
        }

        boolean isEqual = lastResponse[2] == sendBytes[2] &&
                lastResponse[3] == sendBytes[3] &&
                lastResponse[4] == sendBytes[4];

        result = isEqual ? "true" : "false";
        resultEntity.setIsSuccess(isEqual);
        resultEntity.setErrorMsg(isEqual ? "成功" : "失败");
        return resultEntity;
    }

    private static OperationResultEntity parseMode(List<byte[]> responses, byte[] sendBytes) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A016");
            return resultEntity;
        }

        if (sendBytes == null || sendBytes.length < 5) {
            System.out.println("发送报文长度不足5字节，实际长度: " + sendBytes.length);
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A017");
            resultEntity.setErrorArgs(new Object[]{sendBytes.length});
            return resultEntity;
        }

        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 5) {
            System.out.println("回应报文长度不足5字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            resultEntity.setIsSuccess(false);
            resultEntity.setErrorCode("0020A018");
            resultEntity.setErrorArgs(new Object[]{lastResponse == null ? 0 : lastResponse.length});
            return resultEntity;
        }

        boolean isEqual = lastResponse[2] == sendBytes[2] &&
                lastResponse[3] == sendBytes[3] &&
                lastResponse[4] == sendBytes[4];
        int mode = lastResponse[4] & 0xFF;
        if (!String.valueOf(mode).equals(DetectionConstants.luminosity)) {
            DetectionConstants.luminosity = String.valueOf(mode);
            DetectionConstants.zeroResult.clear();
        }

        resultEntity.setIsSuccess(isEqual);
        resultEntity.setErrorMsg(isEqual ? "成功" : "失败");
        return resultEntity;
    }

    /*
        处理查找searcharound指令
    * */
    public static int[] parseSearch(List<byte[]> responses){
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            return null;
        }

        int[] result = new int[6];

        for(int i = 10; i <= 15;i++){
            byte[] data = responses.get(i);
            result [i-10] = data[i] & 0xff;
        }

        System.out.println(result);
        return result;
    }

    public static List<ChangeParam> parseBaseLine(List<byte[]> responses) {
        List<ChangeParam> resultList = new ArrayList<>(); // 用来存储每条回应的解析结果
        ChangeParam tempParam = null; // 临时存储 0x05 的结果，等待 0x06 补充完整

        if (responses == null || responses.isEmpty()) {
            System.out.println("回应数量不足，无法解析 BaseLine");
            return resultList;
        }

        try {
            // 遍历所有的回应报文
            for (byte[] response : responses) {
                byte fourthByte = response[3]; // 获取第四位字节值
                byte tenByte = response[9];

                if (fourthByte == 0x07){
                    continue;
                }
                // 如果第四位字节为 05，解析 s
                if (fourthByte == 0x05) {
                    // 只有 s0 的值
                    int s0 = ((response[7] & 0xFF) << 24) | ((response[8] & 0xFF) << 16) | ((response[9] & 0xFF) << 8) | (response[10] & 0xFF);
                    Double wl0 = (double) ((((response[5] & 0xFF) << 8) | (response[6] & 0xFF))/50.0f);
                    tempParam = new ChangeParam(); // 创建一个新的 ChangeParam 对象，等待 0x06 补充
                    tempParam.setS(String.valueOf(s0)); // 设置 s0
                    tempParam.setWl(String.valueOf(wl0));
                    System.err.println(wl0);

//                    if (wl0 == 538.0){
//                        sleep(1000);
//                    }
                }

                // 如果第四位字节为 06，解析 d 和 r，结合之前的 s0
                if (fourthByte == 0x06 && tempParam != null) {
                    int d0 = ((response[4] & 0xFF) << 16) | ((response[5] & 0xFF) << 8) | (response[6] & 0xFF);
                    int r0 = ((response[8] & 0xFF) << 16) | ((response[9] & 0xFF) << 8) | (response[10] & 0xFF);
                    tempParam.setD(String.valueOf(d0)); // 设置 d0
                    tempParam.setR(String.valueOf(r0)); // 设置 r0

                    // 将解析完成的对象添加到结果列表中
                    resultList.add(tempParam);

                    // 解析完成后重置 tempParam 为 null
                    tempParam = null;
                }
            }
            System.out.println(resultList);

        } catch (Exception e) {
            System.out.println("BaseLine解析异常: " + e.getMessage());
        }

        return resultList;
    }

    static List<Integer> deviceInfo = new ArrayList<>();
    public static OperationResultEntity pareSearch(List<byte[]> responses) {
        deviceInfo.clear();

        if (responses == null || responses.isEmpty()) {
            return resultEntity; // 返回空列表
        }

        byte[] last = responses.get(responses.size() - 1);
        if (last.length < 12) {
            return resultEntity; // 长度不足不处理
        }

        for (int i = 9; i <= 14; i++) {
            deviceInfo.add(last[i] & 0xFF); // 转换为无符号十进制数
        }

        if (last.length > 47) {
            StringBuilder asciiStr = new StringBuilder();
            for (int i = 33; i <= 47; i++) {
                int val = last[i] & 0xFF;
                char ch = (char) val;
                asciiStr.append(ch);
                if (i == 34) {
                    asciiStr.append('-');
                }
            }
            resultEntity.setDeviceSerialNumber(asciiStr.toString());
            System.out.println("序列号：" + asciiStr.toString());
        }

        return resultEntity;
    }

    public static String deviceIp(List<byte[]> responses) {

        if (responses == null || responses.isEmpty()) {
            return ""; // 返回空列表
        }

        byte[] last = responses.get(responses.size() - 1);
        if (last.length < 12) {
            return ""; // 长度不足不处理
        }

        StringBuilder deviceIp = new StringBuilder();

        for (int i = 15; i <=18; i++) {
            deviceInfo.add(last[i] & 0xFF); // 转换为无符号十进制数
            deviceIp.append(last[i] & 0xFF).append(".");
        }

        return deviceIp.substring(0,deviceIp.length()-1);
    }
}
