package com.persee.workstation.uv_inexus.mqconsumer;

import com.persee.workstation.uv_inexus.util.AesUtil;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(
        consumerGroup = "test",
        topic = "test",
        nameServer = "127.0.0.1:9876"
)
public class LocalTestConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        System.out.println("Received message from test: " + message);
        // 先解密
        String decryptedMessage = AesUtil.decrypt(message);
        System.out.println("Received message from test: " + decryptedMessage);
    }
}

