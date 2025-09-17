package com.persee.workstation.uv_inexus.vo.concise.spectrum;

import lombok.Data;

import java.util.List;

/**
 * 光谱查找方法VO类
 */
@Data
public class SpectrumSearchMethodVo {
    // 总条数
    protected long total;
    // 总页数
    protected long size;
    // 当前页
    protected long current;
    protected List<SpectrumNearestMethodVo> records;
}
