package org.example;

import net.bytebuddy.asm.Advice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ResponseParser - 解析接收到的回复报文
 * 支持：
 * 1. 多条回复报文（适用于特殊指令）
 * 2. 每条报文多个 TLV
 * 3. 对比发送报文的 T/V
 */
public class ResponseParser {

    /**
     * 解析多条回复报文
     * @param responses 接收到的报文列表
     * @param sendBytes 发送报文字节数组
     * @return 所有解析结果列表，每条报文的结果依次加入
     */
    public static List<Object> parseMultipleResponses(List<byte[]> responses, byte[] sendBytes) {
        List<Object> allResults = new ArrayList<>();

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

            // ---------------- 校验和 ----------------
            int calculatedChecksum = 0;
            for (int i = 0; i < recvBytes.length - 1; i++) calculatedChecksum += recvBytes[i];
            calculatedChecksum &= 0xFF;

            int receivedChecksum = recvBytes[recvBytes.length - 1] & 0xFF;
            if (calculatedChecksum != receivedChecksum) {
                allResults.add(String.format("[报文%d] 校验和错误", msgIndex));
                continue;
            }

            // ---------------- 解析 TLV ----------------
            int tlvIndex = 8; // TLV 从第 8 个字节开始
            int checksumIndex = recvBytes.length - 1;
            int sendIndex = 0; // 对应发送报文的下标

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

                // 对比 T/V
                boolean tMatch = compareVBytes(recvTBytes, sendTList.get(sendIndex));
                boolean vMatch = compareVBytes(vBytes, sendVList.get(sendIndex));

                int typeKey = (recvTBytes[0] & 0xFF) << 8 | (recvTBytes[1] & 0xFF);
                String typeHex = String.format("0x%04X", typeKey);

                // 特殊类型解析
                if (typeKey == 0x0232) { // Search 类型
                    allResults.add(String.format("[报文%d][TLV %d][T=%s] T匹配:%s, 解析结果:%s",
                            msgIndex, sendIndex, typeHex, tMatch, parseSearchV(vBytes)));
                }else if (typeKey == 0x14FD) {
                    allResults.add(String.format("[报文%d][TLV %d][T=%s] T匹配:%s, 解析结果:%s",
                            msgIndex, sendIndex, typeHex, tMatch, parseSearchV(vBytes)));
                }
                else {
                    allResults.add(String.format("[报文%d][TLV %d][T=%s] T匹配:%s, V匹配:%s",
                            msgIndex, sendIndex, typeHex, tMatch, vMatch));
                }

                // 移动到下一个 TLV
                tlvIndex += 4 + vLen;
                sendIndex++;
            }
        }

        return allResults;
    }

    /**
     * 比较 V
     */
    private static boolean compareVBytes(byte[] a, byte[] b){
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
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
     * 解析 Search 类型 TLV V（MAC + IP）
     */
    private static List<Object> parseSearchV(byte[] vBytes){
        if (vBytes.length < 10) return Arrays.asList("Invalid V length");

        byte[] macBytes = new byte[6];
        System.arraycopy(vBytes, 0, macBytes, 0, 6);
        String mac = bytesToHex(macBytes);

        String ip = String.format("%d.%d.%d.%d",
                vBytes[8] & 0xFF,
                vBytes[9] & 0xFF,
                vBytes[10] & 0xFF,
                vBytes[11] & 0xFF);

        String port = String.format("%d",
                (vBytes[6] & 0xFF) << 8 | (vBytes[7] & 0xFF)
                );

        List<Object> res = new ArrayList<>();
        res.add("MAC=" + mac);
        res.add("IP=" + ip);
        res.add("port=" + port);
        return res;
    }

    /**
     * 提取发送报文的 V 部分
     */
    private static List<byte[]> extractSendTLVVs(byte[] sendBytes){
        List<byte[]> vList = new ArrayList<>();
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

    //解析下位机自动上报的报文
    public static List<Object> parsePassiveReport(byte[] recvBytes) {
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
            if (typeKey == 0x13FD){
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
}
