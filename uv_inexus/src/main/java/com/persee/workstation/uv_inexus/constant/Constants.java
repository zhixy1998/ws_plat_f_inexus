/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.constant;

import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    public final static Integer INIT_STATUS_STOP = 4;
    /**
     * 初始化各条目状态值：0-进行中，1-成功，2-失败
     */
    public final static Integer INIT_TASK_ING = 0;
    public final static Integer INIT_TASK_SUCCESS = 1;
    public final static Integer INIT_TASK_FALSE = 2;
    public final static Integer INIT_TASK_NO = 3;
    public final static Integer INIT_TASK_STOP = 4;

    /**
     * 表字段参数
     */
    public final static String PARAM_ZERO = "0";
    public final static String PARAM_ONE = "1";
    public final static String PARAM_TWO = "2";
    public final static String PARAM_THREE = "3";
    public final static String PARAM_FOUR = "4";
    public final static String SCANFAST = "111";
    public final static String DEFAULT_TASK = "默认任务";

    /**
     * 开机状态：0-未开机，1-开机，2-软关机
     */
    public final static Integer DEVICE_IS_NULL = 0;
    public final static Integer DEVICE_IS_OPEN = 1;
    public final static Integer DEVICE_IS_SOFT_CLOSE = 2;
    /**
     * 联机状态：0-未连接，1-联机中，2-联机成功，3-联机失败, 4-已断开
     */
    public final static Integer CONNECT_DEVICE_NO = 0;
    public final static Integer CONNECT_DEVICE_ING = 1;
    public final static Integer CONNECT_DEVICE_SUCCESS = 2;
    public final static Integer CONNECT_DEVICE_FALSE = 3;
    public final static Integer CONNECT_DEVICE_STOP = 4;

    /**
     * 仪器当前状态 0-空闲，1-初始化中，2-测量中，3-回显线程占用中，4其他指令-占用, 5-零点校准中
     */
    public final static Integer DEVICE_STATUS_IDLE = 0;
    public final static Integer DEVICE_STATUS_INIT = 1;
    public final static Integer DEVICE_STATUS_MEASURE = 2;
    public final static Integer DEVICE_STATUS_ECHO = 3;
    public final static Integer DEVICE_STATUS_OTHER = 4;
    public final static Integer DEVICE_STATUS_ZERO = 5;
    public final static Integer DEVICE_STATUS_DIS_CONNECT = 6;
    public final static Integer DEVICE_STATUS_CALIBRATION = 7;



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

    // 拟合存储对象
    public static volatile  ChangeParam lastStepParam = null; // 上一个点，用于拟合

    // 存储计算后的数据
    public static volatile List<String> originalCalculationData = new CopyOnWriteArrayList<>();

    // 停止状态集合(序列号,停止状态 true-正常运行, false-停止)
    public static volatile Map<String, Boolean> measureStopStatus = new ConcurrentHashMap<>();

    public static volatile List<String> x_data = new ArrayList<>();
    public static volatile List<String> y_data = new ArrayList<>();

    public static volatile List<ChangeParam> measurd_data = new ArrayList<>();

    public static volatile ChangeParam timeChangeparam = new ChangeParam();

    /// 测量状态
    public static volatile boolean measureStatus = false;
    /// 占用状态
    public static volatile boolean occupancyStatus= false;
    /// 存储发送线程
    public static volatile Thread sendMeasureThread = null;

//    public static volatile List<String> r_data = new ArrayList<>();
//    public static volatile List<String> s_data = new ArrayList<>();
//    public static volatile List<String> d_data = new ArrayList<>();

    public static volatile List<String> s_data = new ArrayList<>();
    public static volatile List<String> d_data = new ArrayList<>();
    public static volatile List<String> r_data = new ArrayList<>();

    // 时间扫描发送的时间
    public static volatile List<BigDecimal> sendIntervals = new ArrayList<>();

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

    /**
     * 仪器连接状态，<ip:boolean>, ip为仪器ip，boolean为连接状态-true为连接，false为断开连接
     */
    public static volatile Map<String, Boolean> devConnectInfo = new HashMap<>();

    /*
    * 样品输入参数正则
     */
    public static final Pattern PARAM_PATTERN = Pattern.compile("^[A-Z]{1,2}\\d{1,10}$");


    /*
    消息来源分类：系统消息:system,仪器：device，远程： remote,未知：unknown
     */
    public static final String MESSAGE_SOURCE_SYSTEM = "system";
    public static final String MESSAGE_SOURCE_DEVICE = "device";
    public static final String MESSAGE_SOURCE_REMOTE = "remote";
    public static final String MESSAGE_SOURCE_UNKNOWN = "unknown";


}
