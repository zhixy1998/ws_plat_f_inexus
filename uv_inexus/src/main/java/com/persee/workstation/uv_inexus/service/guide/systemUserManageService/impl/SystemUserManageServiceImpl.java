/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.systemUserManageService.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.systemUserManageDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.instrumentDto.UserLoginDTO;
import com.persee.workstation.uv_inexus.dto.systemUserManageDto.CreateSystemUserDTO;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.systemUserManagePo.SystemUserManagePO;
import com.persee.workstation.uv_inexus.service.guide.systemUserManageService.SystemUserGroupManageService;
import com.persee.workstation.uv_inexus.service.guide.systemUserManageService.SystemUserManageService;
import com.persee.workstation.uv_inexus.service.guide.systemUserManageService.SystemUserPermissionService;
import com.persee.workstation.uv_inexus.service.guide.systemUserManageService.factory.UserFactoryRoute;
import com.persee.workstation.uv_inexus.util.tokenUtil.JWTUtils;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.GroupVO;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.QueryUserVO;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.SystemUserGroupInfo;
import com.persee.workstation.uv_inexus.vo.systemUserManageVo.SystemUserInfoVO;
import com.persee.workstation.uv_inexus.vo.userManageVo.LoginUserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SystemUserManageServiceImpl implements SystemUserManageService {

    @Autowired
    SystemUserManageDAO dao;

    @Autowired
    SystemUserPermissionService permissionService;

    @Autowired
    SystemUserGroupManageService groupService;

    @Override
    public R<Boolean> createUser(CreateSystemUserDTO dto) {

        if (!StringUtils.hasText(dto.getSystemIdentityId())) {
            System.out.println("缺少用户角色");
            //缺少用户角色
            return R.fail(YamlMessageSource.getMessageStatic("00204001"));
        }

        log.info("权限验证");
        Boolean b = permissionService.checkCreateUserPermission(dto.getUpdateBy(), dto.getSystemIdentityId());
        if (!b) {
            log.info("当前用户没有权限");
            //添加用户出错，权限不足
            return R.fail(YamlMessageSource.getMessageStatic("00204002"));
        }

        LambdaQueryWrapper<SystemUserManagePO> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(SystemUserManagePO::getUserName, dto.getUserName())
//                .or()
//                .eq(SystemUserManagePO::getPhone, dto.getPhone())
//                .or()
//                .eq(SystemUserManagePO::getEmail, dto.getEmail());
        wrapper.eq(SystemUserManagePO::getUserName, dto.getUserName());
        if (!dao.selectList(wrapper).isEmpty()) {
            log.info("当前用户名已存在");
            //当前用户名已存在
            return R.fail(YamlMessageSource.getMessageStatic("00204003"));
        }

        UserFactoryRoute route = new UserFactoryRoute();
        R<SystemUserManagePO> user = route.createUser(dto);
        if (!user.getSuccess()) {
            return R.fail(user.getMessage());
        }
        SystemUserManagePO po = user.getData();

        return R.ok(dao.insert(po) > 0);

    }

    @Override
    public R<Boolean> updateUser(CreateSystemUserDTO dto) {

        Boolean b = permissionService.checkUpdateUserPermission(dto.getUpdateBy(), dto);
        if (!b) {
            log.info("操作权限不足");
            //修改用户出错，权限不足
            return R.fail(YamlMessageSource.getMessageStatic("00204004"));
        }

        LambdaQueryWrapper<SystemUserManagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemUserManagePO::getUserName, dto.getUserName());
        SystemUserManagePO po = dao.selectOne(wrapper);
        if (po == null) {
            log.info("用户不存在");
            //修改用户名出错，请检查
            return R.fail(YamlMessageSource.getMessageStatic("00204005"));
        }

        BeanUtils.copyProperties(dto, po);
        po.setUpdateBy(dto.getUpdateBy());
        return R.ok(dao.updateById(po) > 0);
    }

    @Override
    public R<Boolean> deleteUser(String updateBy, String deleteUserName) {

        if (deleteUserName.equals(Constants.SUPER_USER_NAME)) {
            log.info("管理员不允许删除");
            //管理员不允许删除
            return R.fail(YamlMessageSource.getMessageStatic("00204006"));
        }

        if (updateBy.equals(deleteUserName)) {
            //不能删除自己
            return R.fail(YamlMessageSource.getMessageStatic("00204007"));
        }
        Boolean b = permissionService.checkDeleteUserPermission(updateBy, deleteUserName);
        if (!b) {
            log.info("删除权限不足");
            //删除用户出错，权限不足
            return R.fail(YamlMessageSource.getMessageStatic("00204008"));
        }
        LambdaQueryWrapper<SystemUserManagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemUserManagePO::getUserName, deleteUserName);
        int delete = dao.delete(wrapper);
        if (delete == 0) {
            //删除失败
            return R.fail(YamlMessageSource.getMessageStatic("00100008"));
        } else {
            return R.ok(true);
        }
    }

    @Override
    public R<List<SystemUserInfoVO>> getUserInfo(String updateBy) {

        LambdaQueryWrapper<SystemUserManagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemUserManagePO::getUserName, updateBy);
        SystemUserManagePO po = dao.selectOne(wrapper);

        List<SystemUserInfoVO> result = new ArrayList<>();
        List<SystemUserManagePO> list;
        wrapper.clear();
        String s = permissionService.checkQueryPermission(po.getSystemIdentityId(), po.getGroupNo());
        if (s.equals(Constants.GET_ALL_USER)) {

             list = dao.selectList(wrapper);

        } else if (s.equals(Constants.GET_CURRENT_ROLE)) {

            wrapper.eq(SystemUserManagePO::getSystemIdentityId, po.getSystemIdentityId());
            list = dao.selectList(wrapper);

        } else {

            wrapper.eq(SystemUserManagePO::getGroupNo, po.getGroupNo());
            list = dao.selectList(wrapper);

        }
        for (SystemUserManagePO managePO : list) {
            SystemUserInfoVO vo = new SystemUserInfoVO();
            BeanUtils.copyProperties(managePO, vo);
            result.add(vo);
        }
        return R.ok(result);

    }

    @Override
    public R<List<QueryUserVO>> queryUserInfo(String updateBy) {
        LambdaQueryWrapper<SystemUserManagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemUserManagePO::getUserName, updateBy);
        SystemUserManagePO po = dao.selectOne(wrapper);

        List<QueryUserVO> result;


        String s = permissionService.checkQueryPermission(po.getSystemIdentityId(), po.getGroupNo());
        if (s.equals(Constants.GET_ALL_USER)) {
            result = dao.queryRoleInfo(null);

            for (QueryUserVO vo : result) {
                List<GroupVO> voList = dao.queryGroupInfo(vo.getSystemIdentityId(), null);
                for (GroupVO groupVO : voList) {
                    wrapper.clear();
                    wrapper.eq(SystemUserManagePO::getGroupNo, groupVO.getGroupNo());
                    List<SystemUserManagePO> poList = dao.selectList(wrapper);
                    List<SystemUserInfoVO> userInfoVOS = new ArrayList<>();
                    for (SystemUserManagePO userManagePO : poList) {
                        SystemUserInfoVO infoVO = new SystemUserInfoVO();
                        BeanUtils.copyProperties(userManagePO, infoVO);
                        userInfoVOS.add(infoVO);
                    }
                    groupVO.setList(userInfoVOS);
                }
                vo.setList(voList);
            }

        } else if (s.equals(Constants.GET_CURRENT_ROLE)) {
            result = dao.queryRoleInfo(po.getSystemIdentityId());

            for (QueryUserVO vo : result) {
                List<GroupVO> voList = dao.queryGroupInfo(vo.getSystemIdentityId(), null);
                for (GroupVO groupVO : voList) {
                    wrapper.clear();
                    wrapper.eq(SystemUserManagePO::getGroupNo, groupVO.getGroupNo());
                    List<SystemUserManagePO> poList = dao.selectList(wrapper);
                    List<SystemUserInfoVO> userInfoVOS = new ArrayList<>();
                    for (SystemUserManagePO userManagePO : poList) {
                        SystemUserInfoVO infoVO = new SystemUserInfoVO();
                        BeanUtils.copyProperties(userManagePO, infoVO);
                        userInfoVOS.add(infoVO);
                    }
                    groupVO.setList(userInfoVOS);
                }
                vo.setList(voList);
            }

        } else {
            result = dao.queryRoleInfo(po.getSystemIdentityId());

            for (QueryUserVO vo : result) {
                List<GroupVO> voList = dao.queryGroupInfo(vo.getSystemIdentityId(), po.getGroupNo());
                for (GroupVO groupVO : voList) {
                    wrapper.clear();
                    wrapper.eq(SystemUserManagePO::getGroupNo, groupVO.getGroupNo());
                    List<SystemUserManagePO> poList = dao.selectList(wrapper);
                    List<SystemUserInfoVO> userInfoVOS = new ArrayList<>();
                    for (SystemUserManagePO userManagePO : poList) {
                        SystemUserInfoVO infoVO = new SystemUserInfoVO();
                        BeanUtils.copyProperties(userManagePO, infoVO);
                        userInfoVOS.add(infoVO);
                    }
                    groupVO.setList(userInfoVOS);
                }
                vo.setList(voList);
            }
        }
        return R.ok(result);
    }

    @Override
    public R<List<SystemUserGroupInfo>> getGroupInfo(String updateBy) {
        LambdaQueryWrapper<SystemUserManagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemUserManagePO::getUserName, updateBy);
        SystemUserManagePO po = dao.selectOne(wrapper);
        if (po == null) {
            //用户不存在
            return R.fail(YamlMessageSource.getMessageStatic("00000001"));
        }

        List<SystemUserGroupInfo> result;
        wrapper.clear();
        String s = permissionService.checkQueryPermission(po.getSystemIdentityId(), po.getGroupNo());
        if (s.equals(Constants.GET_ALL_USER)) {
            result = groupService.queryGroupInfo(null, null);

        } else if (s.equals(Constants.GET_CURRENT_ROLE)) {
            result = groupService.queryGroupInfo(po.getSystemIdentityId(), null);


        } else {
            result = groupService.queryGroupInfo(null, po.getGroupNo());

        }

        return R.ok(result);
    }

    @Override
    public R<LoginUserInfoVO> login(UserLoginDTO loginDTO) {

        if (loginDTO.getIsFastLogin() != null && loginDTO.getIsFastLogin() == 1) {
            log.info("快捷登录，使用默认用户登录");
            loginDTO.setUserName(Constants.DEFAULT_USER_NAME);
            loginDTO.setPassword(Constants.DEFAULT_USER_PASSWORD);
        }

        if (!StringUtils.hasText(loginDTO.getUserName()) || !StringUtils.hasText(loginDTO.getPassword())) {
            log.info("未传入用户名或密码");
            //缺少用户名或密码
            return R.fail(false, YamlMessageSource.getMessageStatic("00204009"));
        }

        String password = dao.getPassword(loginDTO.getUserName());
        if (!StringUtils.hasText(password)) {
            log.info("未查找到用户密码");
            //没找到该用户
            return R.fail(false, YamlMessageSource.getMessageStatic("0020400A"));
        }

        if (loginDTO.getPassword().equals(password)) {
            log.info("查找到用户，检验token是否还可用");
            String oldToken = dao.getToken(loginDTO.getUserName());

            LoginUserInfoVO vo = new LoginUserInfoVO();

            LambdaQueryWrapper<SystemUserManagePO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SystemUserManagePO::getUserName, loginDTO.getUserName());
            SystemUserManagePO po = dao.selectOne(wrapper);
            BeanUtils.copyProperties(po, vo);

            if (StringUtils.hasText(oldToken) && JWTUtils.verify(oldToken)) {
                log.info("token可用，使用旧token");
                vo.setToken(oldToken);

            } else {
                log.info("token不可用生成新token");
                String newToken = JWTUtils.getToken(loginDTO.getUserName());
                vo.setToken(newToken);
                dao.updateToken(loginDTO.getUserName(), newToken);
            }

            return R.ok(vo);
        } else {
            log.info("用户名密码错误！");
            //密码错误！
            return R.fail(false, YamlMessageSource.getMessageStatic("0020400B"));
        }
    }
}
