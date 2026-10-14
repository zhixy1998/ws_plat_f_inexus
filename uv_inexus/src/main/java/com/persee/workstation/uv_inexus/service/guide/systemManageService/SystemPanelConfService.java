/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.systemManageService;

import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemLoginConfDTO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemPanelConfDTO;
import com.persee.workstation.uv_inexus.dto.systemManageDto.SystemPanelOrderDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemLoginConfVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.SystemPanelConfVO;

import java.util.List;

public interface SystemPanelConfService {
    Boolean savePanelConf(SystemPanelConfDTO dto);

    SystemPanelConfVO queryPanelConf();

    Boolean resetPanelConf(String updateBy);

    List<String> queryPanelOrder();

    R<Boolean> savePanelOrder(SystemPanelOrderDTO dto);

    R<Boolean> changeLoginInfo(SystemLoginConfDTO dto);

    R<SystemLoginConfVO> queryLoginInfo();

    Boolean getDefaultClassicSwitch();
}
