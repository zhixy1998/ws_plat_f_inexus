package com.persee.workstation.uv_inexus.service.guide.instrumentService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.MessageInfoDAO;
import com.persee.workstation.uv_inexus.dto.messageDto.MessageDeleteDTO;
import com.persee.workstation.uv_inexus.dto.messageDto.MessageInfoQueryDTO;
import com.persee.workstation.uv_inexus.enums.messageEnums.MessageStatusEnum;
import com.persee.workstation.uv_inexus.enums.messageEnums.MessageTypeEnum;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.MessageInfoPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.MessageInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.MessageRecycleService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.UserReminderMuteService;
import com.persee.workstation.uv_inexus.vo.messageVo.MessageInfoVO;
import com.persee.workstation.uv_inexus.vo.messageVo.MessageReport;
import com.persee.workstation.uv_inexus.websocket.liveDetectionManyDevices.WebSocketSendUtil;
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
public class MessageInfoServiceImpl implements MessageInfoService {

    @Autowired
    MessageInfoDAO dao;

    @Autowired
    WebSocketSendUtil webSocketSendUtil;

    @Autowired
    MessageRecycleService messageRecycleService;

    @Autowired
    UserReminderMuteService userReminderMuteService;

    @Value("${message.user-isolation-enabled:false}")
    private boolean userIsolationEnabled;

/**
 * 应用用户过滤器，根据用户名条件查询消息信息
 * @param wrapper Lambda查询包装器，用于构建查询条件
 * @param userName 用户名，用于过滤消息更新者
 */
    private void applyUserFilter(LambdaQueryWrapper<MessageInfoPO> wrapper, String userName) {
    // 如果启用了用户隔离并且用户名不为空
        if (userIsolationEnabled && StringUtils.hasText(userName)) {
        // 添加查询条件：更新者等于指定用户名
            wrapper.eq(MessageInfoPO::getUpdateBy, userName);
        }
    }

/**
 * 应用用户过滤器，根据用户名条件更新查询条件
 * @param wrapper Lambda更新包装器，用于构建SQL条件
 * @param userName 用户名，用于过滤更新者
 */
    private void applyUserFilter(LambdaUpdateWrapper<MessageInfoPO> wrapper, String userName) {
    // 检查用户隔离是否启用且用户名不为空
        if (userIsolationEnabled && StringUtils.hasText(userName)) {
        // 添加等于条件，筛选出由指定用户更新的记录
            wrapper.eq(MessageInfoPO::getUpdateBy, userName);
        }
    }

    @Override
    public IPage<MessageInfoVO> getMessageInfoList(MessageInfoQueryDTO query) {
        int pageNum = query.getPageNum() != null && query.getPageNum() > 0 ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 10;

        Page<MessageInfoPO> page = new Page<>(pageNum, pageSize);

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

        applyUserFilter(wrapper, query.getUserName());

        wrapper.orderByDesc(MessageInfoPO::getReceiveTime);

        IPage<MessageInfoPO> iPage = dao.selectPage(page, wrapper);

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
            if (!MessageStatusEnum.STATUS_READ.getCode().equals(po.getMessageStatus())) {
                po.setMessageStatus(MessageStatusEnum.STATUS_READ.getCode());
                dao.updateById(po);
            }
        }

        return vo;
    }

    @Override
    public Boolean deleteMessageInfo(String messageId) {

        if (!StringUtils.hasText(messageId)) {
            return true;
        }

        MessageInfoPO po = dao.selectById(messageId);
        if (po != null) {
            messageRecycleService.moveToRecycle(po);
            dao.deleteById(messageId);
        }
        return true;
    }

    @Override
    public Boolean deleteMessageInfoBatch(MessageDeleteDTO dto) {

        List<String> params = dto.getParams();

        if (params == null || params.isEmpty()) {
            return true;
        }

        List<MessageInfoPO> pos = dao.selectBatchIds(params);
        if (pos != null) {
            for (MessageInfoPO po : pos) {
                messageRecycleService.moveToRecycle(po);
            }
        }
        dao.deleteBatchIds(params);
        return true;
    }

    @Override
    public Boolean saveMessageInfo(MessageInfoPO po) {
        return saveMessageInfo(po, null);
    }

    @Override
    public Boolean saveMessageInfo(MessageInfoPO po, Integer reminderType) {
        po.setMessageStatus(MessageStatusEnum.STATUS_UNREAD.getCode());
        po.setReceiveTime(LocalDateTime.now());
        po.setCreated(LocalDateTime.now());

        if (StringUtils.hasText(po.getId())) {
            po.setId(null);
        }

        if (reminderType != null
                && po.getMessageType() != null
                && po.getMessageType().equals(MessageTypeEnum.TYPE_REMIND_MESSAGE.getCode())
                && userReminderMuteService.isMuted(po.getUpdateBy(), reminderType)) {
            po.setMessageType(MessageTypeEnum.TYPE_NOTICE_MESSAGE.getCode());
            log.info("用户 {} 已屏蔽提醒类型 {}，降级为通知不弹窗", po.getUpdateBy(), reminderType);
        }

        int i = dao.insert(po);
        if (i > 0) {
            MessageReport message = new MessageReport();
            BeanUtils.copyProperties(po, message);
            message.setMessageTypeName(MessageTypeEnum.getDescription(po.getMessageType()));
            webSocketSendUtil.newMessageReport(po.getUpdateBy(), message);
        }

        return i > 0;
    }

    @Override
    public Integer countUnreadMessage() {
        return countUnreadMessage(null);
    }

    @Override
    public Integer countUnreadMessage(String userName) {
        LambdaQueryWrapper<MessageInfoPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageInfoPO::getMessageStatus, MessageStatusEnum.STATUS_UNREAD.getCode());
        applyUserFilter(wrapper, userName);
        Long count = dao.selectCount(wrapper);

        return count.intValue();
    }

    @Override
    public Boolean readAllMessage() {
        return readAllMessage(null);
    }

    @Override
    public Boolean readAllMessage(String userName) {
        LambdaUpdateWrapper<MessageInfoPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(MessageInfoPO::getMessageStatus, MessageStatusEnum.STATUS_UNREAD.getCode());
        applyUserFilter(wrapper, userName);
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
