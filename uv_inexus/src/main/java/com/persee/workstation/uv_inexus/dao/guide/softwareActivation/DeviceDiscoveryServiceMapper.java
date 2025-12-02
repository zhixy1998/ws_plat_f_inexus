/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dao.guide.softwareActivation;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.guide.softwareActivation.SoftwareActivationPO;

// 仅继承BaseMapper，使用其自带的insert方法
@Mapper
public interface DeviceDiscoveryServiceMapper extends BaseMapper<SoftwareActivationPO> {
    // 移除insertBatch方法，无需额外定义
}