package com.persee.workstation.uv_inexus.mqproducer;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 动态生产者注册表
 * 负责维护clusterId与ClusterContext之间的映射关系并统一管理生命周期
 */
@Component
public class ProducerRegistry implements DisposableBean {
    /** 已注册集群的上下文缓存，key为clusterId */
    private final ConcurrentMap<String, ClusterContext> clusters = new ConcurrentHashMap<>();

    /**
     * 注册新的集群上下文
     * clusterId重复时抛出异常，防止误覆盖
     */
    public synchronized void register(String clusterId, ClusterContext context) {
        if (clusters.containsKey(clusterId)) {
            throw new IllegalStateException("cluster exists");
        }
        clusters.put(clusterId, context);
    }

    /**
     * 根据clusterId获取集群上下文
     */
    public ClusterContext get(String clusterId) {
        return clusters.get(clusterId);
    }

    /**
     * 返回当前已注册的所有clusterId列表
     */
    public List<String> list() {
        return clusters.keySet().stream().sorted().collect(Collectors.toList());
    }

    /**
     * 移除指定clusterId并关闭对应生产者
     */
    public synchronized boolean remove(String clusterId) {
        ClusterContext context = clusters.remove(clusterId);
        if (context == null) {
            return false;
        }
        shutdown(context);
        return true;
    }

    /**
     * 关闭ClusterContext中持有的底层生产者
     */
    private void shutdown(ClusterContext context) {
        RocketMQTemplate normalTemplate = context.getNormalTemplate();
        if (normalTemplate != null && normalTemplate.getProducer() != null) {
            normalTemplate.getProducer().shutdown();
        }
    }

    @Override
    public void destroy() {
        clusters.values().forEach(this::shutdown);
        clusters.clear();
    }
}
