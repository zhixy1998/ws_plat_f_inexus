/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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
