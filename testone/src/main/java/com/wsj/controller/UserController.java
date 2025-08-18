package com.wsj.controller;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.wsj.entty.OrderInfo;
import com.wsj.entty.UserInfo;
import com.wsj.service.OrderInfoService;
import com.wsj.service.UserService;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private OrderInfoService orderInfoService;

    @DS("ds1")
    @GetMapping("selectAll")
    public List<UserInfo> hello(){
        return userService.selectAll();
    }

    @DS("ds2")
    @GetMapping("selectAll2")
    public List<OrderInfo> hello2(){
        return orderInfoService.selectAll();
    }
}
