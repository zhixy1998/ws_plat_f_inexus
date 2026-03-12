/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.instrumentService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.MessageInfoDAO;
import com.persee.workstation.uv_inexus.dto.messageDto.MessageInfoQueryDTO;
import com.persee.workstation.uv_inexus.enums.messageEnums.MessageStatusEnum;
import com.persee.workstation.uv_inexus.enums.messageEnums.MessageTypeEnum;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.MessageInfoPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.MessageInfoService;
import com.persee.workstation.uv_inexus.vo.messageVo.MessageInfoVO;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketSendUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageInfoServiceImpl implements MessageInfoService {

    @Autowired
    MessageInfoDAO dao;

    @Autowired
    WebSocketSendUtil webSocketSendUtil;

    @Override
    public IPage<MessageInfoVO> getMessageInfoList(MessageInfoQueryDTO query) {
        // 1. 分页参数默认值
        int pageNum = query.getPageNum() != null && query.getPageNum() > 0 ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 10;

        // 2. 创建分页对象
        Page<MessageInfoPO> page = new Page<>(pageNum, pageSize);

        // 3. 构建查询条件（与之前相同）
        LambdaQueryWrapper<MessageInfoPO> wrapper = new LambdaQueryWrapper<>();
        if (query.getMessageType() != null) {
            wrapper.eq(MessageInfoPO::getMessageType, query.getMessageType());
        }

        if (query.getMessageStatus() != null) {
            wrapper.eq(MessageInfoPO::getMessageStatus, query.getMessageStatus());
        }

        if (StringUtils.hasText(query.getMessageSource())) {
            wrapper.eq(MessageInfoPO::getMessageSource, query.getMessageSource());
        }

        if (StringUtils.hasText(query.getQuery())) {
            wrapper.like(MessageInfoPO::getMessageSummary, query.getQuery());
        }

        if (query.getStartTime() != null) {
            wrapper.ge(MessageInfoPO::getReceiveTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(MessageInfoPO::getReceiveTime, query.getEndTime());
        }

        // 4. 执行分页查询
        IPage<MessageInfoPO> iPage = dao.selectPage(page, wrapper);

        // 5. 转换：直接复制属性（所有字段类型一致）
        List<MessageInfoVO> voList = iPage.getRecords().stream()
                .map(po -> {
                    MessageInfoVO vo = new MessageInfoVO();
                    BeanUtils.copyProperties(po, vo);
                    vo.setMessageStatusName(MessageStatusEnum.getDescription(vo.getMessageStatus()));
                    vo.setMessageTypeName(MessageTypeEnum.getDescription(vo.getMessageType()));
                    return vo;
                })
                .collect(Collectors.toList());
        IPage<MessageInfoVO> voPage = new Page<>(iPage.getCurrent(), iPage.getSize(), iPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public MessageInfoVO getMessageInfo(String messageId) {

        MessageInfoVO vo = new MessageInfoVO();
        MessageInfoPO po = dao.selectById(messageId);
        if (po != null) {
            BeanUtils.copyProperties(po, vo);
            vo.setMessageStatusName(MessageStatusEnum.getDescription(vo.getMessageStatus()));
            vo.setMessageTypeName(MessageTypeEnum.getDescription(vo.getMessageType()));
            po.setMessageStatus(MessageStatusEnum.STATUS_READ.getCode());
            dao.updateById(po);
        }

        return vo;
    }

    @Override
    public Boolean deleteMessageInfo(String messageId) {

        if (!StringUtils.hasText(messageId)) {
            return true;
        }

        int i = dao.deleteById(messageId);
        return i > 0;
    }

    @Override
    public Boolean deleteMessageInfoBatch(List<String> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return true;
        }

        int i = dao.deleteBatchIds(messageIds);
        return i > 0;
    }

    @Override
    public Boolean saveMessageInfo(MessageInfoPO po) {
        po.setMessageStatus(MessageStatusEnum.STATUS_UNREAD.getCode());
        po.setReceiveTime(LocalDateTime.now());
        po.setCreated(LocalDateTime.now());

        if (StringUtils.hasText(po.getId())) {
            po.setId(null);
        }
        int i = dao.insert(po);
        if (i > 0) {
            webSocketSendUtil.newMessageReport();
        }

        return i > 0;
    }

    @Override
    public Integer countUnreadMessage() {
        LambdaQueryWrapper<MessageInfoPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageInfoPO::getMessageStatus, MessageStatusEnum.STATUS_UNREAD.getCode());
        Long count = dao.selectCount(wrapper);

        return count.intValue();
    }

    @Override
    public Boolean readAllMessage() {
        LambdaUpdateWrapper<MessageInfoPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(MessageInfoPO::getMessageStatus, MessageStatusEnum.STATUS_UNREAD.getCode());
        List<MessageInfoPO> pos = dao.selectList(wrapper);
        if (pos == null || pos.isEmpty()) {
            return true;
        } else {
            wrapper.set(MessageInfoPO::getMessageStatus, MessageStatusEnum.STATUS_READ.getCode());
            int i = dao.update(null, wrapper);

            return i > 0;
        }

    }
}
