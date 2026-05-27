package com.persee.workstation.uv_inexus.service.guide.instrumentService;

import com.persee.workstation.uv_inexus.po.guide.instrumentPo.UserReminderMutePO;

import java.util.List;

public interface UserReminderMuteService {

    boolean isMuted(String userName, Integer reminderType);

    void muteReminder(String userName, Integer reminderType, Integer muteScope);

    void unmuteReminder(String userName, Integer reminderType);

    void clearRebootMutes();

    List<UserReminderMutePO> getUserMuteList(String userName);
}
