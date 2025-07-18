/*
    分不同的情况，解析报文
* */
package com.persee.workstation.uv_inexus.util.changeUtil;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ResponseDeal {

    public static String result = "false";
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
            case "SetResponseTime":
            case "SetPhotoMode":

            case "SetPadModeTrue":
            case "SetPadModeFalse":
            case "SetPCModeTrue":
            case "SetPCModeFalse":
            case "SetProgrammeModeTrue":
            case "SetProgrammeModeFalse":
            case "SetInitStatus":
                return parseCompareBytes(responses, sendBytes);

            case "InitSampleCell":
            case "InitFilterMotor":
            case "InitSlit1Motor":
            case "InitWlMotor":
            case "SetSlit1":
            case "SetCellPos":
            case "InitLampMotor":
            case "CheckWLamp":
            case "CheckDLamp":
            case "CheckWl":
            case "GetInitStatus":
            case "Dark":
            case "DisConnect":
            case "DisConnectIAP":
            case "GetAccessoryType":
                return parseBooleanFrom5thByte(responses);
            case "Measure":
            case "Zero":
                return parseZero(responses);
            case "ScanFast":
            case "Scan":
            case "BaseLine":
            case "BaseLineFast":
                return parseScanFast(responses);


//            case "NewIP":
//                return parseNewIP(responses);
            default:
                return 0;
        }
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

        return sn.toString(); // 存储到全局变量
    }
    /*
        S、D、R解析
     */
    public static ChangeParam parseZero(List<byte[]> responses){
        ChangeParam param = new ChangeParam();

        if (responses == null || responses.size() < 2) {
            System.out.println("回应数量不足，无法解析 Zero");
            return null;
        }

        try {
            // 倒数第二条
            byte[] secondLast = responses.get(responses.size() - 2);
            int s0 = ((secondLast[8] & 0xFF) << 16) | ((secondLast[9] & 0xFF) << 8) | (secondLast[10] & 0xFF);
            param.setS(String.valueOf(s0));

            // 倒数第一条
            byte[] last = responses.get(responses.size() - 1);
            int d0 = ((last[4] & 0xFF) << 16) | ((last[5] & 0xFF) << 8) | (last[6] & 0xFF);
            int r0 = ((last[8] & 0xFF) << 16) | ((last[9] & 0xFF) << 8) | (last[10] & 0xFF);

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
                int y = ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
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


}
