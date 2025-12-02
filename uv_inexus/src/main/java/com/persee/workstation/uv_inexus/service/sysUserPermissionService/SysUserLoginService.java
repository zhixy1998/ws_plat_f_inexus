/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.sysUserPermissionService;

import com.persee.workstation.uv_inexus.dto.sysUserPermissionDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;

/**
 * 用户登录Service接口（定义登录业务抽象方法）
 */
public interface SysUserLoginService {

    /**
     * 用户登录核心方法
     * @param loginDTO 登录请求参数（包含用户名、密码、快捷登录标识）
     * @return R<LoginUserInfoVO> 登录结果（成功返回用户信息+token，失败返回错误信息）
     */
    R<LoginUserInfoVO> login(UserLoginDTO loginDTO);
}
