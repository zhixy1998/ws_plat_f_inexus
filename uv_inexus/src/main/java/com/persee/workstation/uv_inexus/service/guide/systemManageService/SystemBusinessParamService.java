/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.systemManageService;

import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemBusinessParamDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemAutoLogoutConfVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemBusinessParamVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemManageMeasureConfVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemStandCurveConfVO;

public interface SystemBusinessParamService {

    SystemBusinessParamVO queryBusinessParam();

    R<SystemManageMeasureConfVO> queryMeasureConf();

    R<SystemStandCurveConfVO> queryStandCurveConf();

    R<SystemAutoLogoutConfVO> queryAutoLogoutConf();

    Boolean saveBusinessParam(SystemBusinessParamDTO dto);

    Boolean resetBusinessParam();

}
