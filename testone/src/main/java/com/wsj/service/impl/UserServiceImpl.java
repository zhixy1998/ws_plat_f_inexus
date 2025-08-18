package com.wsj.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsj.entty.UserInfo;
import com.wsj.mapper.primary.UserMapper;
import com.wsj.service.OrderInfoService;
import com.wsj.service.UserService;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserInfo> implements UserService {

    @Autowired
    private OrderInfoService orderInfoService;

    @Override
    public List<UserInfo> selectAll() {
        return list();
    }
}