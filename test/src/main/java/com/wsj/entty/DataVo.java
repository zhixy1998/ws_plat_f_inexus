package com.wsj.entty;

import lombok.Data;

import java.util.List;

@Data
public class DataVo {
    List<UserInfo> userInfoList;
    List<OrderInfo> orderInfoList;
}
