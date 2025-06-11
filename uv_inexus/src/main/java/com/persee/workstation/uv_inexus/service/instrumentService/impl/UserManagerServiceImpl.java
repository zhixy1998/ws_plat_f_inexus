package com.persee.workstation.uv_inexus.service.instrumentService.impl;


import com.persee.workstation.uv_inexus.dao.userConfigDao.UserConfigDAO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.systemctlConfigPo.UserConfigPO;
import com.persee.workstation.uv_inexus.service.instrumentService.UserManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class UserManagerServiceImpl implements UserManagerService {

    @Autowired
    UserConfigDAO userConfigDAO;

    @Override
    public R<UserConfigPO> login(UserLoginDTO loginDTO) {
        if (StringUtils.hasText(loginDTO.getUserName()) || StringUtils.hasText(loginDTO.getPassword())) {
            log.info("未传入用户名或密码");
            return R.fail(false, "缺少用户名或密码");
        }

        String password = userConfigDAO.getPassword(loginDTO.getUserName());
        if (password.isEmpty()) {
            log.info("未查找到用户密码");
            return R.fail(false, "没找到该用户");
        }

        return R.ok();
    }
}
