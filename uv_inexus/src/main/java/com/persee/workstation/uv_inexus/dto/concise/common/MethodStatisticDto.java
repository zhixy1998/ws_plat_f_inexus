package com.persee.workstation.uv_inexus.dto.concise.common;

import lombok.Data;

import java.util.List;

/**
 * @Description: 方法统计返回类
 * @Author:
 */

@Data
public class MethodStatisticDto {
    // 方法总个数
    private Long methodTotal;
    // 方法完成数
    private Long methodCompleteNum;
    // 方法统计
    private List<MethodDataNumDto> methodDataNum;
    // 总记录数
    private Long total;
    // 总页数
    private Long pages;
    // 当前页码
    private Integer pageNum;
    // 每页大小
    private Integer pageSize;
    // 方法列表
    private List<MethodSumListDto> methodList;
}
