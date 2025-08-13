package org.example;

import org.apache.tomcat.util.net.TLSClientHelloExtractor;

import java.util.List;

public class ResponseParser {

    public static String result = "false";

    public static Object parse(List<byte[]> responses, byte[] sendBytes){

        if (responses == null || responses.isEmpty()) {
            System.out.println("无回复报文");
        }

        // 获取最后一条回应报文
        byte[] lastResponse = responses.get(responses.size() - 1);

        int messageLength = ((lastResponse[6] & 0xFF) << 8) | (lastResponse[7] & 0xFF);
        if (messageLength > lastResponse.length){
            System.out.println("消息长度不正确");
        }

        int checksumIndex = messageLength - 1;

        int tlvIndex = 8;//TLV从第8个字节开始
        while (tlvIndex + 4 <= checksumIndex){
            int tHigh = lastResponse[tlvIndex] & 0xFF;
            int tLow = lastResponse[tlvIndex + 1] & 0xFF;

            int lHigh = lastResponse[tlvIndex + 2] & 0xFF;
            int lLow = lastResponse[tlvIndex + 3] & 0xFF;
            int vLen= (lHigh << 8) | lLow;

            //边界检查
            if (tlvIndex + 4 + vLen > checksumIndex){
                System.out.println("超过边界");
                break;
            }

            //根据T不同进行解析
            if(tHigh == 0 || tHigh == 10){
                return paresSetParam(responses,sendBytes);
            }else if(tHigh == 2 && tLow == 50){
                return paresSearch(responses);
            }
        }
        return responses;
    }

    private static Object paresSearch(List<byte[]> responses) {
        if (responses == null || responses.isEmpty()) {
            return "No responses received.";
        }

        // 获取最后一条回应报文
        byte[] lastResponse = responses.get(responses.size() - 1);

        // 检查报文长度，确保包含了仪器 MAC 地址和 IP 地址
        if (lastResponse.length < 43) {
            return "Invalid response length.";
        }

        // 提取仪器 MAC 地址 (假设它从字节 12 到字节 17)
        byte[] macAddressBytes = new byte[6];
        System.arraycopy(lastResponse, 12, macAddressBytes, 0, 6);
        String macAddress = bytesToHex(macAddressBytes);

        // 提取仪器 IP 地址 (假设它从字节 20 到字节 23)
        byte[] ipAddressBytes = new byte[4];
        System.arraycopy(lastResponse, 20, ipAddressBytes, 0, 4);
        String ipAddress = String.format("%d.%d.%d.%d", ipAddressBytes[0] & 0xFF, ipAddressBytes[1] & 0xFF, ipAddressBytes[2] & 0xFF, ipAddressBytes[3] & 0xFF);

        // 返回解析后的结果
        return new String[] { "MAC 地址: " + macAddress , "IP 地址: " + ipAddress };
    }

    // 将字节数组转换为十六进制字符串
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }

    private static Object paresSetParam(List<byte[]> responses, byte[] sendBytes) {
        if (responses == null || responses.isEmpty()) {
            result = "false";
            return false;
        }

        byte[] last = responses.get(responses.size() - 1);
        if(last.length < 13){
            result = "false";
            return false;
        }

        // 计算校验和
        int calculatedChecksum = 0;
        for (int i = 0; i < last.length - 1; i++) {  // 校验和不包括最后一个字节
            calculatedChecksum += last[i];
        }

        // 取低8位的和作为校验和
        calculatedChecksum = calculatedChecksum & 0xFF;

        // 获取报文中的校验和
        int receivedChecksum = last[last.length - 1];

        // 比较计算的校验和与接收到的校验和
        if (calculatedChecksum == receivedChecksum) {
            boolean isEqual = last[12] == sendBytes[12] &&
                    last[13] == sendBytes[13];
            result = isEqual ? "true" : "false";
            return result;
        } else {
            result = "false";
            return false;
        }
    }
}
