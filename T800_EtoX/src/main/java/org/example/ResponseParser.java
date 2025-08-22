package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResponseParser {

    /**
     * 解析响应报文，支持多 TLV，对应发送报文的 T/V 做比对
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

        // 取最后一条回复报文（假设它是完整的）
        byte[] last = responses.get(responses.size() - 1);

        // ---------------- 校验和检查 ----------------
        int calculatedChecksum = 0;
        for (int i = 0; i < last.length - 1; i++) {
            calculatedChecksum += last[i];
        }
        calculatedChecksum &= 0xFF;  // 取低 8 位

        int receivedChecksum = last[last.length - 1] & 0xFF;
        if (calculatedChecksum != receivedChecksum) {
            System.out.println("校验和错误");
            return results;
        }

        // ---------------- 提取发送报文的 T 和 V 列表 ----------------
        List<byte[]> sendVList = extractSendTLVVs(sendBytes);
        List<byte[]> sendTList = extractSendTLVTs(sendBytes);

        // ---------------- 解析接收报文 TLV ----------------
        int tlvIndex = 8; // TLV 从第 8 个字节开始
        int checksumIndex = last.length - 1;
        int sendIndex = 0; // 同时对应发送报文的 T/V 下标

        while (tlvIndex + 4 <= checksumIndex
                && sendIndex < sendVList.size()
                && sendIndex < sendTList.size()) {

            // 读取 TLV 的 T 和 L
            int tHigh = last[tlvIndex] & 0xFF;
            int tLow  = last[tlvIndex + 1] & 0xFF;
            int lHigh = last[tlvIndex + 2] & 0xFF;
            int lLow  = last[tlvIndex + 3] & 0xFF;

            int vLen = (lHigh << 8) | lLow; // V 的长度
            if (tlvIndex + 4 + vLen > checksumIndex) break; // 防止越界

            // 提取接收的 T 和 V
            byte[] recvTBytes = new byte[2];
            System.arraycopy(last, tlvIndex, recvTBytes, 0, 2);

            byte[] vBytes = new byte[vLen];
            System.arraycopy(last, tlvIndex + 4, vBytes, 0, vLen);

            // 从发送报文获取对应的 T 和 V
            byte[] sendTBytes = sendTList.get(sendIndex);
            byte[] sendVBytes = sendVList.get(sendIndex);

            // 比较结果
            boolean tMatch = compareBytes(recvTBytes, sendTBytes);
            boolean vMatch = compareBytes(vBytes, sendVBytes);

            int typeKey = (tHigh << 8) | tLow;
            String typeHex = String.format("0x%04X", typeKey);

            // ---------------- 按不同 TLV 类型处理 ----------------
            if (typeKey == 0x0232) {
                // Search 类型需要特殊解析
                results.add(String.format("[T=%s] T匹配:%s, 解析结果:%s",
                        typeHex, tMatch, parseSearchV(vBytes)));
            } else {
                // 默认：T 和 V 对比
                results.add(String.format("[T=%s] T匹配:%s, V匹配:%s",
                        typeHex, tMatch, vMatch));
            }

            // 移动到下一个 TLV
            tlvIndex += 4 + vLen;
            sendIndex++;
        }

        return results;
    }

    /**
     * 通用字节数组对比方法
     */
    private static boolean compareBytes(byte[] a, byte[] b){
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    /**
     * 解析 Search 类型 TLV V
     * 格式：MAC(6字节) + IP(4字节)
     */
    private static List<Object> parseSearchV(byte[] vBytes){
        if (vBytes.length < 10) return Arrays.asList("Invalid V length");

        byte[] macBytes = new byte[6];
        System.arraycopy(vBytes, 0, macBytes, 0, 6);
        String mac = bytesToHex(macBytes);

        String ip = String.format("%d.%d.%d.%d",
                vBytes[6] & 0xFF,
                vBytes[7] & 0xFF,
                vBytes[8] & 0xFF,
                vBytes[9] & 0xFF);

        List<Object> res = new ArrayList<>();
        res.add("MAC=" + mac);
        res.add("IP=" + ip);
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

    /**
     * 提取发送报文的 TLV T 部分
     */
    private static List<byte[]> extractSendTLVTs(byte[] sendBytes) {
        List<byte[]> tList = new ArrayList<>();
        int index = 8; // TLV 从第 8 个字节开始
        while (index + 4 <= sendBytes.length - 1) {
            int lHigh = sendBytes[index + 2] & 0xFF;
            int lLow  = sendBytes[index + 3] & 0xFF;
            int vLen = (lHigh << 8) | lLow;

            if (index + 4 > sendBytes.length - 1) break;

            byte[] tBytes = new byte[2]; // T 固定2字节
            System.arraycopy(sendBytes, index, tBytes, 0, 2);
            tList.add(tBytes);

            index += 4 + vLen;
        }
        return tList;
    }

    /**
     * 将字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }
}
