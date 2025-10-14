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

package com.persee.workstation.uv_inexus.utils.Pagination;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PageView<T> {
    @JsonProperty("data")       // 修改字段名
    private List<T> records;
    
    @JsonProperty("total") // 修改字段名
    private long total;
    
    @JsonProperty("pageSize")
    private long size;
    
    @JsonProperty("pageNum")
    private long current;
    
    @JsonProperty("totalPages")
    private long pages;

    public PageView(IPage<T> page) {
        this.records = page.getRecords();
        this.total = page.getTotal();
        this.size = page.getSize();
        this.current = page.getCurrent();
        this.pages = page.getPages();
    }
}
