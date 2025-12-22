package com.persee.workstation.uv_inexus.config;

import com.persee.workstation.uv_inexus.dto.factory_inspection.SerializableWorkbook;
import org.ehcache.CacheManager;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.ExpiryPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
@ComponentScan(basePackages = "com.persee.workstation.uv_inexus.config") // 确保包扫描正确
public class EhcacheConfig {

    @Autowired
    private EhcacheProperties ehcacheProperties;

    @Bean
    public CacheManager ehcacheCacheManager() {
        // 1. 构建CacheManager，并指定磁盘持久化路径
        CacheManagerBuilder<PersistentCacheManager> cacheManagerBuilder =
                CacheManagerBuilder.newCacheManagerBuilder()
                        .with(CacheManagerBuilder.persistence(ehcacheProperties.getDiskStorePath()));


        for (Map.Entry<String, EhcacheProperties.CacheConfig> entry : ehcacheProperties.getCaches().entrySet()) {
            String cacheName = entry.getKey();
            EhcacheProperties.CacheConfig config = entry.getValue();

            switch (cacheName) {
                case "calibrationCache":
                    cacheManagerBuilder = calibrationCacheConfig(cacheManagerBuilder, config);
            }
        }

        // 3. 构建并初始化CacheManager
        PersistentCacheManager cacheManager = cacheManagerBuilder.build(true);
        System.out.println("创建的缓存数量: " + cacheManager.getRuntimeConfiguration().getCacheConfigurations().size());
        cacheManager.getRuntimeConfiguration().getCacheConfigurations().keySet()
                .forEach(name -> System.out.println("已创建缓存: " + name));

        return cacheManager;
    }

    /**
     * 创建检定校准缓存配置
     *
     * @param cacheManagerBuilder
     * @param config
     */
    public CacheManagerBuilder<PersistentCacheManager> calibrationCacheConfig(CacheManagerBuilder<PersistentCacheManager> cacheManagerBuilder, EhcacheProperties.CacheConfig config) {
        System.out.println("配置缓存: " + "calibrationCache");

        // 构建资源池（多级存储）
        ResourcePoolsBuilder resourcePoolsBuilder = ResourcePoolsBuilder.newResourcePoolsBuilder()
                .heap(config.getHeap(),EntryUnit.ENTRIES) // 堆内按条目数
                .disk(config.getDisk(), MemoryUnit.MB, true); // 磁盘持久化，true表示持久化到磁盘

        // 使用具体类型替代 Object.class
        CacheConfiguration<String, SerializableWorkbook> cacheConfiguration = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(String.class,SerializableWorkbook.class, resourcePoolsBuilder)
                .build();

        // 将缓存配置添加到CacheManager构建器中
        cacheManagerBuilder = cacheManagerBuilder.withCache("calibrationCache", cacheConfiguration);

        return cacheManagerBuilder;
    }





    /**
     * 解析TTL字符串，转换为Ehcache的过期策略。
     * 支持格式： 30s, 10m, 2h, 1d
     */
    private ExpiryPolicy<?, ?> parseTtl(String ttlStr) {
        if (!StringUtils.hasText(ttlStr)) {
            return ExpiryPolicyBuilder.noExpiration();
        }
        try {
            char unit = ttlStr.charAt(ttlStr.length() - 1);
            long value = Long.parseLong(ttlStr.substring(0, ttlStr.length() - 1));
            Duration duration;
            switch (unit) {
                case 's':
                    duration = Duration.ofSeconds(value);
                    break;
                case 'm':
                    duration = Duration.ofMinutes(value);
                    break;
                case 'h':
                    duration = Duration.ofHours(value);
                    break;
                case 'd':
                    duration = Duration.ofDays(value);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported TTL unit: " + unit);
            }
            return ExpiryPolicyBuilder.timeToLiveExpiration(duration);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid TTL format: " + ttlStr, e);
        }
    }

}
