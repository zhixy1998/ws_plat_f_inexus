package com.persee.workstation.uv_inexus.mqproducer;

import org.apache.rocketmq.spring.core.RocketMQTemplate;

/**
 * 集群上下文
 * 保存与某个clusterId绑定的RocketMQTemplate实例
 */
public class ClusterContext {
    /** 普通消息发送模板 */
    private RocketMQTemplate normalTemplate;

    public RocketMQTemplate getNormalTemplate() {
        return normalTemplate;
    }

    public void setNormalTemplate(RocketMQTemplate normalTemplate) {
        this.normalTemplate = normalTemplate;
    }
}
