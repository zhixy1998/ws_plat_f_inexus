package com.persee.workstation.uv_inexus.constant;

public class Constants {
    public Constants() {
    }

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



    /**
     * 接收数据字节码类型：
     */

}
