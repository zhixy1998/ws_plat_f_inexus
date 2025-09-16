package com.persee.workstation.uv_inexus.vo.concise.timesweep;

import lombok.Data;

import java.util.List;
/**
 * 时间查找方法VO类
 */
@Data
public class TimeSweepSearchMethodVo {
    // 总条数
    protected long total;
    // 总页数
    protected long size;
    // 当前页
    protected long current;
    protected List<TimeSweepNearestMethodVo> records;
}
