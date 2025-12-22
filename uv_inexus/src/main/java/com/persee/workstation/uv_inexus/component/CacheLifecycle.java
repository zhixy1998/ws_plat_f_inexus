package com.persee.workstation.uv_inexus.component;

import org.ehcache.CacheManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Component
public class CacheLifecycle implements DisposableBean {

    @Autowired
    private CacheManager cacheManager;

    @Override
    public void destroy() throws Exception {
        // Spring容器关闭时自动调用，进行缓存管理器的关闭，保证缓存数据的可靠性
        cacheManager.close();
    }
}
