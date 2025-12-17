package com.persee.workstation.uv_inexus.mqproducer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 动态MQ生产者工具类
 * 对外提供按ID注册、注销以及发送消息的便捷接口
 */
@Component
public class DynamicMqProducerUtil {
    /** 动态生产者工厂，用于创建ClusterContext */
    @Autowired
    private ProducerFactory producerFactory;

    /** 生产者注册表，负责缓存与查找生产者 */
    @Autowired
    private ProducerRegistry producerRegistry;

    /** 动态发送服务，实现具体的发送逻辑 */
    @Autowired
    private DynamicMQSendService dynamicMQSendService;

    /**
     * 根据配置注册一个新的生产者
     */
    public void registerProducer(ClusterConfig config) throws Exception {
        ClusterContext context = producerFactory.create(config);
        producerRegistry.register(config.getClusterId(), context);
    }

    /**
     * 注销指定ID的生产者并释放资源
     */
    public boolean unregisterProducer(String clusterId) {
        return producerRegistry.remove(clusterId);
    }

    /**
     * 使用指定ID的生产者发送普通同步消息
     */
    public boolean sendNormal(String clusterId, String topic, String tag, String jsonBody) {

        return dynamicMQSendService.sendNormal(clusterId, topic, tag, jsonBody);
    }

    /**
     * 使用指定ID的生产者发送顺序消息
     */
    public boolean sendOrderly(String clusterId, String topic, String tag, String jsonBody, String hashKey) {
        return dynamicMQSendService.sendOrderly(clusterId, topic, tag, jsonBody, hashKey);
    }

    /**
     * 使用指定ID的生产者发送延时消息
     */
    public boolean sendDelay(String clusterId, String topic, String tag, String jsonBody, int delayLevel) {
        return dynamicMQSendService.sendDelay(clusterId, topic, tag, jsonBody, delayLevel);
    }
}
