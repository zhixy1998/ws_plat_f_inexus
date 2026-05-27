package com.persee.workstation.uv_inexus.service.guide.instrumentService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.persee.workstation.uv_inexus.dto.messageDto.MessageDeleteDTO;
import com.persee.workstation.uv_inexus.dto.messageDto.MessageInfoQueryDTO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.MessageInfoPO;
import com.persee.workstation.uv_inexus.vo.messageVo.MessageInfoVO;

public interface MessageInfoService {

    IPage<MessageInfoVO> getMessageInfoList(MessageInfoQueryDTO query);

    MessageInfoVO getMessageInfo(String messageId);

    Boolean deleteMessageInfo(String messageId);

    Boolean deleteMessageInfoBatch(MessageDeleteDTO dto);

    Boolean saveMessageInfo(MessageInfoPO po);

    Boolean saveMessageInfo(MessageInfoPO po, Integer reminderType);

    Integer countUnreadMessage();

    Integer countUnreadMessage(String userName);

    Boolean readAllMessage();

    Boolean readAllMessage(String userName);

}
