/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.softwareActivation;

import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.po.guide.softwareActivation.SoftwareActivationPO;

/**
 * 软件激活Service接口（继承MyBatis-Plus的IService，简化CRUD实现）
 */
public interface SoftwareActivationService extends IService<SoftwareActivationPO> {

    /**
     * 分页条件查询
     * @param page 分页对象
     * @param params 查询条件（key为实体类字段名，value为查询值）
     * @return 分页结果
     */
    IPage<SoftwareActivationPO> findPage(Page<SoftwareActivationPO> page, Map<String, String> params);

    /**
     * 根据设备序列号查询记录
     */
    SoftwareActivationPO findByDeviceNumber(String deviceNumber);
}