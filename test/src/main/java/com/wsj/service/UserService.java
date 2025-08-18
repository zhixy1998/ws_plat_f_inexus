package com.wsj.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wsj.entty.DataVo;
import com.wsj.entty.UserInfo;

public interface UserService extends IService<UserInfo> {
    DataVo selectAll();
}
