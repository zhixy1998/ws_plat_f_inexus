package com.persee.workstation.uv_inexus.mqproducer;

import lombok.Data;

/**
 * 动态生产者集群配置
 * 用于描述一个可按ID引用的RocketMQ生产者基础参数
 */
@Data
public class ClusterConfig {
    /** 集群唯一标识，后续通过该ID进行消息发送 */
    private String clusterId;
    /** NameServer地址，支持多个地址以分号分隔 */
    private String nameServers;
    /** Producer所属分组名称 */
    private String producerGroup = "DEFAULT_PRODUCER_GROUP";
    /** 同步发送超时时间，单位毫秒 */
    private Integer sendMessageTimeout = 3000;
    /** 同步发送失败重试次数 */
    private Integer retryTimesWhenSendFailed = 2;
    /** 异步发送失败重试次数 */
    private Integer retryTimesWhenSendAsyncFailed = 2;
}
