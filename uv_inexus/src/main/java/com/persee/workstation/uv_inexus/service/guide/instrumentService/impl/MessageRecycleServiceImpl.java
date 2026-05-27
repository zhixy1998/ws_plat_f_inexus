package com.persee.workstation.uv_inexus.service.guide.instrumentService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.MessageInfoDAO;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.MessageRecycleDAO;
import com.persee.workstation.uv_inexus.dto.messageDto.MessageInfoQueryDTO;
import com.persee.workstation.uv_inexus.enums.messageEnums.MessageStatusEnum;
import com.persee.workstation.uv_inexus.enums.messageEnums.MessageTypeEnum;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.MessageInfoPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.MessageRecyclePO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.MessageRecycleService;
import com.persee.workstation.uv_inexus.vo.messageVo.MessageInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessageRecycleServiceImpl implements MessageRecycleService {

    @Value("${message.recycle-max-count:10000}")
    private int recycleMaxCount;

    @Value("${message.recycle-keep-days:30}")
    private int recycleKeepDays;

    @Autowired
    private MessageRecycleDAO recycleDAO;

    @Autowired
    private MessageInfoDAO messageInfoDAO;

    @Override
    public void moveToRecycle(MessageInfoPO po) {
        if (po == null) {
            return;
        }
        MessageRecyclePO recyclePO = new MessageRecyclePO();
        BeanUtils.copyProperties(po, recyclePO);
        recyclePO.setOriginalCreateBy(po.getCreateBy());
        recyclePO.setOriginalUpdateBy(po.getUpdateBy());
        recyclePO.setOriginalCreateTime(po.getCreated());
        recyclePO.setDeletedTime(LocalDateTime.now());
        recycleDAO.insert(recyclePO);
        log.debug("消息 {} 已移入回收站", po.getId());
    }

    @Override
    public Boolean restoreMessage(String messageId) {
        MessageRecyclePO recyclePO = recycleDAO.selectById(messageId);
        if (recyclePO == null) {
            return false;
        }

        MessageInfoPO messagePO = new MessageInfoPO();
        BeanUtils.copyProperties(recyclePO, messagePO);
        messagePO.setCreateBy(recyclePO.getOriginalCreateBy());
        messagePO.setUpdateBy(recyclePO.getOriginalUpdateBy());
        messagePO.setCreated(recyclePO.getOriginalCreateTime());
        messagePO.setUpdated(LocalDateTime.now());
        messageInfoDAO.insert(messagePO);

        recycleDAO.deleteById(messageId);
        log.info("消息 {} 已从回收站恢复", messageId);
        return true;
    }

    @Override
    public IPage<MessageInfoVO> getRecycleList(MessageInfoQueryDTO query) {
        int pageNum = query.getPageNum() != null && query.getPageNum() > 0 ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 10;

        Page<MessageRecyclePO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MessageRecyclePO> wrapper = new LambdaQueryWrapper<>();

        if (query.getMessageType() != null) {
            wrapper.eq(MessageRecyclePO::getMessageType, query.getMessageType());
        }
        if (StringUtils.hasText(query.getQuery())) {
            wrapper.like(MessageRecyclePO::getMessageSummary, query.getQuery());
        }
        wrapper.orderByDesc(MessageRecyclePO::getDeletedTime);

        IPage<MessageRecyclePO> iPage = recycleDAO.selectPage(page, wrapper);

        List<MessageInfoVO> voList = iPage.getRecords().stream().map(recyclePO -> {
            MessageInfoVO vo = new MessageInfoVO();
            vo.setId(recyclePO.getId());
            vo.setMessageType(recyclePO.getMessageType());
            vo.setMessageTypeName(MessageTypeEnum.getDescription(recyclePO.getMessageType()));
            vo.setMessageSource(recyclePO.getMessageSource());
            vo.setMessageContent(recyclePO.getMessageContent());
            vo.setMessageStatus(recyclePO.getMessageStatus());
            vo.setMessageStatusName(MessageStatusEnum.getDescription(recyclePO.getMessageStatus()));
            vo.setReceiveTime(recyclePO.getReceiveTime());
            vo.setMessageSummary(recyclePO.getMessageSummary());
            return vo;
        }).collect(Collectors.toList());

        IPage<MessageInfoVO> voPage = new Page<>(iPage.getCurrent(), iPage.getSize(), iPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public void cleanupOverLimit() {
        Long totalCount = messageInfoDAO.selectCount(null);
        if (totalCount > recycleMaxCount) {
            long toDelete = totalCount - recycleMaxCount;
            LambdaQueryWrapper<MessageInfoPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByAsc(MessageInfoPO::getCreated)
                   .last("LIMIT " + toDelete);
            List<MessageInfoPO> oldMessages = messageInfoDAO.selectList(wrapper);

            for (MessageInfoPO po : oldMessages) {
                moveToRecycle(po);
                messageInfoDAO.deleteById(po.getId());
            }
            log.info("消息数量超过 {} 条，已将最旧的 {} 条移入回收站", recycleMaxCount, toDelete);
        }
    }

    @Override
    public void cleanupOldRecords() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(recycleKeepDays);
        LambdaQueryWrapper<MessageRecyclePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(MessageRecyclePO::getDeletedTime, threshold);
        int count = recycleDAO.delete(wrapper);
        if (count > 0) {
            log.info("已清理回收站中超过30天的 {} 条记录", count);
        }
    }
}
