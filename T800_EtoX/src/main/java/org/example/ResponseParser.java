package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResponseParser {

    /**
     * 解析响应报文，支持多 TLV，对应发送报文 V 做比对
     * @param responses 接收到的报文列表
     * @param sendBytes 发送报文字节数组
     * @return 每个 TLV 的解析结果
     */
    public static List<Object> parse(List<byte[]> responses, byte[] sendBytes){
        List<Object> results = new ArrayList<>();
        if (responses == null || responses.isEmpty()) {
            System.out.println("无回复报文");
            return results;
        }

        // 获取最后一条回应报文
        byte[] last = responses.get(responses.size() - 1);

        // 校验和
        int calculatedChecksum = 0;
        for (int i = 0; i < last.length - 1; i++) {
            calculatedChecksum += last[i];
        }
        calculatedChecksum &= 0xFF;

        int receivedChecksum = last[last.length - 1] & 0xFF;
        if (calculatedChecksum != receivedChecksum) {
            System.out.println("校验和错误");
            return results;
        }

        // 拆分发送报文的 TLV V 部分，用于一一对应
        List<byte[]> sendVList = extractSendTLVVs(sendBytes);

        int tlvIndex = 8; // TLV 从第 8 个字节开始
        int checksumIndex = last.length - 1;
        int sendVIndex = 0; // 对应发送报文的 V 下标

        while (tlvIndex + 4 <= checksumIndex && sendVIndex < sendVList.size()) {
            int tHigh = last[tlvIndex] & 0xFF;
            int tLow  = last[tlvIndex + 1] & 0xFF;

            int lHigh = last[tlvIndex + 2] & 0xFF;
            int lLow  = last[tlvIndex + 3] & 0xFF;

            int vLen = (lHigh << 8) | lLow;

            if (tlvIndex + 4 + vLen > checksumIndex) break;

            byte[] vBytes = new byte[vLen];
            System.arraycopy(last, tlvIndex + 4, vBytes, 0, vLen);

            int typeKey = (tHigh << 8) | tLow;

            switch (typeKey) {
                case 0x0A01: case 0x0A02: case 0x0A03:

                case 0x0606: case 0x0601: case 0x0607:

                case 0x0605: case 0x0602:

                    results.add(compareVWithSend(vBytes, sendVList.get(sendVIndex)));
                    break;
                case 0x0232:

                    results.add(parseSearchV(vBytes));
                    break;

                default:
                    System.out.println("未处理的 TLV 类型：" + String.format("0x%04X", typeKey));
                    break;
            }

            tlvIndex += 4 + vLen;
            sendVIndex++;
        }

        return results;
    }

    /**
     * 对比响应 V 和发送 V 是否一致
     */
    private static boolean compareVWithSend(byte[] vBytes, byte[] sendVBytes){
        if (vBytes.length != sendVBytes.length) return false;
        for (int i = 0; i < vBytes.length; i++) {
            if (vBytes[i] != sendVBytes[i]) return false;
        }
        return true;
    }

    /**
     * 解析 Search 类型 TLV V
     */
    private static List<Object> parseSearchV(byte[] vBytes){
        if (vBytes.length < 11) return Arrays.asList("Invalid V length");

        byte[] macBytes = new byte[6];
        System.arraycopy(vBytes, 0, macBytes, 0, 6);
        String mac = bytesToHex(macBytes);

        String ip = String.format("%d.%d.%d.%d",
                vBytes[6] & 0xFF,
                vBytes[7] & 0xFF,
                vBytes[8] & 0xFF,
                vBytes[9] & 0xFF);

        List<Object> res = new ArrayList<>();
        res.add(mac);
        res.add(ip);
        return res;
    }

    /**
     * 提取发送报文的 TLV V 部分
     */
    private static List<byte[]> extractSendTLVVs(byte[] sendBytes){
        List<byte[]> vList = new ArrayList<>();
        int index = 8; // TLV 从第 8 个字节开始
        while (index + 4 <= sendBytes.length - 1) {
            int lHigh = sendBytes[index + 2] & 0xFF;
            int lLow  = sendBytes[index + 3] & 0xFF;
            int vLen = (lHigh << 8) | lLow;

            if (index + 4 + vLen > sendBytes.length - 1) break;

            byte[] vBytes = new byte[vLen];
            System.arraycopy(sendBytes, index + 4, vBytes, 0, vLen);
            vList.add(vBytes);

            index += 4 + vLen;
        }
        return vList;
    }

    private static String bytesToHex(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }
}
