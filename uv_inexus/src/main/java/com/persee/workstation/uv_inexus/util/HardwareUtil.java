package com.persee.workstation.uv_inexus.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

public class HardwareUtil {
    
    /**
     * 获取机器硬件指纹
     * 注意：在实际生产环境中，可能需要收集更多硬件信息以确保唯一性
     */
    public static String getHardwareFingerprint() {
        try {
            StringBuilder sb = new StringBuilder();
            
            // 获取操作系统信息
            sb.append(System.getProperty("os.name"));
            sb.append(System.getProperty("os.arch"));
            // 不再包含易变的os.version
            
            // 获取计算机名
            sb.append(InetAddress.getLocalHost().getHostName());
            
            // 获取MAC地址
            sb.append(getMacAddress());
            
            // 计算指纹
            return calculateFingerprint(sb.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate hardware fingerprint", e);
        }
    }
    
    /**
     * 获取MAC地址
     */
    private static String getMacAddress() throws SocketException, UnknownHostException {
        InetAddress ip = InetAddress.getLocalHost();
        NetworkInterface network = NetworkInterface.getByInetAddress(ip);
        
        if (network == null) {
            // 如果无法获取网络接口，尝试遍历所有网络接口
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface net = networks.nextElement();
                if (net != null && net.getHardwareAddress() != null) {
                    network = net;
                    break;
                }
            }
        }
        
        if (network == null) {
            return "UNKNOWN_MAC";
        }
        
        byte[] mac = network.getHardwareAddress();
        if (mac == null) {
            return "UNKNOWN_MAC";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X", mac[i]));
        }
        
        return sb.toString();
    }
    
    /**
     * 计算指纹
     */
    private static String calculateFingerprint(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // 返回完整的64位指纹
            return hexString.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to calculate fingerprint", e);
        }
    }
}