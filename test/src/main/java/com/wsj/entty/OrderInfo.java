package com.wsj.entty;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单信息表实体类
 */
@Data
@TableName("t_order_info")
public class OrderInfo {

    /** 订单ID */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /** 用户ID */
    private String userId;

    /** 订单编号 */
    private String orderNumber;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 订单状态 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 备注信息 */
    private String remarks;
}
