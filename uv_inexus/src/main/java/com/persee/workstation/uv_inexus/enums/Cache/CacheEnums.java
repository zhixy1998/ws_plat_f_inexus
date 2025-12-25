package com.persee.workstation.uv_inexus.enums.Cache;

import lombok.Getter;

import java.util.Arrays;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Getter
public enum CacheEnums {
    CALIBRATION_CACHE("calibrationCache", "检定校准缓存"),
    MQ_IDEMPOTENT_CACHE("mqIdempotentCache", "MQ幂等性缓存"),
    WAVEFORM_DATA_CACHE("waveformDataCache", "波形数据缓存"),
    /**
     * 默认缓存支持多种数据类型，但不支持持久化
     */
    DEFAULT_CACHE("defaultCache", "默认缓存");
    private final String cacheName;
    private final String cacheDesc;

    CacheEnums(String cacheName, String cacheDesc) {
        this.cacheName = cacheName;
        this.cacheDesc = cacheDesc;
    }

    public static CacheEnums fromCacheName(String cacheName) {
        return Arrays.stream(CacheEnums.values())
                .filter(cacheEnum -> cacheEnum.getCacheName().equals(cacheName))
                .findFirst()
                .orElse(null);
    }


}
