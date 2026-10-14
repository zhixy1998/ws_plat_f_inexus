/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.systemManageService;

import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemCustomManageDTO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemDataBaseConfDTO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemDataBaseVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemManageUserConfVO;

public interface SystemctlDatabaseConfService {

    /**
     * 数据库配置(维修人员)
     */
    Boolean saveDatabaseConf(SystemDataBaseConfDTO dto);

    /**
     * 系统配置保存(用户)
     */
    Boolean saveCustomDatabaseConf(SystemCustomManageDTO dto);

    /**
     * 查询系统配置(用户)
     */
    SystemManageUserConfVO querySysManageUserConf(String updateBy);

    /**
     * 查询数据库(维修人员)
     */
    SystemDataBaseVO queryDatabaseConf(String updateBy);

    Boolean resetDatabaseConf(String updateBy);



}
