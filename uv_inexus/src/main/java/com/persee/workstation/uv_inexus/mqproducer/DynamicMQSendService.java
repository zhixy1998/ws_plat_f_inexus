package com.persee.workstation.uv_inexus.mqproducer;

/**
 * 动态消息发送服务接口
 * 按clusterId路由到对应生产者进行多种类型消息发送
 */
public interface DynamicMQSendService {
    /**
     * 发送普通同步消息
     */
    boolean sendNormal(String clusterId, String topic, String tag, String jsonBody);

    /**
     * 发送顺序消息，按hashKey做队列路由
     */
    boolean sendOrderly(String clusterId, String topic, String tag, String jsonBody, String hashKey);

    /**
     * 发送延时消息，delayLevel使用RocketMQ预定义级别
     */
    boolean sendDelay(String clusterId, String topic, String tag, String jsonBody, int delayLevel);
}
