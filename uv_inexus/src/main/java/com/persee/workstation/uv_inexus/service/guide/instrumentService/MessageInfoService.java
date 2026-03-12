/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.instrumentService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.persee.workstation.uv_inexus.dto.messageDto.MessageInfoQueryDTO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.MessageInfoPO;
import com.persee.workstation.uv_inexus.vo.messageVo.MessageInfoVO;

import java.util.List;

public interface MessageInfoService {

    /**
     * 获取消息列表
     */
    IPage<MessageInfoVO> getMessageInfoList(MessageInfoQueryDTO query);

    /**
     * 获取消息内容
     */
    MessageInfoVO getMessageInfo(String messageId);

    /**
     * 删除消息
     */
    Boolean deleteMessageInfo(String messageId);

    /**
     * 批量删除消息
     */
    Boolean deleteMessageInfoBatch(List<String> messageIds);

    /**
     * 保存信息
     */
    Boolean saveMessageInfo(MessageInfoPO po);

    /**
     * 统计未读消息数
     */
    Integer countUnreadMessage();

    /**
     * 一键已读所有消息
     */
    Boolean readAllMessage();

}
