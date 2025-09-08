package com.wsj.service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wsj.entty.OrderInfo;
import com.wsj.entty.UserInfo;

import java.util.List;


public interface OrderInfoService extends IService<OrderInfo> {

    List<OrderInfo> selectAll();
}
