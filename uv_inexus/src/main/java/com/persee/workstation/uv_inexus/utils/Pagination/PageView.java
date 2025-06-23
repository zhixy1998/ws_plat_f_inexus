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
