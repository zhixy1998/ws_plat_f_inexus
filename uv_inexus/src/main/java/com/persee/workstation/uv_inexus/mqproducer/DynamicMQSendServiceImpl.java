package com.persee.workstation.uv_inexus.mqproducer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.persee.workstation.uv_inexus.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 动态消息发送服务实现
 * 根据clusterId从注册表中获取模板并完成消息发送
 */
@Slf4j
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
                log.error("JSON校验异常");
                return false;
            }
            String encryptedBody = AesUtil.encrypt(jsonBody);
            String destination = tag != null && !tag.isEmpty() ? topic + ":" + tag : topic;
            String bizKey = UUID.randomUUID().toString();
            SendResult result = getNormalTemplate(clusterId)
                    .syncSend(destination, MessageBuilder.withPayload(encryptedBody).setHeader(RocketMQHeaders.KEYS, bizKey).build());
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
                log.error("JSON校验异常");
                return false;
            }
            String encryptedBody = AesUtil.encrypt(jsonBody);
            String destination = tag != null && !tag.isEmpty() ? topic + ":" + tag : topic;
            String bizKey = UUID.randomUUID().toString();
            SendResult result = getNormalTemplate(clusterId)
                    .syncSendOrderly(destination, MessageBuilder.withPayload(encryptedBody).setHeader(RocketMQHeaders.KEYS, bizKey).build(), hashKey);
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
                log.error("JSON校验异常");
                return false;
            }
            String encryptedBody = AesUtil.encrypt(jsonBody);
            String destination = tag != null && !tag.isEmpty() ? topic + ":" + tag : topic;
            String bizKey = UUID.randomUUID().toString();
            RocketMQTemplate template = getNormalTemplate(clusterId);
            SendResult result = template.syncSend(
                    destination,
                    MessageBuilder.withPayload(encryptedBody).setHeader(RocketMQHeaders.KEYS, bizKey).build(),
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
