//package com.persee.workstation.uv_inexus.mqconsumer;
//
//import com.persee.workstation.uv_inexus.component.EhcacheUtil;
//import com.persee.workstation.uv_inexus.util.AesUtil;
//import org.apache.rocketmq.common.message.MessageExt;
//import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
//import org.apache.rocketmq.spring.core.RocketMQListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.nio.charset.StandardCharsets;
//
//@Service
//@RocketMQMessageListener(
//        consumerGroup = "test",
//        topic = "test",
//        nameServer = "127.0.0.1:9876"
//)
//public class LocalTestConsumer implements RocketMQListener<MessageExt> {
//
//    @Autowired
//    private EhcacheUtil ehcacheUtil;
//
//    @Override
//    public void onMessage(MessageExt message) {
//        String msgId = message.getMsgId();
//        String keys = message.getKeys();
//
//        // 幂等性校验
//        if (keys != null && !keys.isEmpty()) {
//            // 尝试放入缓存，如果成功则表示是新消息，失败则表示已存在
//            boolean isNew = ehcacheUtil.putIfAbsent("mqIdempotentCache", keys, "1", String.class, String.class);
//            if (!isNew) {
//                System.out.println("Duplicate message detected, keys: " + keys);
//                return;
//            }
//        }
//
//        String body = new String(message.getBody(), StandardCharsets.UTF_8);
//
//        System.out.println("Received message msgId: " + msgId + ", keys: " + keys);
//
//        // 先解密
//        try {
//            String decryptedMessage = AesUtil.decrypt(body);
//            System.out.println("Decrypted message: " + decryptedMessage);
//        } catch (Exception e) {
//            System.out.println("Decrypt failed for body: " + body);
//        }
//    }
//}
