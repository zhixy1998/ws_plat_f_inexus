/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

/*
    分不同的情况，解析报文
* */
package com.persee.workstation.uv_inexus.util.changeUtil;

import com.persee.workstation.uv_inexus.websocket.MeasureController;
import com.persee.workstation.uv_inexus.websocket.liveDetection.DetectionConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.*;

import static com.persee.workstation.uv_inexus.websocket.MeasureController.addLampPosData;

@Slf4j
public class ResponseDeal {

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

    /**
     * 根据命令ID分发解析逻辑
     */
    public static Object parse(String cmdId, List<byte[]> responses,byte[] sendBytes) {
        switch (cmdId) {
            case "GetSn":
                return parseSn(responses);

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
                return parseCompareBytes(responses, sendBytes);
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
                return parseBooleanFrom5thByte(responses);
            case "SetCellPos":
                return parseSetCellPos(responses, sendBytes);
            case "GetAccessoryType":
                return parseType(responses);
            case "Measure":
                return measure(responses);
            case "Zero":
                return parseZero(responses);
            case "ScanFast":
            case "Scan":
                return parseScanFast(responses);
            case "BaseLine":
            case "BaseLineFast":
                return parseBaseLine(responses);


            case "SetResponseTime":
                return parseResponseTime(responses, sendBytes);

            case "GetChangeLampWl":
                return parseChangeLampWl(responses);

//            case "SetCellPos":
//                return parseCellPos(responses, sendBytes);

            case "SetSlit1":
                return parseSlit1(responses, sendBytes);

           case "GetCellPos":
               return parseCellPos(responses, sendBytes);

            case "GetInitStatus":
                return getInitStatus(responses);
            case "GetLampPos":
                return parseLampPos(responses);

            case "SearchAround":
            case "SearchInSameZoom":
                return pareSearch(responses);
            case "InitSlit1Moto1":
                return parseInitSlit1Motor(responses, sendBytes);

            case "GetPhotoMode":
                return parsePhotoMode(responses);

//            case "NewIP":
//                return parseNewIP(responses);
            default:
                return 0;
        }
    }

    private static Object parsePhotoMode(List<byte[]> responses) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            result = "false";
            return false;
        }

        for (int i = 0; i < responses.size(); i++) {
            byte[] data = responses.get(i);

            if (data[3] == 0x05)  {
                int mode = data[4] & 0xFF;

                DetectionConstants.luminosity = String.valueOf(mode + 1);

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
            }
        }
        return true;
    }


    private static Object parseInitSlit1Motor(List<byte[]> responses, byte[] sendBytes) {
        if (responses == null || responses.isEmpty()) {
            result = "false"; // 设置全局变量
            return false;
        }

        byte[] last = responses.get(responses.size() - 1);

        if ((last[3] & 0xFF) == 0x04) {
            boolean success = last.length >= 5 && (last[4] & 0xFF) == 0;
            result = success ? "true" : "false"; // 根据结果更新 result
        }
        return false;
    }

    private static Object parseChangeLampWl(List<byte[]> responses) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            result = "false";
            return false;
        }

        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 6) { // 至少要有6字节
            System.out.println("回应报文长度不足6字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            result = "false";
            return false;
        }

        // 判断第3个字节（索引2）是否为 0x05
        if ((lastResponse[3] & 0xFF) != 0x05) {
            System.out.printf("最后一条报文第3位不是0x05（实际值: %02X），跳过解析\n", lastResponse[2]);
            result = "false";
            return false;
        }

        // 如果通过检查，则进行解析
        int wl = (((lastResponse[4] & 0xFF) << 8) | (lastResponse[5] & 0xFF));
        double ChangeLampWl = wl / 50.0;
        String rawchagelampwl = String.valueOf(ChangeLampWl);

        System.out.printf("波长原始值=%d, 解析结果=%.2f\n", wl, ChangeLampWl);

        MeasureController.addWlData(rawchagelampwl);

        if (DetectionConstants.detectionInfo != null && rawchagelampwl != null) {
            DetectionConstants.detectionInfo.put("changeLampWl", rawchagelampwl);
        }

        result = "true";
        return true;
    }


    private static Object parseResponseTime(List<byte[]> responses, byte[] sendBytes) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            result = "false";
            return false;
        }

        if (sendBytes == null || sendBytes.length < 5) {
            System.out.println("发送报文长度不足5字节，实际长度: " + sendBytes.length);
            result = "false";
            return false;
        }

        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 5) {
            System.out.println("回应报文长度不足5字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            result = "false";
            return false;
        }

        boolean isEqual = lastResponse[2] == sendBytes[2] &&
                lastResponse[3] == sendBytes[3] &&
                lastResponse[4] == sendBytes[4];

        result = isEqual ? "true" : "false";

        float time = (lastResponse[4] & 0xFF) / 100.0f;
        String rawtime = String.valueOf(time);
        MeasureController.addTimeData(rawtime);

        long l = System.currentTimeMillis();
        if (DetectionConstants.detectionInfo != null && rawtime != null) {
            DetectionConstants.detectionInfo.put("time", rawtime);
        }

        long l1 = System.currentTimeMillis() - l;
        System.out.println("耗时：" + l1);

        return isEqual;
    }

    private static Object parseSlit1(List<byte[]> responses, byte[] sendBytes) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            result = "false";
            return false;
        }

        if (sendBytes == null || sendBytes.length < 5) {
            System.out.println("发送报文长度不足5字节，实际长度: " + sendBytes.length);
            result = "false";
            return false;
        }

        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 5) {
            System.out.println("回应报文长度不足5字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            result = "false";
            return false;
        }

        boolean isEqual = lastResponse[2] == sendBytes[2] &&
                lastResponse[3] == sendBytes[3] &&
                lastResponse[4] == sendBytes[4];
//        boolean isEqual = (lastResponse[4] & 0xFF) == 0;

        result = isEqual ? "true" : "false";

        float Bandwidth = (lastResponse[4] & 0xFF) / 10.f;
        String SpectralBandwidth = String.valueOf(Bandwidth);
        MeasureController.addBandwidthData(SpectralBandwidth);

        if (DetectionConstants.detectionInfo != null && SpectralBandwidth != null) {
            DetectionConstants.detectionInfo.put("bandwidth", SpectralBandwidth);
        }

        return isEqual;
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
private static Object parseLampPos(List<byte[]> responses) {
    byte[] lastResponse = responses.get(responses.size() - 1);
    if (lastResponse == null || lastResponse.length < 5) {
        System.out.println("回应报文长度不足5字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
        result = "false";
        return false;
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
    return pos;
}


    /**
     * 通用：最后一条回应的第5位是否为0
     */
    private static Boolean parseBooleanFrom5thByte(List<byte[]> responses) {
        if (responses == null || responses.isEmpty()) {
            result = "false"; // 设置全局变量
            return false;
        }

        byte[] last = responses.get(responses.size() - 1);
        boolean success = last.length >= 5 && (last[4] & 0xFF) == 0;
        result = success ? "true" : "false"; // 根据结果更新 result
        return success;
    }

    private static Boolean getInitStatus(List<byte[]> responses) {
        if (responses == null || responses.isEmpty()) {
            result = "false"; // 设置全局变量
            return false;
        }

        byte[] last = responses.get(responses.size() - 1);
        boolean success = last.length >= 5 && (last[4] & 0xFF) == 1;
        result = success ? "true" : "false"; // 根据结果更新 result
        return success;
    }

    private static Boolean parseSetCellPos(List<byte[]> responses, byte[] sendBytes) {
        if (responses == null || responses.isEmpty()) {
            result = "false"; // 设置全局变量
            return false;
        }

        byte[] last = responses.get(responses.size() - 1);
        boolean success = last.length >= 5 && (last[4] & 0xFF) == (sendBytes[4] & 0xFF);
        result = success ? "true" : "false"; // 根据结果更新 result
        return success;
    }

    /**
     * GetSn：拼接第2~4条回应中的序列号部分
     */


    private static String parseSn(List<byte[]> responses) {
        if (responses.size() < 4) {
            return null;
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

        return Sn; // 返回处理后的结果
    }

    /**
     * 附件类型解析
     */
    private static Integer parseType(List<byte[]> responses) {
        if (responses ==  null || responses.isEmpty()) {
            return -1;
        }

        byte[] last = responses.get(responses.size() - 1);

        int type = (last[4] & 0xFF);
        String sampleType = (type >= 0 && type < SAMPLE_TYPE_NAMES.length)
                ? SAMPLE_TYPE_NAMES[type]
                : "未知类型(" + type + ")";

        // 添加到队列，供推送线程发送
        MeasureController.addSampleTypeData(sampleType);
        // 转成无符号 int
        return last[4] & 0xFF;
    }

    /**
        S、D、R解析
     */
    public static ChangeParam parseZero(List<byte[]> responses) {
        ChangeParam param = new ChangeParam();

        if (responses == null || responses.isEmpty()) {
            System.out.println("回应为空，无法解析 Zero");
            return null;
        }

        try {
            // 遍历所有回应，根据第3个字节判断类型
            for (byte[] resp : responses) {
                if (resp == null || resp.length < 11) {
                    continue; // 跳过无效报文
                }

                int cmdType1 = resp[2] & 0xFF;
                int cmdType = resp[3] & 0xFF; // 第3个字节

                // 0x03 或 0x05 => 解析 wl0 和 s0
                if (cmdType == 0x03) {
                    int wl0 = ((resp[5] & 0xFF) << 8) | (resp[6] & 0xFF);
                    int s0 = ((resp[7] & 0xFF) << 24) | ((resp[8] & 0xFF) << 16) | ((resp[9] & 0xFF) << 8) | (resp[10] & 0xFF);
                    param.setS(String.valueOf(s0));
                    param.setWl(String.valueOf(wl0)); // 可选：记录 wl0，方便计算
                } else if (cmdType == 0x07 && cmdType1 == 0x13) {
                    int wl0 = ((resp[5] & 0xFF) << 8) | (resp[6] & 0xFF);
                    int s0 = ((resp[7] & 0xFF) << 24) | ((resp[8] & 0xFF) << 16) | ((resp[9] & 0xFF) << 8) | (resp[10] & 0xFF);
                    param.setS(String.valueOf(s0));
                    param.setWl(String.valueOf(wl0));
                } else if (cmdType == 0x08 && cmdType1 == 0x13) {
                    int d0 = ((resp[4] & 0xFF) << 16) | ((resp[5] & 0xFF) << 8) | (resp[6] & 0xFF);
                    int r0 = ((resp[7] & 0xFF) << 24) | ((resp[8] & 0xFF) << 16) | ((resp[9] & 0xFF) << 8) | (resp[10] & 0xFF);

                    param.setD(String.valueOf(d0));
                    param.setR(String.valueOf(r0));

                    // 若之前解析到 wl0、s0，可计算并推送
                    if (param.getWl() != null && param.getS() != null) {
                        double wl0 = Double.parseDouble(param.getWl());
                        double s0 = Double.parseDouble(param.getS());
                        double result_x = wl0 / 50.0;
                        double result_y = s0 / 8388607.0 * 100;

                        String raw = result_x + "," + result_y;
                        MeasureController.addData(raw); // 推送
                    }
                }

                // 0x04 或 0x06 => 解析 d0 和 r0，并计算结果
                else if (cmdType == 0x04) {
                    int d0 = ((resp[4] & 0xFF) << 16) | ((resp[5] & 0xFF) << 8) | (resp[6] & 0xFF);
                    int r0 = ((resp[7] & 0xFF) << 24) | ((resp[8] & 0xFF) << 16) | ((resp[9] & 0xFF) << 8) | (resp[10] & 0xFF);

                    param.setD(String.valueOf(d0));
                    param.setR(String.valueOf(r0));

                    // 若之前解析到 wl0、s0，可计算并推送
                    if (param.getWl() != null && param.getS() != null) {
                        double wl0 = Double.parseDouble(param.getWl());
                        double s0 = Double.parseDouble(param.getS());
                        double result_x = wl0 / 50.0;
                        double result_y = s0 / 8388607.0 * 100;

                        String raw = result_x + "," + result_y;
                        MeasureController.addData(raw); // 推送
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Zero解析异常: " + e.getMessage());
            return null;
        }
        if (StringUtils.hasText(param.getWl())) {
            DetectionConstants.zeroResult.putIfAbsent(param.getWl(), param);
        }
        return param;
    }

    public static ChangeParam measure(List<byte[]> responses){
        ChangeParam param = new ChangeParam();

        if (responses == null || responses.size() < 2) {
            System.out.println("回应数量不足，无法解析 Zero");
            return null;
        }

        try {
            // 倒数第二条
            byte[] secondLast = responses.get(responses.size() - 2);
            int wl0 = (((secondLast[5] & 0xFF) << 8) | (secondLast[6] & 0xFF));
            int s0 = ((secondLast[8] & 0xFF) << 16) | ((secondLast[9] & 0xFF) << 8) | (secondLast[10] & 0xFF);
            param.setS(String.valueOf(s0));

            // 倒数第一条
            byte[] last = responses.get(responses.size() - 1);
            int d0 = ((last[4] & 0xFF) << 16) | ((last[5] & 0xFF) << 8) | (last[6] & 0xFF);
            int r0 = ((last[8] & 0xFF) << 16) | ((last[9] & 0xFF) << 8) | (last[10] & 0xFF);

            param.setWl(String.valueOf(wl0 / 50.0));
            param.setD(String.valueOf(d0));
            param.setR(String.valueOf(r0));

        } catch (Exception e) {
            System.out.println("Zero解析异常: " + e.getMessage());
            return null;
        }

        return param;
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

    private static Object parseCellPos(List<byte[]> responses, byte[] sendBytes) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            result = "false";
            return false;
        }

        if (sendBytes == null || sendBytes.length < 5) {
            System.out.println("发送报文长度不足5字节，实际长度: " + sendBytes.length);
            result = "false";
            return false;
        }

        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 5) {
            System.out.println("回应报文长度不足5字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            result = "false";
            return false;
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

        return pos;
    }

    // 处理 NewIP 指令
    private static String parseNewIP(List<byte[]> responses) {


        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            return null;
        }

        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 12) {
            System.out.println("回应报文长度不足12字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            return null;
        }

        int b1 = lastResponse[15] & 0xFF;
        int b2 = lastResponse[16] & 0xFF;
        int b3 = lastResponse[17] & 0xFF;
        int b4 = lastResponse[18] & 0xFF;

        return b1 + "." + b2 + "." + b3 + "." + b4;
    }

    private static Boolean parseCompareBytes(List<byte[]> responses, byte[] sendBytes) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            result = "false";
            return false;
        }

        if (sendBytes == null || sendBytes.length < 5) {
            System.out.println("发送报文长度不足5字节，实际长度: " + sendBytes.length);
            result = "false";
            return false;
        }

        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 5) {
            System.out.println("回应报文长度不足5字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            result = "false";
            return false;
        }

        boolean isEqual = lastResponse[2] == sendBytes[2] &&
                lastResponse[3] == sendBytes[3] &&
                lastResponse[4] == sendBytes[4];

        result = isEqual ? "true" : "false";
        return isEqual;
    }

    /**
     * 设置光度模式
     */
    private static Boolean parseMode(List<byte[]> responses, byte[] sendBytes) {
        if (responses == null || responses.isEmpty()) {
            System.out.println("回应列表为空");
            result = "false";
            return false;
        }

        if (sendBytes == null || sendBytes.length < 5) {
            System.out.println("发送报文长度不足5字节，实际长度: " + sendBytes.length);
            result = "false";
            return false;
        }

        byte[] lastResponse = responses.get(responses.size() - 1);
        if (lastResponse == null || lastResponse.length < 5) {
            System.out.println("回应报文长度不足5字节，实际长度: " + (lastResponse == null ? 0 : lastResponse.length));
            result = "false";
            return false;
        }

        boolean isEqual = lastResponse[2] == sendBytes[2] &&
                lastResponse[3] == sendBytes[3] &&
                lastResponse[4] == sendBytes[4];
        //设置光度模式时，实时更新共享变量
        int mode = lastResponse[4] & 0xFF;
        DetectionConstants.luminosity = String.valueOf(mode);

        result = isEqual ? "true" : "false";
        return isEqual;
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
    public static List<Integer> pareSearch(List<byte[]> responses) {
        deviceInfo.clear();

        if (responses == null || responses.isEmpty()) {
            return deviceInfo; // 返回空列表
        }

        byte[] last = responses.get(responses.size() - 1);
        if (last.length < 12) {
            return deviceInfo; // 长度不足不处理
        }

        for (int i = 9; i <= 14; i++) {
            deviceInfo.add(last[i] & 0xFF); // 转换为无符号十进制数
        }

        return new ArrayList<>(deviceInfo); // 返回副本，避免外部修改
    }


}
