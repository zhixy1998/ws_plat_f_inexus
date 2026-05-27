package com.persee.workstation.uv_inexus.service.guide.instrumentService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.persee.workstation.uv_inexus.dto.messageDto.MessageInfoQueryDTO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.MessageInfoPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.MessageRecyclePO;
import com.persee.workstation.uv_inexus.vo.messageVo.MessageInfoVO;

public interface MessageRecycleService {

    void moveToRecycle(MessageInfoPO po);

    Boolean restoreMessage(String messageId);

    IPage<MessageInfoVO> getRecycleList(MessageInfoQueryDTO query);

    void cleanupOverLimit();

    void cleanupOldRecords();
}
