package com.persee.workstation.uv_inexus.mqproducer;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 动态生产者工厂
 * 根据ClusterConfig创建并启动对应的DefaultMQProducer与RocketMQTemplate
 */
@Component
public class ProducerFactory {
    /** RocketMQ消息转换器，复用Spring配置 */
    @Autowired
    private RocketMQMessageConverter messageConverter;

    /**
     * 创建并启动一个新的生产者集群上下文
     */
    public ClusterContext create(ClusterConfig config) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer(config.getProducerGroup());
        producer.setNamesrvAddr(config.getNameServers());
        producer.setVipChannelEnabled(false);
        if (config.getSendMessageTimeout() != null) {
            producer.setSendMsgTimeout(config.getSendMessageTimeout());
        }
        if (config.getRetryTimesWhenSendFailed() != null) {
            producer.setRetryTimesWhenSendFailed(config.getRetryTimesWhenSendFailed());
        }
        if (config.getRetryTimesWhenSendAsyncFailed() != null) {
            producer.setRetryTimesWhenSendAsyncFailed(config.getRetryTimesWhenSendAsyncFailed());
        }
        producer.start();

        RocketMQTemplate normalTemplate = new RocketMQTemplate();
        normalTemplate.setProducer(producer);
        normalTemplate.setMessageConverter(messageConverter.getMessageConverter());

        ClusterContext context = new ClusterContext();
        context.setNormalTemplate(normalTemplate);
        return context;
    }
}
