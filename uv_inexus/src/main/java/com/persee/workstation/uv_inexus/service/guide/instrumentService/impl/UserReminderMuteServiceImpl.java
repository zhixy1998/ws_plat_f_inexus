package com.persee.workstation.uv_inexus.service.guide.instrumentService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.UserReminderMuteDAO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.UserReminderMutePO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.UserReminderMuteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class UserReminderMuteServiceImpl implements UserReminderMuteService {

    @Autowired
    private UserReminderMuteDAO userReminderMuteDAO;

    @Override
    public boolean isMuted(String userName, Integer reminderType) {
        if (userName == null || reminderType == null) {
            return false;
        }
        LambdaQueryWrapper<UserReminderMutePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserReminderMutePO::getUserName, userName)
               .eq(UserReminderMutePO::getReminderType, reminderType)
               .eq(UserReminderMutePO::getMuted, 1);
        return userReminderMuteDAO.selectCount(wrapper) > 0;
    }

    @Override
    public void muteReminder(String userName, Integer reminderType, Integer muteScope) {
        LambdaQueryWrapper<UserReminderMutePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserReminderMutePO::getUserName, userName)
               .eq(UserReminderMutePO::getReminderType, reminderType);
        UserReminderMutePO existing = userReminderMuteDAO.selectOne(wrapper);

        if (existing != null) {
            existing.setMuted(1);
            existing.setMuteScope(muteScope != null ? muteScope : 0);
            userReminderMuteDAO.updateById(existing);
        } else {
            UserReminderMutePO po = new UserReminderMutePO();
            po.setId(UUID.randomUUID().toString().replace("-", ""));
            po.setUserName(userName);
            po.setReminderType(reminderType);
            po.setMuted(1);
            po.setMuteScope(muteScope != null ? muteScope : 0);
            po.setCreateTime(LocalDateTime.now());
            userReminderMuteDAO.insert(po);
        }
        log.info("用户 {} 屏蔽了提醒类型 {}，范围：{}", userName, reminderType,
                muteScope != null && muteScope == 1 ? "永久" : "下次开机恢复");
    }

    @Override
    public void unmuteReminder(String userName, Integer reminderType) {
        LambdaUpdateWrapper<UserReminderMutePO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserReminderMutePO::getUserName, userName)
               .eq(UserReminderMutePO::getReminderType, reminderType)
               .set(UserReminderMutePO::getMuted, 0);
        userReminderMuteDAO.update(null, wrapper);
        log.info("用户 {} 恢复了提醒类型 {}", userName, reminderType);
    }

    @Override
    public void clearRebootMutes() {
        LambdaUpdateWrapper<UserReminderMutePO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserReminderMutePO::getMuteScope, 0)
               .eq(UserReminderMutePO::getMuted, 1)
               .set(UserReminderMutePO::getMuted, 0);
        int count = userReminderMuteDAO.update(null, wrapper);
        if (count > 0) {
            log.info("启动时清除了 {} 条「下次开机恢复」的提醒屏蔽记录", count);
        }
    }

    @Override
    public List<UserReminderMutePO> getUserMuteList(String userName) {
        LambdaQueryWrapper<UserReminderMutePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserReminderMutePO::getUserName, userName)
               .eq(UserReminderMutePO::getMuted, 1);
        return userReminderMuteDAO.selectList(wrapper);
    }
}
