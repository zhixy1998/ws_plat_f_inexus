/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import javax.annotation.PreDestroy;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

/**
 * 接收线程，负责接收UDP数据，并存放到队列里
 */
public class UdpReceiveThread {

    private final MessageStoreManager store = MessageStoreManager.getInstance();

    private Thread ethernetReceiverThread;
    private Thread wifiReceiverThread;

    private volatile boolean ethernetRunning = false;

    private volatile boolean wifiRunning = false;

    // 安全配置
    private static final int MAX_PACKET_SIZE = 1024; // 4KB

    private static volatile UdpReceiveThread instance;

    private final static ConnectUtil connectUtil = ConnectUtil.getInstance();

    // 私有构造函数，防止外部创建实例
    private UdpReceiveThread() {
        // 初始化代码
    }

    // 获取单例实例
    public static UdpReceiveThread getInstance() {
        if (instance == null) {
            synchronized (UdpReceiveThread.class) {
                if (instance == null) {
                    instance = new UdpReceiveThread();
                }
            }
        }
        return instance;
    }


    public void networkReceptionStart(int port, int connectType) {

        if (DeviceConnectTypeEnum.WIFI.getConnectType().equals(connectType)) {


            if (wifiRunning) return;

            wifiRunning= true;
            wifiReceiverThread = new Thread(() -> {
                try {

                    DatagramSocket socket = connectUtil.getWifiSocket(port);

                    System.out.println("wifi接收线程启动，端口: " + port);

                    DeviceConnectionManager deviceManager = DeviceConnectionManager.getInstance();

                    byte[] buffer = new byte[MAX_PACKET_SIZE];

                    while (wifiRunning) {
                        try {
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                            socket.receive(packet);

                            // 安全检查
                            if (packet.getLength() <= 0 || packet.getLength() > MAX_PACKET_SIZE) {
                                System.err.println("丢弃无效数据包，大小: " + packet.getLength());
                                continue;
                            }

                            // 处理数据
                            String ip = packet.getAddress().getHostAddress();
                            System.out.println("接收数据，来自: " + ip);

                            String deviceId = deviceManager.getOrCreateDeviceId(ip, port, connectType);

                            byte[] data = Arrays.copyOfRange(
                                    packet.getData(),
                                    packet.getOffset(),
                                    packet.getOffset() + packet.getLength()
                            );

                            store.addMessage(deviceId, data);

                        } catch (SocketException e) {
                            // Socket已关闭，这是正常关闭流程

                            if (ethernetRunning) {
                                // 如果不是我们主动关闭的，才打印错误
                                System.err.println("以太网Socket异常关闭: " + e.getMessage());
                            }
                            if (socket.isClosed()) {
                                return;
                            }
                        } catch (Exception e) {
                            if (wifiRunning) {
                                System.out.println(socket.getSoTimeout());
                                System.err.println("接收异常: " + e.getMessage());
                            } else {
                                return;
                            }
                        }
                    }

                    socket.close();
                    System.out.println("wifi接收线程停止");

                } catch (Exception e) {
                    System.err.println("启动失败: " + e.getMessage());
                    wifiRunning = false;
                }
            });

            wifiReceiverThread.start();


        } else {
            if (ethernetRunning) return;

            ethernetRunning = true;
            ethernetReceiverThread = new Thread(() -> {
                try {
                    DatagramSocket socket = connectUtil.getEtherSocket(port);

                    DeviceConnectionManager deviceManager = DeviceConnectionManager.getInstance();

                    System.out.println("UDP接收线程启动，端口: " + port);

                    byte[] buffer = new byte[MAX_PACKET_SIZE];

                    while (ethernetRunning) {
                        try {
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                            socket.receive(packet);


                            // 安全检查
                            if (packet.getLength() <= 0 || packet.getLength() > MAX_PACKET_SIZE) {
                                System.err.println("丢弃无效数据包，大小: " + packet.getLength());
                                continue;
                            }

                            // 处理数据
                            String ip = packet.getAddress().getHostAddress();

                            String deviceId = deviceManager.getOrCreateDeviceId(ip, port, connectType);
                            byte[] data = Arrays.copyOfRange(
                                    packet.getData(),
                                    packet.getOffset(),
                                    packet.getOffset() + packet.getLength()
                            );
                            System.out.println("接收数据，来自: " + ip);
                            System.out.println("接收数据：" + Arrays.toString(data));

                            store.addMessage(deviceId, data);

                        } catch (SocketException e) {
                            // Socket已关闭，这是正常关闭流程

                            if (ethernetRunning) {
                                // 如果不是我们主动关闭的，才打印错误
                                System.err.println("以太网Socket异常关闭: " + e.getMessage());
                            }
                            if (socket.isClosed()) {
                                return;
                            }
                        } catch (Exception e) {
                            if (ethernetRunning) {
                                System.err.println("接收异常: " + e.getMessage());
                            } else {
                                return;
                            }

                        }
                    }

                    socket.close();
                    System.out.println("UDP接收线程停止");

                }catch (Exception e) {
                    System.err.println("启动失败: " + e.getMessage());
                    ethernetRunning = false;
                }
            });

            ethernetReceiverThread.start();
        }
    }

    public Boolean isEthernetRunning() {
        return ethernetRunning;
    }

    public Boolean isWifiRunning() {
        return wifiRunning;
    }

    public Boolean isRunning(Integer connectType) {
        if (connectType.equals(DeviceConnectTypeEnum.WIFI.getConnectType())) {
            return wifiRunning;
        } else {
            return ethernetRunning;
        }
    }


//    @PreDestroy
//    public void stopEthernetReceive() {
//
//        if (ethernetReceiverThread != null) {
//            ethernetReceiverThread.interrupt();
//            ethernetRunning = false;
//        }
//    }
//
//    @PreDestroy
//    public void stopWifiReceive() {
//
//        if (wifiReceiverThread != null) {
//            wifiReceiverThread.interrupt();
//            wifiRunning = false;
//        }
//    }




    /**
     * 停止以太网接收线程（优雅关闭）
     */
    public void stopEthernetReceive() {
        if (ethernetRunning) {
            System.out.println("正在停止以太网接收线程...");
            ethernetRunning = false;

            // 先关闭Socket，使receive()方法返回
            connectUtil.closeEthernetSocket();

            // 给线程一点时间正常退出
            if (ethernetReceiverThread != null && ethernetReceiverThread.isAlive()) {
                try {
                    ethernetReceiverThread.join(2000); // 等待2秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // 如果线程还在运行，强制中断
                if (ethernetReceiverThread.isAlive()) {
                    ethernetReceiverThread.interrupt();
                }
            }

            ethernetReceiverThread = null;
            System.out.println("以太网接收线程已停止");
        }
    }



    /**
     * 停止WiFi接收线程（优雅关闭）
     */
    public void stopWifiReceive() {
        if (wifiRunning) {
            System.out.println("正在停止WiFi接收线程...");
            wifiRunning = false;

            // 先关闭Socket，使receive()方法返回
            connectUtil.closeWifiSocket();

            // 给线程一点时间正常退出
            if (wifiReceiverThread != null && wifiReceiverThread.isAlive()) {
                try {
                    wifiReceiverThread.join(2000); // 等待2秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // 如果线程还在运行，强制中断
                if (wifiReceiverThread.isAlive()) {
                    wifiReceiverThread.interrupt();
                }
            }

            wifiReceiverThread = null;
            System.out.println("WiFi接收线程已停止");
        }
    }

    /**
     * 停止所有接收线程
     */
    @PreDestroy
    public void stopAll() {
        System.out.println("正在停止所有UDP接收线程...");
        stopEthernetReceive();
        stopWifiReceive();
        System.out.println("所有UDP接收线程已停止");
    }



}
