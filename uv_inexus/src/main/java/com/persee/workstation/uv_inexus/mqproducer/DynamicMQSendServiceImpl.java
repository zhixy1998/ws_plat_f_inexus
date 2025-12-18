package com.persee.workstation.uv_inexus.mqproducer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * 动态消息发送服务实现
 * 根据clusterId从注册表中获取模板并完成消息发送
 */
@Service
public class DynamicMQSendServiceImpl implements DynamicMQSendService {
    /** 生产者注册表，负责按clusterId查找模板 */
    @Autowired
    private ProducerRegistry registry;

    /**
     * 简单校验字符串是否为合法JSON
     */
    private boolean isValidJson(String value) {
        try {
            JSON.parse(value);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * 根据clusterId获取普通消息模板
     */
    private RocketMQTemplate getNormalTemplate(String clusterId) {
        ClusterContext context = registry.get(clusterId);
        if (context == null) {
            throw new IllegalArgumentException("cluster not found");
        }
        return context.getNormalTemplate();
    }

    @Override
    /**
     * 普通同步消息发送
     */
    public boolean sendNormal(String clusterId, String topic, String tag, String jsonBody) {
        try {
            if (!isValidJson(jsonBody)) {
                return false;
            }
            String destination = tag != null && !tag.isEmpty() ? topic + ":" + tag : topic;
            SendResult result = getNormalTemplate(clusterId)
                    .syncSend(destination, MessageBuilder.withPayload(jsonBody).build());
            SendStatus status = result.getSendStatus();
            return status == SendStatus.SEND_OK || status == SendStatus.SLAVE_NOT_AVAILABLE;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    /**
     * 顺序消息发送
     */
    public boolean sendOrderly(String clusterId, String topic, String tag, String jsonBody, String hashKey) {
        try {
            if (!isValidJson(jsonBody)) {
                return false;
            }
            String destination = tag != null && !tag.isEmpty() ? topic + ":" + tag : topic;
            SendResult result = getNormalTemplate(clusterId)
                    .syncSendOrderly(destination, MessageBuilder.withPayload(jsonBody).build(), hashKey);
            SendStatus status = result.getSendStatus();
            return status == SendStatus.SEND_OK || status == SendStatus.SLAVE_NOT_AVAILABLE;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    /**
     * 延时消息发送
     */
    public boolean sendDelay(String clusterId, String topic, String tag, String jsonBody, int delayLevel) {
        try {
            if (!isValidJson(jsonBody)) {
                return false;
            }
            String destination = tag != null && !tag.isEmpty() ? topic + ":" + tag : topic;
            RocketMQTemplate template = getNormalTemplate(clusterId);
            SendResult result = template.syncSend(
                    destination,
                    MessageBuilder.withPayload(jsonBody).build(),
                    template.getProducer().getSendMsgTimeout(),
                    delayLevel
            );
            SendStatus status = result.getSendStatus();
            return status == SendStatus.SEND_OK || status == SendStatus.SLAVE_NOT_AVAILABLE;
        } catch (Exception e) {
            return false;
        }
    }
}
