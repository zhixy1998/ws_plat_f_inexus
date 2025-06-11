package com.persee.workstation.uv_inexus.service.instrumentService.impl;


import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.userConfigDao.UserConfigDAO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.systemctlConfigPo.UserConfigPO;
import com.persee.workstation.uv_inexus.service.instrumentService.UserManagerService;
import com.persee.workstation.uv_inexus.util.JWTUtils;
import com.persee.workstation.uv_inexus.vo.instrumentVo.LoginUserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserManagerServiceImpl implements UserManagerService {

    @Autowired
    UserConfigDAO userConfigDAO;

    @Override
    public R<LoginUserInfoVO> login(UserLoginDTO loginDTO) {

        if (loginDTO.getIsFastLogin() != null && loginDTO.getIsFastLogin() == 1) {
            log.info("快捷登录，使用默认用户登录");
            loginDTO.setUserName(Constants.DEFAULT_USER_NAME);
            loginDTO.setPassword(Constants.DEFAULT_USER_PASSWORD);
        }

        if (!StringUtils.hasText(loginDTO.getUserName()) || !StringUtils.hasText(loginDTO.getPassword())) {
            log.info("未传入用户名或密码");
            return R.fail(false, "缺少用户名或密码");
        }

        String password = userConfigDAO.getPassword(loginDTO.getUserName());
        if (!StringUtils.hasText(password)) {
            log.info("未查找到用户密码");
            return R.fail(false, "没找到该用户");
        }

        if (loginDTO.getPassword().equals(password)) {
            log.info("登录成功");
            Map<String, Object> map = new HashMap<>();
            map.put("userName", loginDTO.getUserName());
            String token = JWTUtils.getToken(map);
            LoginUserInfoVO vo = new LoginUserInfoVO();
            vo.setUserName(loginDTO.getUserName());
            vo.setToken(token);
            return R.ok(vo);
        } else {
            log.info("用户名密码错误！");
            return R.fail(false, "密码错误！");
        }
    }
}
