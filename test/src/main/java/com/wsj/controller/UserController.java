package com.wsj.controller;

import com.wsj.entty.DataVo;
import com.wsj.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/")
public class UserController {

    @Autowired
    private UserService userService;
    @GetMapping("selectAll")
    public DataVo hello(){
        return userService.selectAll();
    }
}
