package com.persee.workstation.uv_inexus.util.nettyUtil.SeRePa;/*
    分不同的情况，解析报文
* */


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ResponseParser {

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
            case "SetStartWl":
            case "SetEndWl":
            case "SetFilter":
            case "SetGain":
            case "SetResponseTime":
            case "SetPhotoMode":
            case "SetSlit1":
            case "SetCellPos":
            case "SetPadModeTrue":
            case "SetPadModeFalse":
            case "SetPCModeTrue":
            case "SetPCModeFalse":
            case "SetProgrammeModeTrue":
            case "SetProgrammeModeFalse":
            case "SetInitStatus":
                return parseCompare345Bytes(responses, sendBytes);

            case "InitSampleCell":
            case "InitFilterMotor":
            case "InitSlit1Motor":
            case "InitWlMotor":
            case "InitLampMotor":
            case "CheckWLamp":
            case "CheckDLamp":
            case "CheckWl":
                return parseBooleanFrom5thByte(responses);

            case "ScanFast":
            case "Scan":
                return parseScanFast(responses);

            default:
                System.out.println("尚未定义命令 [" + cmdId + "] 的解析规则");
                return null;
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
    public static String snValue = "";

    public static String parseSn(List<byte[]> responses) {
        if (responses.size() < 4) {
            snValue = "";  // 同步清空
            return "";
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

        snValue = sn.toString().trim(); // 存储到全局变量
        return snValue;
    }

    /*
        扫描的解析规则
    * */
    public static List<Double> xList = new ArrayList<>();
    public static List<Double> yList = new ArrayList<>();

    public static List<List<Double>> parseScanFast(List<byte[]> responses) {
        // 清空上一次的结果
        xList.clear();
        yList.clear();

        for (int i = 0; i < responses.size(); i++) {
            if (i % 2 == 0) { // 奇数条（0-based）
                byte[] data = responses.get(i);
                if (data.length >= 12) {
                    int x = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
                    int y = ((data[8] & 0xFF) << 16) | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
                    xList.add(x / 50.0);
                    yList.add(y / 8388607.0 * 100);
                } else {
                    System.out.println("第 " + (i + 1) + " 条回应太短，跳过");
                }
            }
        }

        return Arrays.asList(xList, yList); // [0] 是 xList， [1] 是 yList
    }

    /*设置参数的解析规则*/
    public static Boolean parseCompare345Bytes(List<byte[]> responses, byte[] sendBytes) {
        if (responses == null || responses.isEmpty() || sendBytes.length < 6) {
            result = "false";
            return false;
        }

        byte[] last = responses.get(responses.size() - 1);
        if (last.length < 6) {
            result = "false";
            return false;
        }

        boolean isEqual =
                last[2] == sendBytes[2] &&
                        last[3] == sendBytes[3] &&
                        last[4] == sendBytes[4];

        result = isEqual ? "true" : "false";
        return isEqual;
    }

}
