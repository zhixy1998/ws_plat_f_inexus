package com.persee.workstation.uv_inexus.util.deviceUtil;



import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于设备ID的消息队列管理器 - 单例模式
 * 线程安全，支持高并发读写，与DeviceConnectionManager集成
 */
@Slf4j
public class MessageStoreManager {

    // 单例：静态内部类实现（线程安全、懒加载）
    private static class Holder {
        static final MessageStoreManager INSTANCE = new MessageStoreManager();
    }
    public static MessageStoreManager getInstance() { return Holder.INSTANCE; }

    // 核心数据结构
    private final ConcurrentHashMap<String, BlockingQueue<byte[]>> deviceMessageMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastActivityMap = new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<String> messageAvailableSignal = new LinkedBlockingQueue<>();

    // 配置参数（可调整）
    private final int maxQueueSize = 2000;      // 每个设备队列最大长度
    private final int maxMessageSize = 1024;    // 单条消息最大字节数
    private final long inactiveCleanupTime = 30 * 60 * 1000L; // 不活跃超时（30分钟）

    // 统计指标
    private final AtomicLong totalMessagesAdded = new AtomicLong(0);
    private final AtomicLong totalMessagesProcessed = new AtomicLong(0);
    private final AtomicLong totalMessagesDropped = new AtomicLong(0);

    // 后台任务调度器
    private ScheduledExecutorService scheduler;

    private MessageStoreManager() {
        init();
    }

    private void init() {
        // 启动后台清理任务（5分钟清理一次）
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MessageStore-Cleanup");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::cleanupInactiveDevices, 5, 5, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(this::printStatistics, 1, 1, TimeUnit.MINUTES);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    /** 添加消息（队列满时丢弃最旧消息） */
    public boolean addMessage(String deviceId, byte[] data) {
        if (data == null || data.length == 0 || data.length > maxMessageSize) {
            return false;
        }
        lastActivityMap.put(deviceId, System.currentTimeMillis());

        BlockingQueue<byte[]> queue = deviceMessageMap.computeIfAbsent(deviceId, id ->
                new ArrayBlockingQueue<>(maxQueueSize)
        );

        byte[] copy = Arrays.copyOf(data, data.length);
        if (queue.offer(copy)) {
            totalMessagesAdded.incrementAndGet();
            messageAvailableSignal.offer(deviceId);
            return true;
        }
        return handleFullQueue(deviceId, queue, copy);
    }

    /** 队列满时的处理逻辑：丢弃最旧消息 + 插入新消息 */
    private boolean handleFullQueue(String deviceId, BlockingQueue<byte[]> queue, byte[] newData) {
        queue.poll();
        if (queue.offer(newData)) {
            totalMessagesDropped.incrementAndGet();
            log.info("设备 {} 队列满，丢弃1条消息", deviceId);
            return true;
        }
        return false;
    }

    /** 获取指定设备的下一条消息（非阻塞，消费后移除） */
    public byte[] getMessage(String deviceId) {
        BlockingQueue<byte[]> q = deviceMessageMap.get(deviceId);
        if (q == null) return null;
        byte[] data = q.poll();
        if (data != null) {
            totalMessagesProcessed.incrementAndGet();
            lastActivityMap.put(deviceId, System.currentTimeMillis());
        }
        return data;
    }

    /** 阻塞式获取任意设备的消息（高效，基于信号队列） */
    public Map.Entry<String, byte[]> getAnyMessageBlocking(long timeout, TimeUnit unit)
            throws InterruptedException {
        String deviceId = messageAvailableSignal.poll(timeout, unit);
        if (deviceId == null) return null;

        BlockingQueue<byte[]> queue = deviceMessageMap.get(deviceId);
        if (queue == null) return null;

        byte[] data = queue.poll();
        if (data != null) {
            totalMessagesProcessed.incrementAndGet();
            lastActivityMap.put(deviceId, System.currentTimeMillis());
            return new AbstractMap.SimpleEntry<>(deviceId, data);
        }
        return null;
    }

    /** 查看指定设备的下一条消息（不消费，仅查看） */
    public byte[] peekMessage(String deviceId) {
        BlockingQueue<byte[]> q = deviceMessageMap.get(deviceId);
        return (q != null) ? q.peek() : null;
    }

    /**
     * 取出指定设备队列中的所有消息（清空队列，返回所有消息）
     * @param deviceId 设备ID
     * @return 包含所有消息的列表（按队列顺序），如果队列为空则返回空列表
     */
    public List<byte[]> drainAllMessages(String deviceId) {
        BlockingQueue<byte[]> queue = deviceMessageMap.get(deviceId);
        if (queue == null) {
            return Collections.emptyList();
        }

        // 创建临时列表，容量预分配（避免扩容开销）
        List<byte[]> messages = new ArrayList<>(queue.size());

        // 一次性清空队列（drainTo 是原子操作）
        queue.drainTo(messages);

        // 更新最后活动时间（表示设备在本次操作中活跃）
        lastActivityMap.put(deviceId, System.currentTimeMillis());

        // 更新总处理消息数（批量处理）
        totalMessagesProcessed.addAndGet(messages.size());

        return messages;
    }

    /** 清理长时间无活动且队列为空的设备 */
    private void cleanupInactiveDevices() {
        long cutoff = System.currentTimeMillis() - inactiveCleanupTime;
        Iterator<Map.Entry<String, BlockingQueue<byte[]>>> it = deviceMessageMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, BlockingQueue<byte[]>> entry = it.next();
            String id = entry.getKey();
            if (lastActivityMap.get(id) != null && lastActivityMap.get(id) < cutoff && entry.getValue().isEmpty()) {
                it.remove();
                lastActivityMap.remove(id);
            }
        }
    }

    /** 队列合并（安全防死锁：按字典序加锁） */
    public int transferQueueMerge(String fromId, String toId) {
        if (fromId.equals(toId)) return 0;

        String lock1 = fromId.compareTo(toId) < 0 ? fromId : toId;
        String lock2 = fromId.compareTo(toId) < 0 ? toId : fromId;

        synchronized (lock1.intern()) {
            synchronized (lock2.intern()) {
                BlockingQueue<byte[]> src = deviceMessageMap.get(fromId);
                if (src == null || src.isEmpty()) return 0;

                BlockingQueue<byte[]> dst = deviceMessageMap.computeIfAbsent(toId,
                        id -> new ArrayBlockingQueue<>(maxQueueSize)
                );

                int transferred = 0;
                while (!src.isEmpty() && dst.size() < maxQueueSize) {
                    byte[] msg = src.poll();
                    if (msg != null && dst.offer(msg)) transferred++;
                }

                if (src.isEmpty()) {
                    deviceMessageMap.remove(fromId);
                    lastActivityMap.remove(fromId);
                }

                Long srcTime = lastActivityMap.get(fromId);
                if (srcTime != null) {
                    lastActivityMap.put(toId, Math.max(srcTime, lastActivityMap.getOrDefault(toId, 0L)));
                }

                return transferred;
            }
        }
    }

    /** 优雅关闭（停止后台任务 + 清理资源） */
    public void shutdown() {
        log.info("MessageStoreManager 关闭中...");
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        deviceMessageMap.clear();
        lastActivityMap.clear();
        messageAvailableSignal.clear();
        totalMessagesAdded.set(0);
        totalMessagesProcessed.set(0);
        totalMessagesDropped.set(0);
        log.info("MessageStoreManager 已关闭");
    }

    // 辅助方法（核心功能支持）
    public int getQueueSize(String deviceId) {
        BlockingQueue<byte[]> q = deviceMessageMap.get(deviceId);
        return q != null ? q.size() : 0;
    }

    public void clearDevice(String deviceId) {
        deviceMessageMap.remove(deviceId);
        lastActivityMap.remove(deviceId);
    }

    public void clearDeviceMessages(String deviceId) {
        BlockingQueue<byte[]> q = deviceMessageMap.get(deviceId);
        if (q != null) {
            q.clear();
        }
    }

    private void printStatistics() {
        log.info("[MessageStore] 设备数量: {}, 消息总数: {}, 添加: {}, 处理: {}, 丢弃: {}",
                deviceMessageMap.size(),
                getTotalMessageCount(),
                totalMessagesAdded.get(),
                totalMessagesProcessed.get(),
                totalMessagesDropped.get()
        );
    }

    private int getTotalMessageCount() {
        int total = 0;
        for (BlockingQueue<byte[]> q : deviceMessageMap.values()) {
            total += q.size();
        }
        return total;
    }

    public byte[] takeMessageBlocking(String deviceId, long timeoutMs)
            throws InterruptedException {

        BlockingQueue<byte[]> queue = deviceMessageMap.get(deviceId);
        if (queue == null) return null;

        byte[] data = queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (data != null) {
            totalMessagesProcessed.incrementAndGet();
            lastActivityMap.put(deviceId, System.currentTimeMillis());
        }
        return data;
    }

}