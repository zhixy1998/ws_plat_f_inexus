/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;


public class UnknowCountMessageQueue {

    // 核心存储结构：仪器序列号 -> (指令 -> 消息队列)
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, BlockingDeque<Object>>> store;

    // 默认队列容量
    private final int queueCapacity;

    /**
     * 构造函数
     * @param queueCapacity 每个队列的容量
     */
    public UnknowCountMessageQueue(int queueCapacity) {
        this.store = new ConcurrentHashMap<>();
        this.queueCapacity = queueCapacity;
    }

    private static class Holder {
        static final UnknowCountMessageQueue INSTANCE = new UnknowCountMessageQueue();
    }
    public static UnknowCountMessageQueue getInstance() { return UnknowCountMessageQueue.Holder.INSTANCE; }


    /**
     * 构造函数（默认容量1000）
     */
    public UnknowCountMessageQueue() {
        this(1000);
    }

    // ============== 存储接口 ==============

    /**
     * 存储消息到指定IP和指令的队列
     * @param deviceSerialNumber 仪器序列号
     * @param command 指令
     * @param message 消息内容
     * @return 是否存储成功
     */
    public boolean putMessage(String deviceSerialNumber, String command, Object message) {
        if (deviceSerialNumber == null || command == null || message == null) {
            return false;
        }

        // 获取或创建IP对应的内层map
        ConcurrentHashMap<String, BlockingDeque<Object>> ipMap =
                store.computeIfAbsent(deviceSerialNumber, k -> new ConcurrentHashMap<>());

        // 获取或创建指令对应的队列
        BlockingDeque<Object> queue = ipMap.computeIfAbsent(command, k ->
                new LinkedBlockingDeque<>(queueCapacity));

        // 尝试添加消息
        return queue.offer(message);
    }

    /**
     * 存储消息（阻塞方式，队列满时等待）
     * @param deviceSerialNumber 仪器序列号
     * @param command 指令
     * @param message 消息内容
     * @throws InterruptedException 线程中断异常
     */
    public void putMessageBlocking(String deviceSerialNumber, String command, Object message)
            throws InterruptedException {
        if (deviceSerialNumber == null || command == null || message == null) {
            return;
        }

        ConcurrentHashMap<String, BlockingDeque<Object>> ipMap =
                store.computeIfAbsent(deviceSerialNumber, k -> new ConcurrentHashMap<>());

        BlockingDeque<Object> queue = ipMap.computeIfAbsent(command, k ->
                new LinkedBlockingDeque<>(queueCapacity));

        queue.put(message);
    }

    // ============== 提取接口 ==============

    /**
     * 提取消息（阻塞方式）
     * @param deviceSerialNumber 仪器序列号
     * @param command 指令
     * @return 消息内容，阻塞直到有消息
     * @throws InterruptedException 线程中断异常
     */
    public Object takeMessage(String deviceSerialNumber, String command) throws InterruptedException {
        ConcurrentHashMap<String, BlockingDeque<Object>> ipMap = store.get(deviceSerialNumber);
        if (ipMap == null) {
            return null;
        }

        BlockingDeque<Object> queue = ipMap.get(command);
        if (queue == null) {
            return null;
        }

        return queue.take();
    }

    /**
     * 提取消息（非阻塞方式）
     * @param deviceSerialNumber 仪器序列号
     * @param command 指令
     * @return 消息内容，没有消息时返回null
     */
    public Object pollMessage(String deviceSerialNumber, String command) {
        ConcurrentHashMap<String, BlockingDeque<Object>> ipMap = store.get(deviceSerialNumber);
        if (ipMap == null) {
            return null;
        }

        BlockingDeque<Object> queue = ipMap.get(command);
        if (queue == null) {
            return null;
        }

        return queue.poll();
    }

    /**
     * 提取消息（带超时）
     * @param deviceSerialNumber 仪器序列号
     * @param command 指令
     * @param timeout 超时时间（毫秒）
     * @return 消息内容，超时返回null
     * @throws InterruptedException 线程中断异常
     */
    public Object pollMessage(String deviceSerialNumber, String command, long timeout)
            throws InterruptedException {
        ConcurrentHashMap<String, BlockingDeque<Object>> ipMap = store.get(deviceSerialNumber);
        if (ipMap == null) {
            return null;
        }

        BlockingDeque<Object> queue = ipMap.get(command);
        if (queue == null) {
            return null;
        }

        return queue.poll(timeout, TimeUnit.MILLISECONDS);
    }

    // ============== 管理接口 ==============

    /**
     * 获取队列大小
     * @param deviceSerialNumber 仪器序列号
     * @param command 指令
     * @return 队列中消息数量
     */
    public int getQueueSize(String deviceSerialNumber, String command) {
        ConcurrentHashMap<String, BlockingDeque<Object>> ipMap = store.get(deviceSerialNumber);
        if (ipMap == null) {
            return 0;
        }

        BlockingDeque<Object> queue = ipMap.get(command);
        return queue != null ? queue.size() : 0;
    }

    /**
     * 清理指定IP的指令队列
     * @param deviceSerialNumber 仪器序列号
     * @param command 指令
     */
    public void clearCommand(String deviceSerialNumber, String command) {
        ConcurrentHashMap<String, BlockingDeque<Object>> ipMap = store.get(deviceSerialNumber);
        if (ipMap != null) {
            BlockingDeque<Object> queue = ipMap.remove(command);
            if (queue != null) {
                queue.clear();
            }
        }
    }

    /**
     * 清理指定IP的所有队列
     * @param deviceSerialNumber 仪器序列号
     */
    public void clearIp(String deviceSerialNumber) {
        ConcurrentHashMap<String, BlockingDeque<Object>> ipMap = store.remove(deviceSerialNumber);
        if (ipMap != null) {
            ipMap.clear();
        }
    }

    /**
     * 判断队列是否存在
     * @param deviceSerialNumber 仪器序列号
     * @param command 指令
     * @return 是否存在
     */
    public boolean hasQueue(String deviceSerialNumber, String command) {
        ConcurrentHashMap<String, BlockingDeque<Object>> ipMap = store.get(deviceSerialNumber);
        if (ipMap == null) {
            return false;
        }
        return ipMap.containsKey(command);
    }
}
