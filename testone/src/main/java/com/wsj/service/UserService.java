package com.wsj.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wsj.entty.UserInfo;
import org.apache.catalina.User;

import java.util.List;

public interface UserService extends IService<UserInfo> {
    List<UserInfo> selectAll();
}
