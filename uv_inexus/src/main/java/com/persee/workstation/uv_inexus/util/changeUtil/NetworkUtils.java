package com.persee.workstation.uv_inexus.util.changeUtil;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkUtils {
    /**
     * 获取本机以太网接口的 MAC 地址（排除虚拟网卡）
     *
     * @return 本机的以太网 MAC 地址，如果获取失败则返回 null
     */
    public static String getEthernetMacAddress() {
        try {
            // 获取所有网络接口
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // 排除不活跃的接口以及环回接口（lo）
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {

                    String name = networkInterface.getName().toLowerCase();
                    String displayName = networkInterface.getDisplayName().toLowerCase();

                    // 排除虚拟网卡接口（例如 vmnet、vbox、wlan 等）
                    if (name.contains("vmnet") || displayName.contains("vmware") ||
                            name.contains("vbox") || displayName.contains("virtualbox") ||
                            name.contains("wlan") || displayName.contains("wireless")) {
                        continue;
                    }

                    // 只获取以太网接口（例如 eth、ethernet）
                    if (name.contains("eth") || displayName.contains("ethernet")) {
                        byte[] macAddressBytes = networkInterface.getHardwareAddress();
                        if (macAddressBytes != null) {
                            StringBuilder macAddress = new StringBuilder();
                            for (byte b : macAddressBytes) {
                                // 将字节转换为十六进制并拼接
                                macAddress.append(String.format("%02X:", b));
                            }
                            // 删除最后一个冒号
                            macAddress.deleteCharAt(macAddress.length() - 1);
                            return macAddress.toString();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;  // 如果未找到以太网 MAC 地址，返回 null
    }

//    public static void main(String[] args) {
//        String macAddress = getEthernetMacAddress();
//        if (macAddress != null) {
//            System.out.println("本机的以太网 MAC 地址是: " + macAddress);
//        } else {
//            System.out.println("无法获取本机的以太网 MAC 地址");
//        }
//    }
}
