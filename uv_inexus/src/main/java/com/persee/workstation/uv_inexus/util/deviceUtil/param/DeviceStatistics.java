/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.deviceUtil.param;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统计信息类
 */
@Data
public class DeviceStatistics {
    public int totalDevices;
    public int connectedDevices;
    public int temporaryDevices;
    public int permanentDevices;
    public long totalMessages;
    public int cleanupCount;
    public LocalDateTime lastCleanupTime;
    public double averageMessagesPerDevice;

    @Override
    public String toString() {
        String lastCleanup = lastCleanupTime != null ?
                lastCleanupTime.toString().substring(11, 19) : "从未清理";

        return String.format(
                "设备总数=%d(连接=%d,临时=%d,永久=%d), 消息总数=%d, 清理次数=%d, 最后清理=%s, 平均消息=%.1f",
                totalDevices, connectedDevices, temporaryDevices, permanentDevices,
                totalMessages, cleanupCount, lastCleanup, averageMessagesPerDevice
        );
    }
}