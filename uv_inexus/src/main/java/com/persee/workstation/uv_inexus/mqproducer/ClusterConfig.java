package com.persee.workstation.uv_inexus.mqproducer;

/**
 * 动态生产者集群配置
 * 用于描述一个可按ID引用的RocketMQ生产者基础参数
 */
public class ClusterConfig {
    /** 集群唯一标识，后续通过该ID进行消息发送 */
    private String clusterId;
    /** NameServer地址，支持多个地址以分号分隔 */
    private String nameServers;
    /** Producer所属分组名称 */
    private String producerGroup;
    /** 同步发送超时时间，单位毫秒 */
    private Integer sendMessageTimeout;
    /** 同步发送失败重试次数 */
    private Integer retryTimesWhenSendFailed;
    /** 异步发送失败重试次数 */
    private Integer retryTimesWhenSendAsyncFailed;

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getNameServers() {
        return nameServers;
    }

    public void setNameServers(String nameServers) {
        this.nameServers = nameServers;
    }

    public String getProducerGroup() {
        return producerGroup;
    }

    public void setProducerGroup(String producerGroup) {
        this.producerGroup = producerGroup;
    }

    public Integer getSendMessageTimeout() {
        return sendMessageTimeout;
    }

    public void setSendMessageTimeout(Integer sendMessageTimeout) {
        this.sendMessageTimeout = sendMessageTimeout;
    }

    public Integer getRetryTimesWhenSendFailed() {
        return retryTimesWhenSendFailed;
    }

    public void setRetryTimesWhenSendFailed(Integer retryTimesWhenSendFailed) {
        this.retryTimesWhenSendFailed = retryTimesWhenSendFailed;
    }

    public Integer getRetryTimesWhenSendAsyncFailed() {
        return retryTimesWhenSendAsyncFailed;
    }

    public void setRetryTimesWhenSendAsyncFailed(Integer retryTimesWhenSendAsyncFailed) {
        this.retryTimesWhenSendAsyncFailed = retryTimesWhenSendAsyncFailed;
    }
}
