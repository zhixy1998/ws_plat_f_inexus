/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.*;
import java.util.Enumeration;

/**
 * UDP Socket管理工具类 - 带端口占用检测
 */

@Component
public class ConnectUtil {
    // 单例实例
    private static ConnectUtil instance;

    // Socket实例
    private DatagramSocket ethernetSocket;
    private DatagramSocket wifiSocket;

    // 状态信息
    private String ethernetLocalIp;

    private String wifiLocalIp;

    private int ethernetPort = -1;

    private int wifiPort = -1;

    /**
     * 获取单例实例
     */
    public static synchronized ConnectUtil getInstance() {
        if (instance == null) {
            instance = new ConnectUtil();
        }
        return instance;
    }

    // 私有构造方法
    private ConnectUtil() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::closeAllSockets));
    }

    /**
     * 网络连接类型枚举
     */
    public enum ConnectionType {
        ETHERNET,
        WIFI
    }


    /**
     * 获取以太网Socket（如果端口被占用会自动查找空闲端口）
     */
    public synchronized DatagramSocket getEtherSocket(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("端口号无效: " + port);
        }

        // 如果Socket已存在且可用，直接返回
        if (isSocketAvailable()) {
            return ethernetSocket;
        }

        // 关闭旧的Socket
        closeEthernetSocket();

        // 创建新Socket（带端口占用检测）
        return createEthernetSocket(port);
    }

    public synchronized DatagramSocket getWifiSocket(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("端口号无效: " + port);
        }

        // 如果Socket已存在且可用，直接返回
        if (isWifiSocketAvailable()) {
            return wifiSocket;
        }

        // 关闭旧的Socket
        closeWifiSocket();

        // 创建新Socket
        return createWifiSocket(port);
    }

    /**
     * 获取socket
     */
    public synchronized DatagramSocket getSocket(Integer connectionType, Integer port) {

        if (connectionType.equals(DeviceConnectTypeEnum.WIFI.getConnectType())) {
            return getWifiSocket(port);
        } else {
            return getEtherSocket(port);
        }
    }

    /**
     * 查询socket端口
     */
    public synchronized Integer getSocketPort(Integer connectionType) {

        if (connectionType.equals(DeviceConnectTypeEnum.WIFI.getConnectType())) {
            return wifiPort;
        } else {

            return ethernetPort;
        }
    }


    /**
     * 创建以太网Socket
     */
    private DatagramSocket createEthernetSocket(int port) {
        try {
            // 获取以太网IP
            NetworkInterfaceInfo ethInfo = findEthernetInterface();
            if (ethInfo == null || ethInfo.ip == null) {
                System.out.println("未找到可用的以太网接口");
                return null;
            }

            ethernetLocalIp = ethInfo.ip;
            ethernetPort = port;

            InetAddress addr = InetAddress.getByName(ethernetLocalIp);

            // 查找可用端口
            int actualPort = findAvailablePortForInterface(port, addr);
            ethernetSocket = new DatagramSocket(actualPort, addr);

            // 配置Socket
            configureSocket(ethernetSocket);

            System.out.println("以太网Socket创建成功: " + ethInfo.name +
                    " IP: " + ethernetLocalIp + ":" + actualPort);

            return ethernetSocket;

        } catch (Exception e) {
            System.err.println("创建以太网Socket失败: " + e.getMessage());
            resetEthernetState();
            return null;
        }
    }

    /**
     * 创建WiFi Socket
     */
    private DatagramSocket createWifiSocket(int port) {
        try {
            // 获取WiFi IP
            NetworkInterfaceInfo wifiInfo = findWifiInterface();
            if (wifiInfo == null || wifiInfo.ip == null) {
                System.out.println("未找到可用的WiFi接口");
                return null;
            }

            wifiLocalIp = wifiInfo.ip;
            wifiPort = port;

            InetAddress addr = InetAddress.getByName(wifiLocalIp);

            // 查找可用端口
            int actualPort = findAvailablePortForInterface(port, addr);
            wifiSocket = new DatagramSocket(actualPort, addr);

            // 配置Socket
            configureSocket(wifiSocket);

            System.out.println("WiFi Socket创建成功: " + wifiInfo.name +
                    " IP: " + wifiLocalIp + ":" + actualPort);

            return wifiSocket;

        } catch (Exception e) {
            System.err.println("创建WiFi Socket失败: " + e.getMessage());
            resetWifiState();
            return null;
        }
    }

    /**
     * 查找可用端口（考虑接口特定IP）
     */
    private int findAvailablePortForInterface(int startPort, InetAddress interfaceAddr) {
        int port = startPort;
        int maxAttempts = 100;
        int attempts = 0;

        while (attempts < maxAttempts) {
            try (DatagramSocket testSocket = new DatagramSocket(port, interfaceAddr)) {
                // 测试端口是否可用
                return port;
            } catch (BindException e) {
                // 端口被占用，尝试下一个
                port++;
                attempts++;

                if (port > 65535) {
                    port = 1024; // 重新开始
                }

                if (attempts >= maxAttempts) {
                    throw new RuntimeException("尝试了 " + maxAttempts + " 个端口，都无法绑定");
                }
            } catch (Exception e) {
                throw new RuntimeException("端口检测失败: " + e.getMessage(), e);
            }
        }

        return port;
    }

    /**
     * 网络接口信息封装类
     */
    private static class NetworkInterfaceInfo {
        String name;
        String displayName;
        String ip;
        NetworkInterface networkInterface;

        NetworkInterfaceInfo(String name, String displayName, String ip, NetworkInterface ni) {
            this.name = name;
            this.displayName = displayName;
            this.ip = ip;
            this.networkInterface = ni;
        }
    }

    /**
     * 查找以太网接口
     */
    private NetworkInterfaceInfo findEthernetInterface() {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();

                // 过滤无效接口
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) {
                    continue;
                }

                String name = ni.getName().toLowerCase();
                String displayName = ni.getDisplayName().toLowerCase();

                // 判断是否为以太网接口
                if (isEthernetInterface(name, displayName, os)) {
                    // 获取IPv4地址
                    String ip = getEthernetIPv4();
                    if (ip != null) {
                        return new NetworkInterfaceInfo(name, displayName, ip, ni);
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("查找以太网接口失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 查找WiFi接口
     */
    private NetworkInterfaceInfo findWifiInterface() {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();

                // 过滤无效接口
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) {
                    continue;
                }

                String name = ni.getName().toLowerCase();
                String displayName = ni.getDisplayName().toLowerCase();

                // 判断是否为WiFi接口
                if (isWifiInterface(name, displayName, os)) {
                    // 获取IPv4地址
                    String ip = getWifiIPv4();
                    if (ip != null) {
                        return new NetworkInterfaceInfo(name, displayName, ip, ni);
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("查找WiFi接口失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 判断是否为以太网接口
     */
    private boolean isEthernetInterface(String name, String displayName, String os) {
        if (os.contains("linux") || os.contains("mac")) {
            // Linux/Mac系统
            return name.startsWith("eth") ||
                    name.startsWith("eno") ||
                    name.startsWith("ens") ||
                    name.startsWith("enp") ||
                    name.startsWith("enx") ||
                    name.contains("ethernet") ||
                    displayName.contains("ethernet");
        } else{
            // Windows系统
            if (displayName.contains("ethernet") ||
                    displayName.contains("以太网") ||
                    displayName.contains("adapter") ||
                    (displayName.contains("realtek") &&
                            !displayName.contains("wireless") &&
                            !displayName.contains("wlan") &&
                            !displayName.contains("wi-fi") &&
                            !displayName.contains("virtual") &&
                            !displayName.contains("vmware") &&
                            !displayName.contains("vbox"))) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断是否为WiFi接口
     */
    private boolean isWifiInterface(String name, String displayName, String os) {
        if (os.contains("win")) {
            // Windows系统
            if (displayName.contains("wireless") ||
                    displayName.contains("wi-fi") ||
                    displayName.contains("wlan") ||
                    displayName.contains("wifi")) {
                return true;
            }
        } else if (os.contains("linux") || os.contains("mac")) {
            // Linux/Mac系统
            return name.startsWith("wlan") ||
                    name.startsWith("wlp") ||
                    name.startsWith("wifi") ||
                    name.contains("wireless") ||
                    displayName.contains("wireless") ||
                    displayName.contains("wlan") ||
                    displayName.contains("wifi") ||
                    (name.startsWith("en0") && displayName.contains("airport")) || // Mac的WiFi
                    (name.startsWith("en1") && displayName.contains("airport"));
        }

        return false;
    }

    /**
     * 获取第一个IPv4地址
     */
    private String getFirstIPv4Address(NetworkInterface ni) {
        Enumeration<InetAddress> addresses = ni.getInetAddresses();
        while (addresses.hasMoreElements()) {
            InetAddress addr = addresses.nextElement();
            if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                return addr.getHostAddress();
            }
        }
        return null;
    }

    /**
     * 配置Socket
     */
    private void configureSocket(DatagramSocket socket) throws SocketException {
        // 无限阻塞，适合接收线程
//        socket.setSoTimeout(0);

        // 地址重用，允许同一个端口绑定到不同接口
        socket.setReuseAddress(true);

        // 设置缓冲区大小（根据您的报文优化）
        int bufferSize = 8 * 1024; // 8KB
        socket.setReceiveBufferSize(bufferSize);
        socket.setSendBufferSize(bufferSize);

        // 允许广播
        socket.setBroadcast(true);
    }

    /**
     * 检查以太网Socket是否可用
     */
    public synchronized boolean isEthernetSocketAvailable() {
        return ethernetSocket != null && !ethernetSocket.isClosed();
    }

    /**
     * 检查WiFi Socket是否可用
     */
    public synchronized boolean isWifiSocketAvailable() {
        return wifiSocket != null && !wifiSocket.isClosed();
    }

    /**
     * 关闭所有Socket
     */
    public synchronized void closeAllSockets() {
        closeEthernetSocket();
        closeWifiSocket();
    }

    /**
     * 关闭以太网Socket
     */
    public synchronized void closeEthernetSocket() {
        if (ethernetSocket != null && !ethernetSocket.isClosed()) {
            try {
                ethernetSocket.close();
                System.out.println("以太网Socket已关闭");
            } catch (Exception e) {
                System.err.println("关闭以太网Socket时发生异常: " + e.getMessage());
            }
        }
        resetEthernetState();
    }

    /**
     * 关闭WiFi Socket
     */
    public synchronized void closeWifiSocket() {
        if (wifiSocket != null && !wifiSocket.isClosed()) {
            try {
                wifiSocket.close();
                System.out.println("WiFi Socket已关闭");
            } catch (Exception e) {
                System.err.println("关闭WiFi Socket时发生异常: " + e.getMessage());
            }
        }
        resetWifiState();
    }

    /**
     * 重置以太网状态
     */
    private void resetEthernetState() {
        ethernetSocket = null;
        ethernetLocalIp = null;
        ethernetPort = -1;
    }

    /**
     * 重置WiFi状态
     */
    private void resetWifiState() {
        wifiSocket = null;
        wifiLocalIp = null;
        wifiPort = -1;
    }

    /**
     * 获取以太网端口
     */
    public synchronized int getEthernetPort() {
        return ethernetPort;
    }

    /**
     * 获取WiFi端口
     */
    public synchronized int getWifiPort() {
        return wifiPort;
    }


    /**
     * 获取当前状态信息
     */
    public synchronized String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ConnectUtil状态 ===\n");

        sb.append("以太网接口:\n");
        if (isEthernetSocketAvailable()) {
            sb.append(String.format("  状态: 已连接\n"));
            sb.append(String.format("  IP: %s\n", ethernetLocalIp));
            sb.append(String.format("  端口: %d\n", ethernetPort));
            try {
                sb.append(String.format("  实际绑定: %s:%d\n",
                        ethernetSocket.getLocalAddress().getHostAddress(),
                        ethernetSocket.getLocalPort()));
            } catch (Exception e) {
                sb.append("  获取绑定信息失败\n");
            }
        } else {
            sb.append("  状态: 未连接\n");
        }

        sb.append("WiFi接口:\n");
        if (isWifiSocketAvailable()) {
            sb.append(String.format("  状态: 已连接\n"));
            sb.append(String.format("  IP: %s\n", wifiLocalIp));
            sb.append(String.format("  端口: %d\n", wifiPort));
            try {
                sb.append(String.format("  实际绑定: %s:%d\n",
                        wifiSocket.getLocalAddress().getHostAddress(),
                        wifiSocket.getLocalPort()));
            } catch (Exception e) {
                sb.append("  获取绑定信息失败\n");
            }
        } else {
            sb.append("  状态: 未连接\n");
        }

        return sb.toString();
    }



    /**
     * 检查Socket是否可用
     */
    public synchronized boolean isSocketAvailable() {
        return ethernetSocket != null && !ethernetSocket.isClosed();
    }

    /**
     * 关闭Socket
     */
    public synchronized void closeSocket() {
        if (ethernetSocket != null && !ethernetSocket.isClosed()) {
            try {
                ethernetSocket.close();
            } catch (Exception e) {
                // 忽略关闭异常
            }
        }

        // 重置状态
        ethernetSocket = null;
        ethernetLocalIp = null;
        ethernetPort = -1;
    }

    /**
     * 获取当前绑定的以太网IP
     */
    public synchronized String getEthernetLocalIp() {

        if (!StringUtils.hasText(ethernetLocalIp)) {
            ethernetLocalIp = getEthernetIPv4();
        }

        return ethernetLocalIp;
    }
    /**
     * 获取当前绑定的IP
     */
    public synchronized String getWifiLocalIp() {

        if (!StringUtils.hasText(wifiLocalIp)) {
            wifiLocalIp = getWifiIPv4();
        }
        return wifiLocalIp;
    }


    /**
     * 检查端口是否可用（独立方法，可用于外部调用）
     */
    public boolean isPortAvailable(int port) {
        if (port < 0 || port > 65535) {
            return false;
        }

        try (DatagramSocket testSocket = new DatagramSocket(port)) {
            return true;
        } catch (BindException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }



    /**
     * 配置Socket以适应长期运行
     */
    private void configureForLongTerm() throws SocketException {
        // 无限阻塞，适合接收线程
        ethernetSocket.setSoTimeout(0);

        // 地址重用，避免端口占用问题
        ethernetSocket.setReuseAddress(true);

        // 设置缓冲区大小
        int bufferSize = 8 * 1024; // 8KB
        ethernetSocket.setReceiveBufferSize(bufferSize);
        ethernetSocket.setSendBufferSize(bufferSize);

        // 允许广播
        ethernetSocket.setBroadcast(true);
    }

    /**
     * 重置状态
     */
    private void resetState() {
        ethernetSocket = null;
        ethernetLocalIp = null;
        ethernetPort = -1;
    }

    /**
     * 获取以太网IP
     */
    private String getEthernetIPv4() {
        try {
            String os = System.getProperty("os.name").toLowerCase();

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;

                byte[] mac = ni.getHardwareAddress();
                if (mac == null || mac.length == 0) continue;

                String name = ni.getName().toLowerCase();
                String displayName = ni.getDisplayName().toLowerCase();

                // Windows识别
                if (os.contains("win")) {
                    boolean isWireless = displayName.contains("wireless") ||
                            displayName.contains("wi-fi") ||
                            displayName.contains("wlan") ||
                            displayName.contains("bluetooth");
                    boolean isVirtual = displayName.contains("virtual") ||
                            displayName.contains("vmware") ||
                            displayName.contains("vbox");

                    if (!isWireless && !isVirtual &&
                            (displayName.contains("ethernet") ||
                                    displayName.contains("以太网") ||
                                    (displayName.contains("adapter") ||
                                            displayName.contains("realtek")||
                                            displayName.contains("gigabit")||
                                            displayName.contains("network connection")||
                                            displayName.contains("family controller")||
                                            displayName.contains("pcie")||
                                            displayName.contains("pci")||
                                            displayName.contains("fast ethernet")||
                                            displayName.contains("usb ethernet")||
                                            displayName.contains("intel")||
                                            displayName.contains("broadcom")||
                                            displayName.contains("marvell")||
                                            displayName.contains("atheros")||
                                            displayName.contains("killer")||
                                            displayName.contains("asix")||
                                            displayName.contains("本地连接")))) {

                        Enumeration<InetAddress> addresses = ni.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            InetAddress addr = addresses.nextElement();
                            if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                                return addr.getHostAddress();
                            }
                        }
                    }
                }
                // Linux识别
                else if (os.contains("linux")) {
                    if (!name.startsWith("wlan") && !name.contains("docker") &&
                            !name.contains("veth") && !name.contains("virbr") &&
                            (name.startsWith("eth") || name.startsWith("eno") ||
                                    name.startsWith("ens") || name.startsWith("enp"))) {

                        Enumeration<InetAddress> addresses = ni.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            InetAddress addr = addresses.nextElement();
                            if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                                return addr.getHostAddress();
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("获取网络接口失败: " + e.getMessage());
        }
        return null;
    }

    //获取wifi连接的ip地址
    private String getWifiIPv4() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();

                if (!ni.isUp() || ni.isLoopback()) continue;

                String name = ni.getName().toLowerCase();
                String displayName = ni.getDisplayName().toLowerCase();

                // 保留有线以太网接口和WiFi接口（常见关键词）
                boolean isEthernetOrWiFi = name.contains("eth") ||
                        displayName.contains("ethernet") ||
                        displayName.contains("以太网") ||
                        name.contains("wlan") ||
                        displayName.contains("wi-fi") ||
                        displayName.contains("wireless");

                if (!isEthernetOrWiFi) continue;

                // 排除 VMware、VirtualBox 等虚拟网卡
                if (name.contains("vmnet") || displayName.contains("vmware")) continue;
                if (name.contains("vbox") || displayName.contains("virtualbox")) continue;
                if (displayName.contains("virtual ethernet")) continue;

                // 遍历地址，返回第一个 IPv4 地址
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }



    /**
     * 获取指定类型网络接口的MAC地址
     *
     * @param type 连接类型
     * @return 指定类型接口的MAC地址，格式为"XX:XX:XX:XX:XX:XX"，未找到返回null
     */
    private String getMacAddress(ConnectionType type) {
        if (type == null) {
            return null;
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();

                // 跳过不活跃、环回、无MAC地址的接口
                if (!ni.isUp() || ni.isLoopback() || ni.getHardwareAddress() == null) {
                    continue;
                }

                String name = ni.getName().toLowerCase();
                String displayName = ni.getDisplayName().toLowerCase();

                // 排除虚拟网卡
                if (isVirtualInterface(name, displayName)) {
                    continue;
                }

                byte[] mac = ni.getHardwareAddress();

                // 根据类型判断
                if (type == ConnectionType.ETHERNET && isEthernetInterface(name, displayName)) {
                    return formatMacAddress(mac);
                } else if (type == ConnectionType.WIFI && isWifiInterface(name, displayName)) {
                    return formatMacAddress(mac);
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 判断是否为虚拟接口
     */
    private boolean isVirtualInterface(String name, String displayName) {
        return name.contains("vmnet") || displayName.contains("vmware") ||
                name.contains("vbox") || displayName.contains("virtualbox") ||
                name.contains("docker") || displayName.contains("virtual") ||
                name.contains("hyper-v") || name.startsWith("br-") ||
                name.contains("tun") || name.contains("tap");
    }

    /**
     * 判断是否为以太网接口
     */
    private boolean isEthernetInterface(String name, String displayName) {
        // 以太网接口通常包含 eth、en、ethernet 等关键词
        // 同时排除包含 wifi、wireless 的接口
        return (name.contains("eth") || name.startsWith("en") ||
                displayName.contains("ethernet") || displayName.contains("gigabit")) &&
                !name.contains("wlan") && !displayName.contains("wireless") &&
                !name.contains("wifi") && !displayName.contains("wi-fi");
    }

    /**
     * 判断是否为WiFi接口
     */
    private boolean isWifiInterface(String name, String displayName) {
        // WiFi接口通常包含 wlan、wifi、wireless、wi-fi 等关键词
        // 同时排除包含 ethernet 的接口
        return (name.contains("wlan") || name.contains("wifi") || name.startsWith("wl") ||
                displayName.contains("wireless") || displayName.contains("wi-fi")) &&
                !displayName.contains("ethernet") && !name.contains("eth");
    }

    /**
     * 格式化MAC地址
     */
    private String formatMacAddress(byte[] macAddressBytes) {
        if (macAddressBytes == null || macAddressBytes.length == 0) {
            return null;
        }

        StringBuilder macAddress = new StringBuilder();
        for (byte b : macAddressBytes) {
            macAddress.append(String.format("%02X:", b));
        }
        // 删除最后一个冒号
        if (macAddress.length() > 0) {
            macAddress.deleteCharAt(macAddress.length() - 1);
        }
        return macAddress.toString();
    }

    /**
     * 获取本机以太网接口的MAC地址（排除虚拟网卡）
     *
     * @return 本机的以太网MAC地址，如果获取失败则返回null
     */
    public String getEthernetMacAddress() {
        return getMacAddress(ConnectionType.ETHERNET);
    }

    /**
     * 获取本机WiFi接口的MAC地址
     *
     * @return 本机的WiFi MAC地址，如果获取失败则返回null
     */
    public String getWifiMacAddress() {
        return getMacAddress(ConnectionType.WIFI);
    }

    public DatagramSocket getExistingWifiSocket() {
        return wifiSocket; // 直接返回已有 socket，不做 close/create
    }

    public DatagramSocket getExistingEtherSocket() {
        return ethernetSocket;
    }

//    public synchronized DatagramSocket getWifiSocket(int port) {
//        if (port < 0 || port > 65535) {
//            throw new IllegalArgumentException("端口号无效: " + port);
//        }
//
//        if (wifiSocket != null && !wifiSocket.isClosed() && wifiPort == port) {
//            return wifiSocket;
//        }
//
//        if (wifiSocket == null || wifiSocket.isClosed()) {
//            try {
//                wifiSocket = new DatagramSocket(port);
//            } catch (SocketException e) {
//                throw new RuntimeException("创建 WIFI socket 失败: " + e.getMessage(), e);
//            }
//            wifiPort = port;
//        } else if (wifiPort != port) {
//            throw new IllegalStateException("WIFI socket 已绑定端口 " + wifiPort + ", 不能再绑定 " + port);
//        }
//
//        return wifiSocket;
//    }
//
//    public synchronized DatagramSocket getEtherSocket(int port) {
//        if (port < 0 || port > 65535) {
//            throw new IllegalArgumentException("端口号无效: " + port);
//        }
//
//        if (ethernetSocket != null && !ethernetSocket.isClosed() && ethernetPort == port) {
//            return ethernetSocket;
//        }
//
//        if (ethernetSocket == null || ethernetSocket.isClosed()) {
//            try {
//                ethernetSocket = new DatagramSocket(port);
//            } catch (SocketException e) {
//                throw new RuntimeException("创建 Ethernet socket 失败: " + e.getMessage(), e);
//            }
//            ethernetPort = port;
//        } else if (ethernetPort != port) {
//            throw new IllegalStateException("Ethernet socket 已绑定端口 " + ethernetPort + ", 不能再绑定 " + port);
//        }
//
//        return ethernetSocket;
//    }


}
