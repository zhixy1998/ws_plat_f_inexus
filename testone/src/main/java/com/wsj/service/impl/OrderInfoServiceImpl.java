package com.wsj.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wsj.entty.OrderInfo;
import com.wsj.mapper.secondary.OrderInfoMapper;
import com.wsj.service.OrderInfoService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("OrderInfoService")
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Override
    public List<OrderInfo> selectAll() {
        return this.list();
    }
}
