package com.wsj.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsj.entty.DataVo;
import com.wsj.entty.UserInfo;
import com.wsj.mapper.primary.UserMapper;
import com.wsj.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserInfo> implements UserService {
    @Override
    public DataVo selectAll() {
        DataVo dataVo = new DataVo();
//        dataVo.setOrderInfoList(orderInfoService.list());
        dataVo.setUserInfoList(this.list());
        return dataVo;
    }
}
