/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.constant;

import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

public class Constants {
    public Constants() {
    }

    public static final String SUPER_USER_NAME = "admin";

    public final static String DEFAULT_USER_NAME = "default";

    public final static String DEFAULT_USER_PASSWORD = "123456";

    /**
     * 自检状态：0：未完成；1：自检成功； 2：自检失败； 3：自检进行中
     */
    public final static Integer INIT_STATUS_NO = 0;
    public final static Integer INIT_STATUS_SUCCESS = 1;
    public final static Integer INIT_STATUS_FALSE = 2;
    public final static Integer INIT_STATUS_RUNNING = 3;
    /**
     * 初始化各条目状态值：0-进行中，1-成功，2-失败
     */
    public final static Integer INIT_TASK_STATUS_ING = 0;
    public final static Integer INIT_TASK_STATUS_SUCCESS = 1;
    public final static Integer INIT_TASK_STATUS_FALSE = 2;

    /**
     * 开机状态：0-未开机，1-开机，2-软关机
     */
    public final static Integer DEVICE_IS_NULL = 0;
    public final static Integer DEVICE_IS_OPEN = 1;
    public final static Integer DEVICE_IS_SOFT_CLOSE = 2;
    /**
     * 联机状态：0-未连接，1-联机中，2-联机成功，3-联机失败
     */
    public final static Integer CONNECT_DEVICE_NO = 0;
    public final static Integer CONNECT_DEVICE_ING = 1;
    public final static Integer CONNECT_DEVICE_SUCCESS = 2;
    public final static Integer CONNECT_DEVICE_FALSE = 3;

    public static volatile LocalDateTime startTime=null;
    public static volatile LocalDateTime endTime = null;

    public static volatile Boolean isRunning = true;

    public static final ReentrantLock MEASURE_LOCK = new ReentrantLock(true);

    public static volatile List<String> field1Values = new CopyOnWriteArrayList<>();
    // 存储原始的数据
    public static volatile List<String> field2Values = new CopyOnWriteArrayList<>();
    // 存储处理后的数据
    public static volatile List<String> field3Values = new CopyOnWriteArrayList<>();

    // 存储仪器数据
    public static volatile List<String> instrumentData = new CopyOnWriteArrayList<>();

    // 存储计算后的数据
    public static volatile List<String> originalCalculationData = new CopyOnWriteArrayList<>();

    public static volatile List<String> x_data = new ArrayList<>();
    public static volatile List<String> y_data = new ArrayList<>();

    public static volatile List<ChangeParam> measurd_data = new ArrayList<>();

    public static volatile ChangeParam timeChangeparam = new ChangeParam();

//    public static volatile List<String> r_data = new ArrayList<>();
//    public static volatile List<String> s_data = new ArrayList<>();
//    public static volatile List<String> d_data = new ArrayList<>();

    public static volatile List<String> s_data = new ArrayList<>();
    public static volatile List<String> d_data = new ArrayList<>();
    public static volatile List<String> r_data = new ArrayList<>();

    public static final String EndData = "0D0A,000";

    //等待操作，所有操作执行完成后恢复需要该状态，异常之后，也要恢复该状态
    public static final String machine_status_origin = "0";
    public static final String machine_status_init = "init";
    public static final String machine_status_zero = "zero";
    public static final String machine_status_setParam = "param";
    public static final String machine_status_measure = "measure";
    public static final String machine_status_error = "error"; // 仪器故障，处理待定



    /**
     * 查询用户时，权限校验结果
     */
    public static final String GET_ALL_USER = "all";

    public static final String GET_CURRENT_ROLE = "current_role";

    public static final String GET_CURRENT_GROUP = "current_group";

    /**
     * 小数位数默认值
     */
    public static final Integer DEFAULT_DECIMAL_NUM = 4;

    public static final Integer DEFAULT_DECIMAL_NUM_MAX = 12;

    //全局变量用来判断前端是否请求结束
    public static volatile boolean Running = true;

    /*
    * 样品输入参数正则
     */
    public static final Pattern PARAM_PATTERN = Pattern.compile("^[A-Z]{1,2}\\d{1,10}$");

}
