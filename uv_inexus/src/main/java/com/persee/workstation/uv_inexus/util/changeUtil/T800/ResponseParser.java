/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.changeUtil.T800;

import com.alibaba.fastjson.JSON;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;

import java.util.*;

/**
 * ResponseParser - 解析接收到的回复报文
 * 支持：
 * 1. 多条回复报文（适用于特殊指令）
 * 2. 每条报文多个 TLV
 * 3. 对比发送报文的 T/V
 */
public class ResponseParser {

    // 用于存储最新解析的下位机信息
    public static final Map<String, Object> negotiationInfo = new HashMap<>();

    public static String Result;

    // 标记是否处于连续报文接收模式
    public static volatile boolean inContinuous = false;

    // 上一次收到 0x0A02 报文的时间戳
    private static long lastContinuousTime = 0;

    // 超时时间（单位：ms，根据下位机报文频率调节）
    private static final long END_TIMEOUT = 2000;

    public static List<Map<String, Object>> parseMultipleResponses(List<byte[]> responses, byte[] sendBytes) {
//    public static Object parseMultipleResponses(List<byte[]> responses, byte[] sendBytes) {
//        List<Object> allResults = new ArrayList<>();
        List<Map<String, Object>> allResults = new ArrayList<>();

        if (responses == null || responses.isEmpty()) {
            System.out.println("无回复报文");
            return allResults;
        }

        // 提取发送报文的 T/V 列表
        List<byte[]> sendVList = extractSendTLVVs(sendBytes);
        List<byte[]> sendTList = extractSendTLVTs(sendBytes);

        // 遍历每条接收报文
        for (int msgIndex = 0; msgIndex < responses.size(); msgIndex++) {
            byte[] recvBytes = responses.get(msgIndex);

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("rawBytes", recvBytes); // 保留原始报文

            // ---------------- 校验和 ----------------
            int calculatedChecksum = 0;
            for (int i = 0; i < recvBytes.length - 1; i++) calculatedChecksum += recvBytes[i];
            calculatedChecksum &= 0xFF;

//            int receivedChecksum = recvBytes[recvBytes.length - 1] & 0xFF;
//            if (calculatedChecksum != receivedChecksum) {
//                allResults.add(String.format("[报文%d] 校验和错误", msgIndex));
//                continue;

            int receivedChecksum = recvBytes[recvBytes.length - 1] & 0xFF;
            if (calculatedChecksum != receivedChecksum) {
                resultMap.put("error", String.format("[报文%d] 校验和错误", msgIndex));
                allResults.add(resultMap);
                continue;
            }

            // ---------------- 解析 TLV ----------------
            int tlvIndex = 8; // TLV 从第 8 个字节开始
            int checksumIndex = recvBytes.length - 1;
            int sendIndex = 0; // 对应发送报文的下标

            List<Map<String, Object>> tlvResults = new ArrayList<>();

            while (tlvIndex + 4 <= checksumIndex
                    && sendIndex < sendVList.size()
                    && sendIndex < sendTList.size()) {

                // 提取接收的 T 和 L
                byte[] recvTBytes = new byte[2];
                System.arraycopy(recvBytes, tlvIndex, recvTBytes, 0, 2);

                int lHigh = recvBytes[tlvIndex + 2] & 0xFF;
                int lLow = recvBytes[tlvIndex + 3] & 0xFF;
                int vLen = (lHigh << 8) | lLow;

                if (tlvIndex + 4 + vLen > checksumIndex) break; // 防越界

                // 提取接收 V
                byte[] vBytes = new byte[vLen];
                System.arraycopy(recvBytes, tlvIndex + 4, vBytes, 0, vLen);

                Map<String, Object> tlvMap = new HashMap<>();
                tlvMap.put("T", recvTBytes);
                tlvMap.put("V", vBytes);

                // 对比 T/V
                boolean tMatch = compareVBytes(recvTBytes, sendTList.get(sendIndex));
                boolean vMatch = compareVBytes(vBytes, sendVList.get(sendIndex));

                int typeKey = (recvTBytes[0] & 0xFF) << 8 | (recvTBytes[1] & 0xFF);
                String typeHex = String.format("0x%04X", typeKey);
                tlvMap.put("typeKey", typeKey);//改


//                // 特殊类型解析
//                switch (typeKey) {
//                    case 0x14FC: // Negotiation 类型
//                        allResults.add(String.format("解析结果:%s",
//                                parseNegotiation(vBytes)));
//                        break;
//                    case 0x1401: // 连接
//                        allResults.add(String.format("解析结果：%s",
//                                parseConnect(vBytes)
//                        ));
//                        break;
//                    case 0x13FD: // 获取下位机的序列号
//                        allResults.add(String.format(parseSn(vBytes)
//                        ));
//                        break;
//                    case 0x0A02: // 获取扫描结果
//                        allResults.add(String.format("解析结果：%s",
//                                parseScan(vBytes)
//                        ));
//                        break;
//                    default: // 默认逻辑
//                      allResults.add(String.format("[报文%d][TLV %d][T=%s] T匹配:%s, V匹配:%s",
//                              msgIndex + 1, sendIndex + 1, typeHex, tMatch, vMatch));
//                        if (tMatch && vMatch) {
//                            Result = "true";
//                        } else {
//                            Result = "false";
//                        }
//                        break;
//                }
//
//                // 移动到下一个 TLV
//                tlvIndex += 4 + vLen;
//                sendIndex++;
//            }
//        }
                // 特殊类型解析
                switch (typeKey) {
                    case 0x14FC:
                        tlvMap.put("negotiation", parseNegotiation(vBytes));
                        break;
                    case 0x1401:
                        parseConnect(vBytes);
                        break;
                    case 0x13FD:
                        parseSn(vBytes);
                        break;
                    case 0x0A02:
                        parseScan(vBytes);
                        break;
                    case 0x1307:
                        parseMeasure(vBytes);
                    default:
                        Result = (tMatch && vMatch) ? "true" : "false";
                        break;
                }

                tlvResults.add(tlvMap);
                tlvIndex += 4 + vLen;
                sendIndex++;
            }

            resultMap.put("tlvs", tlvResults);
            allResults.add(resultMap);
        }

        return allResults;
    }

    public static ChangeParam parseMeasure(byte[] vBytes) {
        ChangeParam param = new ChangeParam();

        if (vBytes == null) {
            System.out.println("vBytes为空");
            return null;
        }

        //        int wl0 = (((vBytes[1] & 0xFF) << 8) | (vBytes[2] & 0xFF));
        int s0 = ((vBytes[3] & 0xFF) << 16) | ((vBytes[4] & 0xFF) << 8) | (vBytes[5] & 0xFF);
        int d0 = ((vBytes[6] & 0xFF) << 16) | ((vBytes[7] & 0xFF) << 8) | (vBytes[8] & 0xFF);
        int r0 = ((vBytes[10] & 0xFF) << 16) | ((vBytes[11] & 0xFF) << 8) | (vBytes[12] & 0xFF);
        param.setS(java.lang.String.valueOf(s0));
        param.setD(java.lang.String.valueOf(d0));
        param.setR(java.lang.String.valueOf(r0));

        return param;
    }

    //解析下位机自动上报的报文
    public static Object parsePassiveReport(byte[] recvBytes) {
        List<Object> results = new ArrayList<>();

        if (recvBytes == null || recvBytes.length < 9) {
            results.add("报文为空或长度不足");
            return results;
        }

        // ---------------- 校验和 ----------------
        int calculatedChecksum = 0;
        for (int i = 0; i < recvBytes.length - 1; i++) {
            calculatedChecksum += recvBytes[i];
        }
        calculatedChecksum &= 0xFF;
        int receivedChecksum = recvBytes[recvBytes.length - 1] & 0xFF;

        if (calculatedChecksum != receivedChecksum) {
            results.add("校验和错误");
            return results;
        }

        // ---------------- 解析 TLV ----------------
        int tlvIndex = 8; // TLV 从第 8 个字节开始
        int checksumIndex = recvBytes.length - 1;
        int tlvCounter = 0;

        while (tlvIndex + 4 <= checksumIndex) {
            // 提取 T (2字节)
            byte[] tBytes = new byte[2];
            System.arraycopy(recvBytes, tlvIndex, tBytes, 0, 2);

            // 提取 L (2字节，大端)
            int lHigh = recvBytes[tlvIndex + 2] & 0xFF;
            int lLow = recvBytes[tlvIndex + 3] & 0xFF;
            int vLen = (lHigh << 8) | lLow;

            if (tlvIndex + 4 + vLen > checksumIndex) break; // 防越界

            // 提取 V
            byte[] vBytes = new byte[vLen];
            System.arraycopy(recvBytes, tlvIndex + 4, vBytes, 0, vLen);

            // 转换 key
            int typeKey = (tBytes[0] & 0xFF) << 8 | (tBytes[1] & 0xFF);
            String typeHex = String.format("0x%04X", typeKey);

            // 解析
            if (typeKey == 0x0A02){
                int x = ((recvBytes[5] & 0xFF) << 8) | (recvBytes[6] & 0xFF);
                int y = ((recvBytes[8] & 0xFF) << 16) | ((recvBytes[9] & 0xFF) << 8) | (recvBytes[10] & 0xFF);
                double result_x = x / 50.0;
                double result_y = y / 8388607.0 * 100;

                results.add("X=" + result_x);
                results.add("Y=" + result_y);
            }
            // 移动到下一个 TLV
            tlvIndex += 4 + vLen;
            tlvCounter++;
        }

        return results;
    }

    //解析多个回复的情况
//    public static Object parseNullReport(byte[] recvBytes) {
//        List<Object> results = new ArrayList<>();
//        ChangeParam changeParam = new ChangeParam();
//
//        if (recvBytes == null || recvBytes.length < 9) {
//            results.add("报文为空或长度不足");
//            return results;
//        }
//
//        // ---------------- 校验和 ----------------
//        int calculatedChecksum = 0;
//        for (int i = 0; i < recvBytes.length - 1; i++) {
//            calculatedChecksum += recvBytes[i];
//        }
//        calculatedChecksum &= 0xFF;
//        int receivedChecksum = recvBytes[recvBytes.length - 1] & 0xFF;
//
//        if (calculatedChecksum != receivedChecksum) {
//            results.add("校验和错误");
//            return results;
//        }
//
//        // ---------------- 解析 TLV ----------------
//        int tlvIndex = 8; // TLV 从第 8 个字节开始
//        int checksumIndex = recvBytes.length - 1;
//        int tlvCounter = 0;
//
//        while (tlvIndex + 4 <= checksumIndex) {
//            // 提取 T (2字节)
//            byte[] tBytes = new byte[2];
//            System.arraycopy(recvBytes, tlvIndex, tBytes, 0, 2);
//
//            // 提取 L (2字节，大端)
//            int lHigh = recvBytes[tlvIndex + 2] & 0xFF;
//            int lLow = recvBytes[tlvIndex + 3] & 0xFF;
//            int vLen = (lHigh << 8) | lLow;
//
//            if (tlvIndex + 4 + vLen > checksumIndex) break; // 防越界
//
//            // 提取 V
//            byte[] vBytes = new byte[vLen];
//            System.arraycopy(recvBytes, tlvIndex + 4, vBytes, 0, vLen);
//
//            // 转换 key
//            int typeKey = (tBytes[0] & 0xFF) << 8 | (tBytes[1] & 0xFF);
//            String typeHex = String.format("0x%04X", typeKey);
//
//            // 解析
//            if (typeKey == 0x0A02){
//                int x = ((vBytes[0] & 0xFF) << 8) | (vBytes[1] & 0xFF);
//                int y = ((vBytes[0] & 0xFF) << 16) | ((vBytes[1] & 0xFF) << 8) | (vBytes[2] & 0xFF);
//
//                int s = ((vBytes[1] & 0xFF) << 16) | ((vBytes[2] & 0xFF) << 8) | (vBytes[3] & 0xFF);
//                int d = ((vBytes[0] & 0xFF) << 16) | ((vBytes[2] & 0xFF) << 8) | (vBytes[1] & 0xFF);
//                int r = ((vBytes[0] & 0xFF) << 16) | ((vBytes[3] & 0xFF) << 8) | (vBytes[2] & 0xFF);
//
//                double result_x = x / 50.0;
//                double result_y = y / 8388607.0 * 100;
//
//                changeParam.setWl(String.valueOf(result_x));
//                changeParam.setS(String.valueOf(s));
//                changeParam.setD(String.valueOf(d));
//                changeParam.setR(String.valueOf(r));
//
//                if (changeParam != null && changeParam.getWl() != null && changeParam.getS() != null && changeParam.getD() != null && changeParam.getR() != null) {
//                    Constants.measurd_data.add(changeParam);  // 完成的对象添加到公共列表
//                    System.out.println("已完成一个完整的对象：" + changeParam);
//                    Constants.instrumentData.add(changeParam.toString());
////                    changeParam = null;  // 清空临时对象，准备接收下一个
//                }
//
//            }
//            // 移动到下一个 TLV
//            tlvIndex += 4 + vLen;
//            tlvCounter++;
//        }
//
//        return changeParam;
////        return results;
//    }



// 解析多个回复的情况
public static Object parseNullReport(byte[] recvBytes) {
    if (recvBytes == null) {
        System.err.println("parseNullReport: 收到空报文");
        return null;
    }

    if (recvBytes.length < 9) {
        System.err.println("parseNullReport: 报文长度不足，len=" + recvBytes.length);
        return null;
    }

    ChangeParam changeParam = new ChangeParam();

    // ---------------- 校验和 ----------------
    int calculatedChecksum = 0;
    for (int i = 0; i < recvBytes.length - 1; i++) {
        calculatedChecksum += recvBytes[i];
    }
    calculatedChecksum &= 0xFF;
    int receivedChecksum = recvBytes[recvBytes.length - 1] & 0xFF;

    if (calculatedChecksum != receivedChecksum) {
        System.err.println("parseNullReport: 校验和错误, calc=" + calculatedChecksum + ", recv=" + receivedChecksum);
        return null;
    }

    // ---------------- 解析 TLV ----------------
    int tlvIndex = 8; // TLV 从第 8 个字节开始
    int checksumIndex = recvBytes.length - 1;

    while (tlvIndex + 4 <= checksumIndex) {
        // 剩余长度不足 T+L，跳出
        if (tlvIndex + 4 > recvBytes.length) break;

        // 提取 T (2字节)
        int typeKey = ((recvBytes[tlvIndex] & 0xFF) << 8) | (recvBytes[tlvIndex + 1] & 0xFF);

        // 提取 L (2字节，大端)
        int lHigh = recvBytes[tlvIndex + 2] & 0xFF;
        int lLow = recvBytes[tlvIndex + 3] & 0xFF;
        int vLen = (lHigh << 8) | lLow;

        if (vLen < 0 || tlvIndex + 4 + vLen > checksumIndex) {
            System.err.println("parseNullReport: TLV 长度超界, vLen=" + vLen);
            break;
        }

        // 提取 V
        byte[] vBytes = Arrays.copyOfRange(recvBytes, tlvIndex + 4, tlvIndex + 4 + vLen);

        // 解析数据
        if (typeKey == 0x0A02) {
            // 进入连续报文模式
            inContinuous = true;
            lastContinuousTime = System.currentTimeMillis();

            int x = ((vBytes[0] & 0xFF) << 8) | (vBytes[1] & 0xFF);
            int y = ((vBytes[0] & 0xFF) << 16) | ((vBytes[1] & 0xFF) << 8) | (vBytes[2] & 0xFF);

            int s = ((vBytes[1] & 0xFF) << 16) | ((vBytes[2] & 0xFF) << 8) | (vBytes[3] & 0xFF);
            int d = ((vBytes[0] & 0xFF) << 16) | ((vBytes[2] & 0xFF) << 8) | (vBytes[1] & 0xFF);
            int r = ((vBytes[4] & 0xFF) << 16) | ((vBytes[3] & 0xFF) << 8) | (vBytes[2] & 0xFF);

            double result_x = x / 50.0;
            double result_y = y / 8388607.0 * 100;

            changeParam.setWl(String.valueOf(result_x));
            changeParam.setS(String.valueOf(s));
            changeParam.setD(String.valueOf(d));
            changeParam.setR(String.valueOf(r));

            Constants.measurd_data.add(changeParam);
            Constants.instrumentData.add(changeParam.toString());
            System.out.println("已完成一个完整的对象：" + changeParam);
        }

        // 移动到下一个 TLV
        tlvIndex += 4 + vLen;
    }

    // 如果没解析到有效数据，返回 null
    if (changeParam.getWl() == null) {
        return null;
    }

    System.out.println(changeParam);
    return changeParam;
}



    // 定义两个静态 List，存放所有结果
    private static List<Double> xList = new ArrayList<>();
    private static List<Double> yList = new ArrayList<>();

    public static String parseScan(byte[] vBytes) {
        ChangeParam changeParam = new ChangeParam();
        // 每次解析前清空旧数据（如果你需要累积历史数据，可以去掉这两行）

        if (vBytes != null && vBytes.length >= 3) {
            int x = ((vBytes[0] & 0xFF) << 8) | (vBytes[1] & 0xFF);
            int y = ((vBytes[0] & 0xFF) << 16) | ((vBytes[1] & 0xFF) << 8) | (vBytes[2] & 0xFF);

            int s = ((vBytes[1] & 0xFF) << 16) | ((vBytes[2] & 0xFF) << 8) | (vBytes[3] & 0xFF);
            int d = ((vBytes[2] & 0xFF) << 16) | ((vBytes[3] & 0xFF) << 8) | (vBytes[4] & 0xFF);
            int r = ((vBytes[4] & 0xFF) << 16) | ((vBytes[5] & 0xFF) << 8) | (vBytes[6] & 0xFF);

            double result_x = x / 50.0;
            double result_y = y / 8388607.0 * 100;

            changeParam.setWl(String.valueOf(result_x));
            changeParam.setS(String.valueOf(s));
            changeParam.setD(String.valueOf(d));
            changeParam.setR(String.valueOf(r));

            String json = JSON.toJSONString(changeParam);
        }
        return JSON.toJSONString(changeParam);
    }


//    public static String parseSn(byte[] vBytes) {
//        StringBuilder snBuilder = new StringBuilder();
//
//        // 取完整的序列号，不固定16
//        for (int i = 0; i < vBytes.length; i++) {
//            snBuilder.append((char) (vBytes[i] & 0xFF));
//        }
//
////        negotiationInfo.put("Sn", snBuilder);
//        Result = snBuilder.toString();
//        return snBuilder.toString();
//    }

    public static String parseSn(byte[] vBytes) {
        StringBuilder snBuilder = new StringBuilder();
        for (byte b : vBytes) snBuilder.append((char) (b & 0xFF));
        Result = snBuilder.toString();
        return snBuilder.toString();
    }

    public static boolean parseConnect(byte[] vBytes) {
        if (vBytes[0] == 0){
            System.out.println("连接成功");
            Result  = "true";
            return true;
        }
        Result  = "false";
        return false;
    }


//    private static Object parseNegotiation(byte[] vBytes) {
//        if (vBytes == null || vBytes.length < 5) {
//            return "V长度不足";
//        }
//
//        // 解析前两字节之和
//        int sum = (vBytes[0] & 0xFF) + (vBytes[1] & 0xFF);
//        Object sumResult;
//        switch (sum) {
//            case 16:
//                sumResult = "PeerSee";
//                break;
//            default:
//                sumResult = "未获取到厂家信息";
//                break;
//        }
//
//        // 解析第三字节高4位
//        int highNibble = (vBytes[2] & 0xF0) >> 4;
//        Object typeResult;
//        switch (highNibble) {
//            case 1:
//                typeResult = "UV";
//                break;
//            case 2:
//                typeResult = "iNexus";
//                break;
//            default:
//                typeResult = "未知类型";
//                break;
//        }
//
//        // 解析第四字节
//        byte fourthByte = vBytes[3];
//        Object modelResult;
//        switch (fourthByte) {
//            case 1:
//                modelResult = "T800";
//                break;
//            case 0:
//                modelResult = "T600";
//                break;
//            default:
//                modelResult = "未知型号";
//                break;
//        }
//
//        // 解析第五字节，直接返回 v + 数值
//        int fifthByte = vBytes[4] & 0xFF; // 转成无符号
//        Object versionResult = "v" + fifthByte;
//
//        //下位机的MAC地址
//        StringBuilder macBuilder = new StringBuilder();
//        for (int i = 8; i <= 13; i++) {
//            macBuilder.append(String.format("%02X", vBytes[i]));
//            if (i < 13) {
//                macBuilder.append(":");
//            }
//        }
//        String mac = macBuilder.toString();
//
//        System.out.println("下位机的MAC地址: " + mac);
//        // 解析端口号（14、15位）
//        int port = ((vBytes[13] & 0xFF) << 8) | (vBytes[14] & 0xFF);
//
//        System.out.println("下位机的端口号: " + port);
//
//        // IP 地址：第 16~19 字节
//        String ip = String.format("%d.%d.%d.%d",
//                vBytes[15] & 0xFF, vBytes[16] & 0xFF,
//                vBytes[17] & 0xFF, vBytes[18] & 0xFF);
//
//        System.out.println("下位机的IP地址: " + ip);
//
//        // 存入全局 Map
//        negotiationInfo.put("manufacturer", sumResult);//厂家
//        negotiationInfo.put("type", typeResult);// 软件类型
//        negotiationInfo.put("model", modelResult);// 型号
//        negotiationInfo.put("version", versionResult);// 版本
//        negotiationInfo.put("mac", mac);// MAC
//        negotiationInfo.put("port", port);// 端口
//        negotiationInfo.put("ip", ip);// IP
//        // 返回组合结果
//        return String.format("%s, %s, %s, %s", sumResult, typeResult, modelResult, versionResult);
//    }

    public static Map<String, Object> parseNegotiation(byte[] vBytes) {
        Map<String, Object> info = new HashMap<>();
        if (vBytes == null || vBytes.length < 5) {
            info.put("error", "V长度不足");
            return info;
        }

        int sum = (vBytes[0] & 0xFF) + (vBytes[1] & 0xFF);
        Object sumResult = (sum == 16) ? "PeerSee" : "未获取到厂家信息";

        int highNibble = (vBytes[2] & 0xF0) >> 4;
        String typeResult;
        switch (highNibble) {
            case 1:
                typeResult = "UV";
                break;
            case 2:
                typeResult = "iNexus";
                break;
            default:
                typeResult = "未知类型";
                break;
        };

        String modelResult;
        switch (vBytes[3]) {
            case 1:
                modelResult = "T800";
                break;
            case 0:
                modelResult = "T600";
                break;
            default:
                modelResult = "未知型号";
                break;
        };

        Object versionResult = "v" + (vBytes[4] & 0xFF);

        StringBuilder macBuilder = new StringBuilder();
        for (int i = 8; i <= 13; i++) {
            macBuilder.append(String.format("%02X", vBytes[i]));
            if (i < 13) macBuilder.append(":");
        }
        String mac = macBuilder.toString();

        int port = ((vBytes[13] & 0xFF) << 8) | (vBytes[14] & 0xFF);
        String ip = String.format("%d.%d.%d.%d",
                vBytes[15] & 0xFF, vBytes[16] & 0xFF,
                vBytes[17] & 0xFF, vBytes[18] & 0xFF);

        info.put("manufacturer", sumResult);
        info.put("type", typeResult);
        info.put("model", modelResult);
        info.put("version", versionResult);
        info.put("mac", mac);
        info.put("port", port);
        info.put("ip", ip);

        return info;
    }

    /**
     * 比较 V
     */
    private static boolean compareVBytes(byte[] a, byte[] b){
        if (a.length != b.length){
            Result = "false";
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                Result = "false";
                return false;}
        }
        Result = "true";
        return true;
    }

    /**
     * 比较 T：发送T的低字节 == 接收T的低字节
     */
    private static boolean compareTBytes(byte[] recvT, byte[] sendT){
        if (recvT.length < 2 || sendT.length < 2) return false;

        int recvTLow = recvT[1] & 0xFF;
        int sendTLow = sendT[1] & 0xFF;

        return sendTLow == recvTLow;
    }


    /**
     * 提取发送报文的 V 部分
     */
    private static List<byte[]> extractSendTLVVs(byte[] sendBytes){
        List<byte[]> vList = new ArrayList<>();
        // 如果 sendBytes 为 null，直接返回空列表
        if (sendBytes == null || sendBytes.length == 0) {
            return vList;
        }

        int index = 8;

        while (index + 4 <= sendBytes.length - 1) {
            int lHigh = sendBytes[index + 2] & 0xFF;
            int lLow = sendBytes[index + 3] & 0xFF;
            int vLen = (lHigh << 8) | lLow;

            if (index + 4 + vLen > sendBytes.length - 1) break;

            byte[] vBytes = new byte[vLen];
            System.arraycopy(sendBytes, index + 4, vBytes, 0, vLen);
            vList.add(vBytes);

            index += 4 + vLen;
        }
        return vList;
    }

    /**
     * 提取发送报文的 T 部分
     */
    private static List<byte[]> extractSendTLVTs(byte[] sendBytes) {
        List<byte[]> tList = new ArrayList<>();
        if (sendBytes == null || sendBytes.length == 0) {
            return tList;
        }

        int index = 8;
        while (index + 4 <= sendBytes.length - 1) {
            int lHigh = sendBytes[index + 2] & 0xFF;
            int lLow = sendBytes[index + 3] & 0xFF;
            int vLen = (lHigh << 8) | lLow;

            if (index + 4 > sendBytes.length - 1) break;

            byte[] tBytes = new byte[2];
            System.arraycopy(sendBytes, index, tBytes, 0, 2);
            tList.add(tBytes);

            index += 4 + vLen;
        }
        return tList;
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

}
