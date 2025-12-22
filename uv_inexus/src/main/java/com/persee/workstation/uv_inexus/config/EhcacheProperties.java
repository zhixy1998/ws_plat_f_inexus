package com.persee.workstation.uv_inexus.config;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ehcache")
@Data
public class EhcacheProperties {
    private String diskStorePath;
    private Map<String, CacheConfig> caches = new HashMap<>();

    @Data
    public static class CacheConfig {
        //堆内缓存条目数
        private long heap;
        //堆外缓存大小
        private long offheap;
        //磁盘缓存大小
        private long disk;
        //缓存过期时间
        private String ttl; // 支持如 30m, 2h, 1d 的格式
    }
}
