package com.persee.workstation.uv_inexus.service.userManageService.impl;


import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.userManageDao.UserConfigDAO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.service.userManageService.UserManagerService;
import com.persee.workstation.uv_inexus.util.tokenUtil.JWTUtils;
import com.persee.workstation.uv_inexus.vo.userManageVo.LoginUserInfoVO;
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
            log.info("查找到用户，检验token是否还可用");
            String oldToken = userConfigDAO.getToken(loginDTO.getUserName());

            LoginUserInfoVO vo = new LoginUserInfoVO();
            vo.setUserName(loginDTO.getUserName());

            if (StringUtils.hasText(oldToken) && JWTUtils.verify(oldToken)) {
                log.info("token可用，使用旧token");
                vo.setToken(oldToken);

            } else {
                log.info("token不可用生成新token");
                String newToken = JWTUtils.getToken(loginDTO.getUserName());
                vo.setToken(newToken);
                userConfigDAO.updateToken(loginDTO.getUserName(), newToken);
            }

            return R.ok(vo);
        } else {
            log.info("用户名密码错误！");
            return R.fail(false, "密码错误！");
        }
    }
}
