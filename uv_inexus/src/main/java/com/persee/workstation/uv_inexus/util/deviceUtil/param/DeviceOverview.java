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

/**
 * 设备概览类
 */
@Data
public class DeviceOverview {
    public int totalDevices;
    public int connectedDevices;
    public int temporaryDevices;
    public int permanentDevices;
    public long totalMessages;

    @Override
    public String toString() {
        return String.format(
                "设备概览: 总计=%d, 连接中=%d, 临时=%d, 永久=%d, 消息总数=%d",
                totalDevices, connectedDevices, temporaryDevices, permanentDevices, totalMessages
        );
    }
}