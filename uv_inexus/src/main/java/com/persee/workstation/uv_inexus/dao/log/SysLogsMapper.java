/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dao.log;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.log.SysLogsPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统日志表 Mapper：路由到 log 数据源，支持基础 CRUD
 */
//@DS("log")
@Mapper
public interface SysLogsMapper extends BaseMapper<SysLogsPO> {
}
