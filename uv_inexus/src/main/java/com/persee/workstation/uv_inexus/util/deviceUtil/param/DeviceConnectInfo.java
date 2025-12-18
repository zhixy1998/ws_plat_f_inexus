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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class DeviceConnectInfo {

    /**
     * 唯一标识，uuid生成，临时标识加前缀--temp
     */
    private String id;

    /**
     * 仪器ip或串口
     */
    private String connectIdentity;

    /**
     * 端口号
     */
    private Integer port;

    /**
     * 连接方式 0 串口 1 网口 2 wifi
     */
    private Integer connectType;

    /**
     * 仪器mac信息
     */
    private String macInfo;

    /**
     * 仪器xml类型
     */
    private String xmlKey;

    /**
     * 仪器连接状态
     */
    private Boolean connectStatus;

    /**
     * 仪器连接时间
     */
    private LocalDateTime connectTime;

    /**
     * 仪器最后使用时间
     */
    private LocalDateTime lastUseTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 消息计数
     */
    private AtomicInteger messageCount;

    // 缓存 identityKey 用于快速查找
    private String identityKey;

    public DeviceConnectInfo() {
        this.messageCount = new AtomicInteger(0);
        this.createTime = LocalDateTime.now();
        this.connectStatus = false;
        this.identityKey = null; // only used in full constructor
    }

    public DeviceConnectInfo(String connectIdentity, Integer port, Integer connectType) {
        this();
        this.connectIdentity = connectIdentity;
        this.port = port;
        this.identityKey = connectIdentity + ":" + port;
        this.connectType = connectType;
        generateTemporaryId();
    }

    private void generateTemporaryId() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        this.id = "temp--" + uuid;
    }

    public boolean isTemporary() {
        return id != null && id.startsWith("temp--");
    }

    public synchronized void recordMessage() {
        this.lastUseTime = LocalDateTime.now();
        this.messageCount.incrementAndGet();
    }

    public synchronized void updateLastUseTime() {
        this.lastUseTime = LocalDateTime.now();
    }

    public synchronized void setConnectStatus(Boolean connectStatus) {
        this.connectStatus = connectStatus;
        if (Boolean.TRUE.equals(connectStatus) && this.connectTime == null) {
            this.connectTime = LocalDateTime.now();
        }
    }

    public synchronized Boolean getConnectStatus() {
        return connectStatus;
    }

    public synchronized LocalDateTime getLastUseTime() {
        return lastUseTime;
    }

    public synchronized int getMessageCount() {
        return messageCount.get();
    }


    public void convertToPermanent() {
        if (isTemporary()) {
            String uuid = UUID.randomUUID().toString().replace("-", "");
            this.id = uuid;
            this.connectStatus = true;
            if (this.connectTime == null) {
                this.connectTime = LocalDateTime.now();
            }
        }
    }

    public boolean isExpired(int minutes) {
        if (lastUseTime == null) return false;
        return java.time.Duration.between(lastUseTime, LocalDateTime.now()).toMinutes() > minutes;
    }

    public DeviceConnectInfo createCopy() {
        DeviceConnectInfo copy = new DeviceConnectInfo();
        copy.id = this.id;
        copy.connectIdentity = this.connectIdentity;
        copy.port = this.port;
        copy.connectType = this.connectType;
        copy.macInfo = this.macInfo;
        copy.xmlKey = this.xmlKey;
        copy.connectStatus = this.connectStatus;
        copy.connectTime = this.connectTime;
        copy.lastUseTime = this.lastUseTime;
        copy.createTime = this.createTime;
        copy.messageCount = new AtomicInteger(this.messageCount.get());
        copy.identityKey = this.identityKey; // safe to copy
        return copy;
    }

    @Override
    public String toString() {
        return String.format(
                "DeviceConnectInfo{id=%s, identity=%s:%s, status=%s, temp=%s, messages=%d}",
                id, connectIdentity, port, connectStatus, isTemporary(), getMessageCount()
        );
    }

}
