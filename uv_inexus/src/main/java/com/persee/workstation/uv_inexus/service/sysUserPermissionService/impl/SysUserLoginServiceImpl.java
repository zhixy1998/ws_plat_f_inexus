package com.persee.workstation.uv_inexus.service.sysUserPermissionService.impl;

import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserMapper;
import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysUserLoginService;
import com.persee.workstation.uv_inexus.util.tokenUtil.JWTUtils;
import com.persee.workstation.uv_inexus.utils.OperateLog.UserContext;
import com.persee.workstation.uv_inexus.utils.PasswordUtils.PasswordUtils;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
/**
 * 用户登录服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor // 构造器注入，替代@Autowired
public class SysUserLoginServiceImpl implements SysUserLoginService {

    private final SysUserMapper sysUserDAO; // 注入用户DAO（操作t_sys_user表）

    @Override
    public R<LoginUserInfoVO> login(UserLoginDTO loginDTO) {
        // 1. 处理快捷登录：补全默认用户名/密码（参照你原有逻辑）
        if (loginDTO.getIsFastLogin() != null && loginDTO.getIsFastLogin() == 1) {
            log.info("触发快捷登录，使用默认用户");
            // 建议在Constants类中定义默认值，避免硬编码（示例：Constants.DEFAULT_USERNAME）
            loginDTO.setUserName("admin");
            loginDTO.setPassword("123456");
        }

        // 2. 基础参数校验：用户名/密码不能为空
        String username = loginDTO.getUserName();
        String rawPassword = loginDTO.getPassword(); // 前端传入的明文密码
        if (!StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
            log.warn("登录失败：用户名或密码为空，username={}", username);
            return R.fail("缺少用户名或密码");
        }

        // 3. 查询数据库中的用户（只查询「启用状态」的用户，status=1）
        SysUserPO userPO = sysUserDAO.selectByUsername(username);
        if (userPO == null) {
            log.warn("登录失败：未找到用户或用户已禁用，username={}", username);
            return R.fail("用户名不存在或已被禁用");
        }

        // 4. 密码校验：用PasswordUtils匹配（明文 vs BCrypt加密密码）→ 核心修复！
        String encodedPassword = userPO.getPassword(); // 数据库中存储的BCrypt加密密码
        boolean isPasswordMatch = PasswordUtils.matches(rawPassword, encodedPassword);
        if (!isPasswordMatch) {
            log.warn("登录失败：密码错误，username={}", username);
            return R.fail("密码错误");
        }

        // 5. Token处理：复用有效Token / 生成新Token（保留原有逻辑）
        String token;
        LocalDateTime now = LocalDateTime.now(); // 当前登录时间（用于更新最后登录时间）
        String oldToken = sysUserDAO.selectTokenByUsername(username);

        if (StringUtils.hasText(oldToken) && JWTUtils.verify(oldToken)) {
            // 旧Token未过期：直接复用
            token = oldToken;
            log.info("登录成功：复用有效Token，username={}", username);
        } else {
            // 旧Token过期/不存在：生成新Token（JWT工具类需确保正确）
            token = JWTUtils.getToken(username); // 建议在JWT中加入用户ID，增强安全性
            System.out.println("111111111111111111"+token);

            // 更新数据库：存储新Token + 最后登录时间
            int updateRows = sysUserDAO.updateTokenAndLoginTime(username, token, now);
            if (updateRows <= 0) {
                log.error("登录警告：Token和最后登录时间更新失败，username={}", username);
                return R.fail("登录成功，但身份令牌存储失败，请重试");
            }
            log.info("登录成功：生成新Token，username={}", username);
        }

        // 6. 组装登录响应VO（拷贝用户信息 + 填充Token）
        LoginUserInfoVO loginVO = new LoginUserInfoVO();
        BeanUtils.copyProperties(userPO, loginVO); // 拷贝用户ID、姓名、手机号等基础信息
        loginVO.setToken(token); // 填充Token
        loginVO.setLastLoginTime(now); // 填充本次登录时间（实时更新）
        loginVO.setId(userPO.getId()); // 从数据库查的真实ID
        loginVO.setUsername(loginDTO.getUserName()); // 登录用户名
        // System.out.println("wewrwerwe"+userPO.getId());
        // System.out.println("qweqeqweqweqwe"+loginDTO.getUserName());
        UserContext.setCurrentUser(loginVO);

        // 7. 返回成功结果
        return R.ok(loginVO);
    }
}